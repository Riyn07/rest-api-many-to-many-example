package com.example.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.example.entities.Tutorial;
import com.example.repository.TutorialRepository;

@DisplayName("TutorialController (integracion con Spring Security + JWT)")
class TutorialControllerSecurityIntegrationTest extends AbstractControllerIntegrationTest {

    private static final String TUTORIALS = "/api/tutorials";

    @Autowired
    private TutorialRepository tutorialRepository;

    private Tutorial tutorial;

    @BeforeEach
    void crearTutorial() {
        tutorial = tutorialRepository.save(Tutorial.builder()
                .title("Tutorial de prueba")
                .description("Descripcion")
                .published(true)
                .build());
    }

    // ---------------------------------------------------------------- lectura

    @Test
    @DisplayName("GET sin token devuelve 401")
    void getWithoutToken() throws Exception {
        mockMvc.perform(get(TUTORIALS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401));
    }

    @Test
    @DisplayName("GET con token invalido devuelve 401")
    void getWithInvalidToken() throws Exception {
        mockMvc.perform(get(TUTORIALS).header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET con token manipulado (firma alterada) devuelve 401")
    void getWithTamperedToken() throws Exception {
        String manipulado = userToken.substring(0, userToken.length() - 3) + "abc";

        mockMvc.perform(get(TUTORIALS).header("Authorization", bearer(manipulado)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET sin cabecera Bearer bien formada devuelve 401")
    void getWithMalformedAuthorizationHeader() throws Exception {
        mockMvc.perform(get(TUTORIALS).header("Authorization", userToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET con token de USER devuelve 200")
    void getAsUser() throws Exception {
        mockMvc.perform(get(TUTORIALS).header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Tutorial de prueba"));
    }

    @Test
    @DisplayName("GET con token de ADMIN devuelve 200")
    void getAsAdmin() throws Exception {
        mockMvc.perform(get(TUTORIALS).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET por id con token de USER devuelve 200")
    void getByIdAsUser() throws Exception {
        mockMvc.perform(get(TUTORIALS + "/" + tutorial.getId())
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tutorial de prueba"));
    }

    @Test
    @DisplayName("GET por id inexistente devuelve 404")
    void getByIdNotFound() throws Exception {
        mockMvc.perform(get(TUTORIALS + "/999999")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET con id no numerico devuelve 400")
    void getByInvalidId() throws Exception {
        mockMvc.perform(get(TUTORIALS + "/abc")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET filtrando por titulo devuelve 200")
    void getFilteredByTitle() throws Exception {
        mockMvc.perform(get(TUTORIALS)
                        .param("title", "prueba")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Tutorial de prueba"));
    }

    // -------------------------------------------------------------- escritura

    @Test
    @DisplayName("POST con token de USER devuelve 403")
    void createAsUserIsForbidden() throws Exception {
        mockMvc.perform(post(TUTORIALS)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", "No permitido", "description", "d", "published", "true").toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403));
    }

    @Test
    @DisplayName("POST sin token devuelve 401")
    void createWithoutToken() throws Exception {
        mockMvc.perform(post(TUTORIALS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", "Sin token", "description", "d", "published", "true").toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST con token de ADMIN devuelve 201")
    void createAsAdmin() throws Exception {
        mockMvc.perform(post(TUTORIALS)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", "Creado por admin", "description", "d", "published", "true").toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Creado por admin"));
    }

    @Test
    @DisplayName("PUT con token de USER devuelve 403")
    void updateAsUserIsForbidden() throws Exception {
        mockMvc.perform(put(TUTORIALS + "/" + tutorial.getId())
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", "Cambiado", "description", "d2", "published", "false").toString()))
                .andExpect(status().isForbidden());

        assertThatTituloNoCambiado();
    }

    @Test
    @DisplayName("PUT con token de ADMIN actualiza y devuelve 200")
    void updateAsAdmin() throws Exception {
        mockMvc.perform(put(TUTORIALS + "/" + tutorial.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", "Actualizado", "description", "d2", "published", "false").toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Actualizado"))
                .andExpect(jsonPath("$.published").value(false));
    }

    @Test
    @DisplayName("DELETE con token de USER devuelve 403 y no borra")
    void deleteAsUserIsForbidden() throws Exception {
        mockMvc.perform(delete(TUTORIALS + "/" + tutorial.getId())
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());

        assertThatTituloNoCambiado();
        assertThat(tutorialRepository.findById(tutorial.getId())).isPresent();
    }

    @Test
    @DisplayName("DELETE con token de ADMIN devuelve 204")
    void deleteAsAdmin() throws Exception {
        mockMvc.perform(delete(TUTORIALS + "/" + tutorial.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        assertThat(tutorialRepository.findById(tutorial.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE de un tutorial inexistente devuelve 404")
    void deleteNotFound() throws Exception {
        mockMvc.perform(delete(TUTORIALS + "/999999")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE de todos los tutoriales con token de USER devuelve 403")
    void deleteAllAsUserIsForbidden() throws Exception {
        mockMvc.perform(delete(TUTORIALS).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /published devuelve 200 para un usuario autenticado")
    void findPublished() throws Exception {
        mockMvc.perform(get(TUTORIALS + "/published")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Tutorial de prueba"));
    }

    private void assertThatTituloNoCambiado() throws Exception {
        assertThat(tutorialRepository.findById(tutorial.getId()).orElseThrow().getTitle())
                .isEqualTo("Tutorial de prueba");
    }
}
