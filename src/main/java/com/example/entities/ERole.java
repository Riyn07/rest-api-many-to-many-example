package com.example.entities;

/**
 * Roles de la aplicacion. El prefijo ROLE_ lo anade Spring Security al mapear
 * las autoridades, por lo que en las reglas de autorizacion se usan
 * hasRole("ADMIN") / hasAnyRole("USER", "ADMIN").
 */
public enum ERole {
    USER,
    ADMIN
}