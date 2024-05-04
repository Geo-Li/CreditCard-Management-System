package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.*;
import com.shepherdmoney.interviewproject.repository.*;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
    private final CreditCardRepository creditCardRepository;
    private final UserRepository userRepository;

    @Autowired
    public CreditCardController(CreditCardRepository creditCardRepository, UserRepository userRepository) {
        this.creditCardRepository = creditCardRepository;
        this.userRepository = userRepository;
    }


    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with
        //     user with given userId
        //     Return 200 OK with the credit card id if the user exists and credit card is
        //     successfully associated with the user
        //     Return other appropriate response code for other exception cases
        //     Do not worry about validating the card number, assume card number could be
        //     any arbitrary format and length

        // if the card owner doesn't exit, then we report an error
        Optional<User> owner = userRepository.findById(payload.getUserId());
        if (!owner.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        // if the card number has already been taken, then we report an error
        if (creditCardRepository.findByNumber(payload.getCardNumber()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
        try {
            CreditCard newCard = new CreditCard();
            newCard.setIssuanceBank(payload.getCardIssuanceBank());
            newCard.setNumber(payload.getCardNumber());
            newCard.setOwner(owner.get());

            // Save the new credit card to the database
            CreditCard savedCard = creditCardRepository.save(newCard);
            return ResponseEntity.ok(savedCard.getId());
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId,
        //     using CreditCardView class
        //     if the user has no credit card, return empty list, never return null

        // if the user doesn't exist, we report an error
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        // Retrieve all credit cards associated with the user
        List<CreditCard> creditCards = creditCardRepository.findByOwnerId(userId);
        List<CreditCardView> allCreditCards = new ArrayList<>();
        for (CreditCard creditCard : creditCards) {
            CreditCardView creditCardView = new CreditCardView(creditCard.getIssuanceBank(), creditCard.getNumber());
            allCreditCards.add(creditCardView);
        }
        return ResponseEntity.ok(allCreditCards);
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user
        //     associated with the credit card
        //     If so, return the user id in a 200 OK response. If no such user exists,
        //     return 400 Bad Request
        CreditCard card = creditCardRepository.findByNumber(creditCardNumber);
        // if there is no such card or there is no associated owner, we report an error
        if (card == null || card.getOwner() == null) {
            return ResponseEntity.badRequest().body(null);
        } else { // return the userId
            return ResponseEntity.ok(card.getOwner().getId());
        }
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<String> updateBalance(@RequestBody UpdateBalancePayload[] payloads) {
        // TODO: Given a list of transactions, update credit cards' balance history.
        //     1. For the balance history in the credit card
        //     2. If there are gaps between two balance dates, fill the empty date with the
        //     balance of the previous date
        //     3. Given the payload `payload`, calculate the balance different between the
        //     payload and the actual balance stored in the database
        //     4. If the different is not 0, update all the following budget with the
        //     difference
        //     For example: if today is 4/12, a credit card's balanceHistory is [{date:
        //     4/12, balance: 110}, {date: 4/10, balance: 100}],
        //     Given a balance amount of {date: 4/11, amount: 110}, the new balanceHistory
        //     is
        //     [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10,
        //     balance: 100}]
        //     This is because
        //     1. You would first populate 4/11 with previous day's balance (4/10), so
        //     {date: 4/11, amount: 100}
        //     2. And then you observe there is a +10 difference
        //     3. You propagate that +10 difference until today
        //     Return 200 OK if update is done and successful, 400 Bad Request if the given
        //     card number
        //     is not associated with a card.
        for (UpdateBalancePayload payload : payloads) {
            CreditCard creditCard = creditCardRepository.findByNumber(payload.getCreditCardNumber());
            // if there is no such credit card in the database, we report an error
            if (creditCard == null) {
                return ResponseEntity.badRequest().body("Credit card not found");
            } else {
                // Call the helper function below
                addBalance(payload);
            }
        }
        return ResponseEntity.ok("Credit card updated successfully");
    }

    private void addBalance(UpdateBalancePayload payload) {
        // Get all balance histories
        CreditCard creditCard = creditCardRepository.findByNumber(payload.getCreditCardNumber());
        List<BalanceHistory> balanceHistories = creditCard.getBalanceHistories();
        Collections.sort(balanceHistories);

        LocalDate payloadDate = payload.getBalanceDate();
        double payloadAmount = payload.getBalanceAmount();
        // if there is no need for payload update, we add the entry to balanceHistories
        boolean added_payload = false;

        // This following for loop is slow and dumb, I know this for certain!
        // It's just learning Spring Boot took forever time from me, and besides fixing bugs,
        // I don't really have time to optimize performance!!!
        for (int index = 0; index < balanceHistories.size(); index++) {
            BalanceHistory balanceHistory = balanceHistories.get(index);
            // if the date inserted is already existed
            boolean is_same_date = balanceHistory.getDate().isEqual(payloadDate);
            // if the date inserted is located within a gap
            boolean is_gap = (payloadDate.isAfter(balanceHistory.getDate()) &&
                    (index == balanceHistories.size()-1 || payloadDate.isBefore(balanceHistories.get(index+1).getDate())));
            // Update all the entries that are after the inserted date
            if (is_same_date || is_gap) {
                double difference = payloadAmount - balanceHistory.getBalance();
                // if the balance is the same as the record, we skip the updating process
                if (difference != 0) {
                    for (int update_index = 0; update_index < balanceHistories.size(); update_index++) {
                        BalanceHistory updatedBalanceHistory = balanceHistories.get(update_index);
                        if (updatedBalanceHistory.getDate().isAfter(payloadDate)) {
                            updatedBalanceHistory.setBalance(updatedBalanceHistory.getBalance() + difference);
                            balanceHistories.set(update_index, updatedBalanceHistory);
                        }
                    }
                }
            }
            if (is_same_date) {
                // Update existing balance history
                balanceHistory.setBalance(payload.getBalanceAmount());
                balanceHistories.set(index, balanceHistory);
                added_payload = true;
                break;
            } else if (is_gap) {
                // Insert the new balance history to the list
                BalanceHistory newBalanceHistory = new BalanceHistory();
                newBalanceHistory.setCreditCard(creditCard);
                newBalanceHistory.setDate(payload.getBalanceDate());
                newBalanceHistory.setBalance(payload.getBalanceAmount());
                balanceHistories.add(newBalanceHistory);
                added_payload = true;
                break;
            }
        }
        if (!added_payload) {
            // Insert the new balance history to the list
            BalanceHistory newBalanceHistory = new BalanceHistory();
            newBalanceHistory.setCreditCard(creditCard);
            newBalanceHistory.setDate(payload.getBalanceDate());
            newBalanceHistory.setBalance(payload.getBalanceAmount());
            balanceHistories.add(newBalanceHistory);
        }
        System.out.println(balanceHistories.toString());
        Collections.sort(balanceHistories);
        // Save the updated balanceHistories to the database
        creditCard.setBalanceHistories(balanceHistories);
        creditCardRepository.save(creditCard);
    }

    @GetMapping("/credit-card:date")
    public ResponseEntity<String> getCreditCardBalanceForDate(@RequestParam String number, @RequestParam LocalDate date) {
        // Get the balance with the passed in date variable
        CreditCard creditCard = creditCardRepository.findByNumber(number);
        List<BalanceHistory> balanceHistories = creditCard.getBalanceHistories();
        Collections.sort(balanceHistories);
        for (int index = 0; index < balanceHistories.size(); index++) {
            BalanceHistory balanceHistory = balanceHistories.get(index);
            // if the inquiry is before the earliest date, then we report an error
            if (index == 0) {
                if (date.isBefore(balanceHistory.getDate())) {
                    return ResponseEntity.badRequest().body("Inquiry is before the issuance of the credit card");
                }
            }
            // if we find an exact match with the date
            if (balanceHistory.getDate().equals(date)) {
                return ResponseEntity.ok(balanceHistory.toString());
            }
            // if the inquiry is located within a gap
            else if (balanceHistory.getDate().isAfter(date)) {
                return ResponseEntity.ok(creditCard.getBalanceHistories().get(index-1).toString());
            }
        }
        // Let's assume that any date that passes today is not a valid date to retrieve the balance
        return ResponseEntity.notFound().build();
    }
}
