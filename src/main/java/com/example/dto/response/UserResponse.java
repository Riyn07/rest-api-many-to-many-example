package com.example.dto.response;

import com.example.entities.ERole;

/**
 * Datos publicos de un usuario. Nunca incluye la contrasena.
 */
public record UserResponse(long id, String email, ERole role, boolean enabled) {

    public static UserResponse from(com.example.entities.User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.isEnabled());
    }
}
