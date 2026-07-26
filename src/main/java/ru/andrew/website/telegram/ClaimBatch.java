package ru.andrew.website.telegram;

import java.util.List;

public record ClaimBatch(
        List<ClaimedDelivery> deliveries,
        int recoveredLeaseCount) {

    public ClaimBatch {
        deliveries = List.copyOf(deliveries);
    }
}
