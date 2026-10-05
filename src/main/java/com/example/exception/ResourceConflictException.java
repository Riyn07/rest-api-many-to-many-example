package com.example.exception;

/**
 * Conflicto con el estado actual del recurso, por ejemplo intentar registrar
 * un email que ya existe. Se traduce a HTTP 409 Conflict.
 */
public class ResourceConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceConflictException(String message) {
        super(message);
    }
}