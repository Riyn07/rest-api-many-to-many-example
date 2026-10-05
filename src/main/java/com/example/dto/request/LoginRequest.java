package com.example.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Peticion de login. La credencial de acceso es el <b>email</b>, no un
 * nombre de usuario.
 */
public record LoginRequest(

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido, se espera algo como usuario@dominio.com")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        String password) {
}
