package com.example.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.example.entities.ERole;

import tools.jackson.databind.node.ObjectNode;

@DisplayName("AuthController (integracion con Spring Security + JWT)")
class AuthControllerIntegrationTest extends AbstractControllerIntegrationTest {

    private static final String REGISTER = "/api/auth/register";
    private static final String LOGIN = "/api/auth/login";

    @Test
    @DisplayName("register() crea un usuario con rol USER y no devuelve la contrasena")
    void registerOk() throws Exception {
        String email = "nuevo.integration@test.com";

        mockMvc.perform(post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", email, "password", PASSWORD).toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value(ERole.USER.name()))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());

        assertThat(userService.existsByEmail(email)).isTrue();
    }

    @Test
    @DisplayName("register() es publico: no hace falta token")
    void registerIsPublic() throws Exception {
        mockMvc.perform(post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", "publico@test.com", "password", PASSWORD).toString()))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("register() normaliza el email a minusculas")
    void registerNormalisesEmail() throws Exception {
        mockMvc.perform(post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", "Mayusculas@Test.COM", "password", PASSWORD).toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("mayusculas@test.com"));
    }

    @Test
    @DisplayName("register() rechaza con 400 un email con espacios alrededor")
    void registerRejectsPaddedEmail() throws Exception {
        mockMvc.perform(post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", "  conEspacios@Test.COM  ", "password", PASSWORD).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists());
    }

    @Test
    @DisplayName("register() devuelve 400 con TODOS los campos invalidos, no solo el primero")
    void registerReturnsAllValidationErrors() throws Exception {
        ObjectNode invalido = objectMapper.createObjectNode();
        invalido.put("email", "no-es-un-email");
        invalido.put("password", "corta");

        mockMvc.perform(post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalido.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                // email: formato invalido y password: longitud insuficiente
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    @DisplayName("register() devuelve 400 si faltan campos obligatorios")
    void registerWithMissingFields() throws Exception {
        mockMvc.perform(post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists());
    }

    @Test
    @DisplayName("register() devuelve 400 con un email vacio en blanco")
    void registerWithBlankEmail() throws Exception {
        mockMvc.perform(post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", "   ", "password", PASSWORD).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    @DisplayName("register() devuelve 400 con JSON mal formado")
    void registerWithMalformedJson() throws Exception {
        mockMvc.perform(post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"a@b.com\", \"password\": }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("register() devuelve 409 si el email ya esta registrado")
    void registerDuplicateEmail() throws Exception {
        mockMvc.perform(post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", USER_EMAIL, "password", PASSWORD).toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409));
    }

    @Test
    @DisplayName("login() autentica por email y devuelve un token Bearer usable")
    void loginOk() throws Exception {
        mockMvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", USER_EMAIL, "password", PASSWORD).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.role").value(ERole.USER.name()))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("login() acepta el email escrito en mayusculas")
    void loginIsCaseInsensitive() throws Exception {
        mockMvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", USER_EMAIL.toUpperCase(), "password", PASSWORD).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("el token devuelto por login() sirve para llamar a un endpoint protegido")
    void loginTokenWorks() throws Exception {
        String token = loginYExtraerToken(USER_EMAIL, PASSWORD);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("login() devuelve 401 con contrasena incorrecta")
    void loginWrongPassword() throws Exception {
        mockMvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", USER_EMAIL, "password", "ContrasenaMala1!").toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401));
    }

    @Test
    @DisplayName("login() devuelve 401 con email desconocido")
    void loginUnknownEmail() throws Exception {
        mockMvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", "nadie.integration@test.com", "password", PASSWORD).toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login() devuelve 400 con todos los campos invalidos")
    void loginValidationErrors() throws Exception {
        ObjectNode invalido = objectMapper.createObjectNode();
        invalido.put("email", "esto-no-es-un-email");
        invalido.put("password", "");

        mockMvc.perform(post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalido.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists());
    }

    @Test
    @DisplayName("el ADMIN de bootstrap puede autenticarse y escribir; es el unico camino a ese rol")
    void bootstrapAdminCanLogInAndWrite() throws Exception {
        String adminBootstrapEmail = "admin@test.com";
        String adminBootstrapPassword = "AdminTest2026!";

        String token = loginYExtraerToken(adminBootstrapEmail, adminBootstrapPassword);
        assertThat(token).isNotBlank();

        // Con ese token puede crear, es decir, tiene rol ADMIN de verdad.
        mockMvc.perform(post("/api/tutorials")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", "Creado por el admin de bootstrap",
                                "description", "d", "published", true).toString()))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("el alta publica nunca concede el rol ADMIN")
    void publicRegisterCannotCreateAdmin() throws Exception {
        mockMvc.perform(post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("email", "falso-admin@test.com", "password", PASSWORD,
                        "role", "ADMIN").toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value(ERole.USER.name()));
    }
}
