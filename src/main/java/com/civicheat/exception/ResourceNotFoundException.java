package com.civicheat.exception;

/**
 * Exception thrown when a requested domain entity or file resource is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
