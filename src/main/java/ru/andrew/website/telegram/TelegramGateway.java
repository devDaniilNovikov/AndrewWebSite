package ru.andrew.website.telegram;

import java.time.Instant;

public interface TelegramGateway {
    TelegramDeliveryResult send(
            TelegramLeadMessage message, Instant latestStart);
}
