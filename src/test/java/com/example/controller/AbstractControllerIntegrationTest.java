package com.example.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.entities.ERole;
import com.example.entities.User;
import com.example.security.JwtTokenProvider;
import com.example.services.UserService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Base comun de los tests de integracion de controladores.
 *
 * <p>Se arranca el contexto completo (@SpringBootTest), de modo que la cadena
 * de Spring Security, el filtro JWT y los servicios intervienen de verdad: lo
 * que se verifica es el comportamiento observable por HTTP, no una simulacion.
 *
 * <p>La clase es {@code @Transactional} y MockMvc ejecuta la peticion en el
 * mismo hilo, de modo que las peticiones comparten la transaccion del test y
 * todo lo escrito se revierte al terminar cada metodo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class AbstractControllerIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin.integration@test.com";
    protected static final String USER_EMAIL = "user.integration@test.com";
    protected static final String PASSWORD = "Secreto1!";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserService userService;

    @Autowired
    protected JwtTokenProvider tokenProvider;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String adminToken;
    protected String userToken;

    @BeforeEach
    void crearUsuarios() {
        adminToken = tokenDe(ADMIN_EMAIL, ERole.ADMIN);
        userToken = tokenDe(USER_EMAIL, ERole.USER);
    }

    /** Crea el usuario si hace falta y devuelve un token JWT real para el. */
    private String tokenDe(String email, ERole role) {
        if (!userService.existsByEmail(email)) {
            userService.create(email, PASSWORD, role);
        }

        User user = userService.getByEmail(email);
        return tokenProvider.generateToken(user);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    /** Hace login contra /api/auth/login y devuelve el token resultante. */
    protected String loginYExtraerToken(String email, String password) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("email", email);
        body.put("password", password);

        String json = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(json).get("token").asText();
    }

    /**
     * Construye un objeto JSON a partir de pares alternos clave/valor, p.ej.
     * {@code json("title", "Introduccion", "published", true)}.
     *
     * <p>El tipo del valor se respeta: un {@code Boolean} se escribe como
     * literal JSON {@code true}, no como la cadena {@code "true"}.
     */
    protected ObjectNode json(Object... paresClaveValor) {
        if (paresClaveValor.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Se esperan pares clave/valor, se han recibido " + paresClaveValor.length + " argumentos");
        }

        ObjectNode node = objectMapper.createObjectNode();

        for (int i = 0; i < paresClaveValor.length; i += 2) {
            String clave = String.valueOf(paresClaveValor[i]);
            Object valor = paresClaveValor[i + 1];

            if (valor instanceof Boolean booleano) {
                node.put(clave, booleano);
            } else if (valor instanceof Integer entero) {
                node.put(clave, entero);
            } else if (valor instanceof Long largo) {
                node.put(clave, largo.longValue());
            } else {
                node.put(clave, valor == null ? null : valor.toString());
            }
        }

        return node;
    }
}
