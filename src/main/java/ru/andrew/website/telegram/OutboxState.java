package ru.andrew.website.telegram;

public enum OutboxState {
    pending,
    processing,
    retry,
    blocked,
    delivered
}
