package com.apilens.domain.exception;

/** Thrown when a requested domain entity does not exist (or isn't owned by the caller). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
