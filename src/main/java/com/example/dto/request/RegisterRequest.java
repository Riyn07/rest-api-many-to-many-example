package com.example.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Peticion de registro de usuario. Todas las anotaciones de validacion se
 * evaluan a la vez, de modo que la respuesta de error contiene la lista
 * completa de campos invalidos y no solo el primero.
 */
public record RegisterRequest(

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido, se espera algo como usuario@dominio.com")
        @Size(max = 190, message = "El email no puede superar los 190 caracteres")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 50, message = "La contrasena debe tener entre 8 y 50 caracteres")
        @Pattern(regexp = PASSWORD_REGEX, message = "La contrasena debe incluir al menos una minuscula, una mayuscula, un digito y un simbolo")
        String password) {

    /**
     * Exigencia de al menos una minuscula, una mayuscula, un digito y un
     * caracter especial, con longitud entre 8 y 50.
     */
    private static final String PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,50}$";
}