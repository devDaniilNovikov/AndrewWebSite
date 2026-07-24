package ru.andrew.website.leads;

public final class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() {
        super("Idempotency conflict");
    }
}
