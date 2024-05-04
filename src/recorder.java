
private void addBalance(UpdateBalancePayload payload) {
    if (balanceHistories.lastEntry() != null) {
        // Fill the gap between date entries
        LocalDate lastDate = balanceHistories.lastEntry().getKey().plusDays(1);
        double lastBalance = getMostRecentBalance();
                    while (lastDate.isBefore(date)) {
                balanceHistories.putIfAbsent(lastDate, lastBalance);
        lastDate = lastDate.plusDays(1);
                    }
                            }
        updatePayLoad(date, balance);
                balanceHistories.put(date, balance);
                payloadHistories.put(date, balance);
                System.out.println(balanceHistories.toString());
        }

private double getBalance(LocalDate date) {
    if (balanceHistories.containsKey(date)) {
        return balanceHistories.get(date);
    } else {
        // If the date is later than the latest date, then there shouldn't be
        // any data to retrieve
        return -1;
    }
}

private double getMostRecentBalance() {
    if (balanceHistories.lastEntry() != null) {
        return balanceHistories.lastEntry().getValue();
    } else {
        return -1;
    }
}

private void updatePayLoad(List<BalanceHistory> balanceHistories, int index, double difference) {
    for (int i = index; i < balanceHistories.size(); i++) {

    }

    if (balanceHistories.containsKey(date)) {
        double difference = balance - balanceHistories.get(date);
        if (difference == 0) return;
        LocalDate dateFollowed = date.plusDays(1);
        // Update all the entries followed
        while (balanceHistories.containsKey(dateFollowed)) {
            double updatedBalance = balanceHistories.get(dateFollowed) + difference;
            balanceHistories.put(dateFollowed, updatedBalance);
            // Update payloadHistories if the future entry is existed
            if (payloadHistories.containsKey(dateFollowed)) {
                payloadHistories.put(dateFollowed, updatedBalance);
            }
            dateFollowed = dateFollowed.plusDays(1);
        }
    }
}