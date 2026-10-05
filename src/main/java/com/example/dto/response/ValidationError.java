package com.example.dto.response;

/**
 * Error de validacion de un campo concreto del JSON recibido.
 *
 * @param field         nombre del campo invalido
 * @param message       motivo de la invalidez
 * @param rejectedValue valor recibido, o null si no se decide exponerlo
 */
public record ValidationError(String field, String message, Object rejectedValue) {

    public static ValidationError of(String field, String message, Object rejectedValue) {
        return new ValidationError(field, message, rejectedValue);
    }

    /** Variante para quando no se quiere exponer el valor recibido. */
    public static ValidationError of(String field, String message) {
        return new ValidationError(field, message, null);
    }
}