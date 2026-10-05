package com.example.dto.response;

import com.example.entities.ERole;

/**
 * Respuesta del login: token JWT y metadatos de la sesion iniciada.
 *
 * @param token      token JWT
 * @param tokenType  tipo de autorizacion, siempre {@code Bearer}
 * @param expiresIn  validez del token en milisegundos
 * @param email      email del usuario autenticado
 * @param role       rol con el que se ha autenticado
 */
public record AuthResponse(String token, String tokenType, long expiresIn, String email, ERole role) {
}
