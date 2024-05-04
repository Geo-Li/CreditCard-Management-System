package com.shepherdmoney.interviewproject.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class BalanceHistory implements Comparable<BalanceHistory> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne
    @JoinColumn(name = "credit_card_id")
    private CreditCard creditCard;

    private LocalDate date;

    private double balance;

    @Override
    public int compareTo(BalanceHistory other) {
        return this.date.compareTo(other.date);
    }

    @Override
    public String toString() {
        return "[card number=" + creditCard.getNumber() + ", date=" + date + ", balance=" + balance + "]";
    }
}
