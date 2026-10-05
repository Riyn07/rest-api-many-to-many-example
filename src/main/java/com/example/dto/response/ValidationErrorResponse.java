package com.example.dto.response;

import java.util.Date;
import java.util.List;

/**
 * Respuesta 400 que contiene <b>todos</b> los errores de validacion
 * detectados en el JSON recibido, no solo el primero.
 *
 * @param statusCode  codigo HTTP
 * @param timestamp   momento de la respuesta
 * @param message     resumen del error
 * @param description recurso afectado (p.ej. {@code uri=/api/auth/register})
 * @param errors      lista completa de campos invalidos
 */
public record ValidationErrorResponse(
        int statusCode,
        Date timestamp,
        String message,
        String description,
        List<ValidationError> errors) {

    public ValidationErrorResponse(int statusCode, Date timestamp, String message,
            String description, List<ValidationError> errors) {
        this.statusCode = statusCode;
        this.timestamp = timestamp;
        this.message = message;
        this.description = description;
        this.errors = List.copyOf(errors);
    }
}