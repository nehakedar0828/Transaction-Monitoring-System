package com.nehakedar.backend.exception;

// Exception thrown when a requested transaction does not exist.
public class TransactionNotFoundException extends RuntimeException {

    // Constructor receives the error message.
    public TransactionNotFoundException(String message) {

        // Pass the message to RuntimeException.
        super(message);
    }
}