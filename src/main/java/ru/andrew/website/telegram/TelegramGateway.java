package ru.andrew.website.telegram;

public interface TelegramGateway {
    TelegramDeliveryResult send(TelegramLeadMessage message);
}
