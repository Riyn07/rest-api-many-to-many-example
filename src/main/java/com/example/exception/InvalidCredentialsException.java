package com.example.exception;

/**
 * Credenciales invalidas (email o contrasena incorrectos). Se traduce a
 * HTTP 401 Unauthorized en el manejador global.
 */
public class InvalidCredentialsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException(String message) {
        super(message);
    }
}