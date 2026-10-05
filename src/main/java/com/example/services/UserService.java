package com.example.services;

import com.example.entities.ERole;
import com.example.entities.User;

public interface UserService {

    /**
     * Registra un usuario cifrando su contrasena con BCrypt.
     *
     * @throws com.example.exception.ResourceConflictException si el email ya esta registrado
     */
    User create(String email, String rawPassword, ERole role);

    /** @throws com.example.exception.ResourceNotFoundException si el email no existe */
    User getByEmail(String email);

    boolean existsByEmail(String email);

    /** Normaliza el email para que el login no dependa de mayusculas/espacios. */
    String normalizeEmail(String email);
}
