package com.civicheat.exception;

/**
 * Exception thrown when an uploaded file violates format, dimension, or security constraints.
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
