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

import com.example.entities.Tag;
import com.example.entities.Tutorial;
import com.example.repository.TagRepository;
import com.example.repository.TutorialRepository;

@DisplayName("TagController (integracion con Spring Security + JWT)")
class TagControllerSecurityIntegrationTest extends AbstractControllerIntegrationTest {

    private static final String TAGS = "/api/tags";
    private static final String TUTORIALS = "/api/tutorials";

    @Autowired
    private TutorialRepository tutorialRepository;

    @Autowired
    private TagRepository tagRepository;

    private Tutorial tutorial;

    @BeforeEach
    void crearTutorial() {
        tutorial = tutorialRepository.save(Tutorial.builder()
                .title("Tutorial con tags")
                .description("Descripcion")
                .published(true)
                .build());
    }

    private String tagsDeTutorial() {
        return TUTORIALS + "/" + tutorial.getId() + "/tags";
    }

    // ---------------------------------------------------------------- lectura

    @Test
    @DisplayName("GET /tags sin token devuelve 401")
    void getTagsWithoutToken() throws Exception {
        mockMvc.perform(get(TAGS)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /tags con token de USER devuelve 200")
    void getTagsAsUser() throws Exception {
        tagRepository.save(Tag.builder().name("java").build());

        mockMvc.perform(get(TAGS).header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("java"));
    }

    @Test
    @DisplayName("GET /tags sin tags devuelve 204")
    void getTagsWhenEmpty() throws Exception {
        mockMvc.perform(get(TAGS).header("Authorization", bearer(userToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /tags/{id} inexistente devuelve 404")
    void getTagByIdNotFound() throws Exception {
        mockMvc.perform(get(TAGS + "/999999").header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET de los tags de un tutorial inexistente devuelve 404")
    void getTagsByTutorialNotFound() throws Exception {
        mockMvc.perform(get(TUTORIALS + "/999999/tags").header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------- asociar tags

    @Test
    @DisplayName("POST de un tag con token de USER devuelve 403")
    void addTagAsUserIsForbidden() throws Exception {
        mockMvc.perform(post(tagsDeTutorial())
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "intentos").toString()))
                .andExpect(status().isForbidden());

        assertThat(tagRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("POST de un tag nuevo con token de ADMIN devuelve 201 y persiste la asociacion")
    void addNewTagAsAdminPersistsAssociation() throws Exception {
        mockMvc.perform(post(tagsDeTutorial())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "java").toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("java"))
                .andExpect(jsonPath("$.id").isNumber());

        // El tag se creo...
        assertThat(tagRepository.findAll()).hasSize(1);

        // ...y la fila de la tabla puente tambien: este es el caso que antes
        // no persistia la relacion.
        mockMvc.perform(get(tagsDeTutorial()).header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("java"));
    }

    @Test
    @DisplayName("POST de un tag ya existente reutiliza ese tag y no crea otro")
    void addExistingTagAsAdmin() throws Exception {
        Tag existente = tagRepository.save(Tag.builder().name("java").build());

        mockMvc.perform(post(tagsDeTutorial())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("id", String.valueOf(existente.getId())).toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("java"));

        assertThat(tagRepository.findAll()).hasSize(1);

        mockMvc.perform(get(tagsDeTutorial()).header("Authorization", bearer(userToken)))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("POST de un tag sobre un tutorial inexistente devuelve 404")
    void addTagToMissingTutorial() throws Exception {
        mockMvc.perform(post(TUTORIALS + "/999999/tags")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "java").toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST de un tag con id inexistente devuelve 404")
    void addMissingExistingTag() throws Exception {
        mockMvc.perform(post(tagsDeTutorial())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("id", "999999").toString()))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------- actualizar/borrar

    @Test
    @DisplayName("PUT de un tag con token de USER devuelve 403")
    void updateTagAsUserIsForbidden() throws Exception {
        Tag tag = tagRepository.save(Tag.builder().name("java").build());

        mockMvc.perform(put(TAGS + "/" + tag.getId())
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "kotlin").toString()))
                .andExpect(status().isForbidden());

        assertThat(tagRepository.findById(tag.getId()).orElseThrow().getName()).isEqualTo("java");
    }

    @Test
    @DisplayName("PUT de un tag con token de ADMIN actualiza el nombre")
    void updateTagAsAdmin() throws Exception {
        Tag tag = tagRepository.save(Tag.builder().name("java").build());

        mockMvc.perform(put(TAGS + "/" + tag.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "kotlin").toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("kotlin"));
    }

    @Test
    @DisplayName("DELETE de un tag con token de USER devuelve 403")
    void deleteTagAsUserIsForbidden() throws Exception {
        Tag tag = tagRepository.save(Tag.builder().name("java").build());

        mockMvc.perform(delete(TAGS + "/" + tag.getId())
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());

        assertThat(tagRepository.findById(tag.getId())).isPresent();
    }

    @Test
    @DisplayName("DELETE de un tag con token de ADMIN devuelve 204 y limpia la tabla puente")
    void deleteTagAsAdminCleansJoinTable() throws Exception {
        Tag tag = tagRepository.save(Tag.builder().name("java").build());
        tutorial.addTag(tag);
        tutorialRepository.save(tutorial);

        mockMvc.perform(delete(TAGS + "/" + tag.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        assertThat(tagRepository.findById(tag.getId())).isEmpty();

        // Tras borrar el tag, el tutorial ya no debe arrastrarlo.
        mockMvc.perform(get(tagsDeTutorial()).header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("DELETE de un tag inexistente devuelve 404")
    void deleteTagNotFound() throws Exception {
        mockMvc.perform(delete(TAGS + "/999999").header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE de la asociacion con token de USER devuelve 403")
    void removeAssociationAsUserIsForbidden() throws Exception {
        Tag tag = tagRepository.save(Tag.builder().name("java").build());
        tutorial.addTag(tag);
        tutorialRepository.save(tutorial);

        mockMvc.perform(delete(tagsDeTutorial() + "/" + tag.getId())
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE de la asociacion con token de ADMIN devuelve 204")
    void removeAssociationAsAdmin() throws Exception {
        Tag tag = tagRepository.save(Tag.builder().name("java").build());
        tutorial.addTag(tag);
        tutorialRepository.save(tutorial);

        mockMvc.perform(delete(tagsDeTutorial() + "/" + tag.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(tagsDeTutorial()).header("Authorization", bearer(userToken)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("DELETE de una asociacion inexistente devuelve 404")
    void removeMissingAssociation() throws Exception {
        Tag tag = tagRepository.save(Tag.builder().name("java").build());

        mockMvc.perform(delete(tagsDeTutorial() + "/" + tag.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /tags/{id}/tutorials devuelve los tutoriales del tag")
    void getTutorialsByTag() throws Exception {
        Tag tag = tagRepository.save(Tag.builder().name("java").build());
        tutorial.addTag(tag);
        tutorialRepository.save(tutorial);

        mockMvc.perform(get(TAGS + "/" + tag.getId() + "/tutorials")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Tutorial con tags"));
    }
}