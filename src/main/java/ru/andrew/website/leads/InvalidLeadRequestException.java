package ru.andrew.website.leads;

public final class InvalidLeadRequestException extends RuntimeException {
    public InvalidLeadRequestException() {
        super("Invalid lead request");
    }
}
