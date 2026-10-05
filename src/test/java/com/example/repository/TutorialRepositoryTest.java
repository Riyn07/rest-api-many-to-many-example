package com.example.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.example.entities.Tag;
import com.example.entities.Tutorial;

@DataJpaTest
@DisplayName("TutorialRepository")
class TutorialRepositoryTest {

    @Autowired
    private TutorialRepository tutorialRepository;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    void setUp() {
        tutorialRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    @DisplayName("save() persiste un tutorial y findById() lo recupera")
    void saveAndFindById() {
        Tutorial saved = tutorialRepository.save(Tutorial.builder()
                .title("Spring Boot")
                .description("Introduccion a Spring Boot")
                .published(true)
                .build());

        assertThat(saved.getId()).isPositive();
        assertThat(tutorialRepository.findById(saved.getId()))
                .hasValueSatisfying(t -> {
                    assertThat(t.getTitle()).isEqualTo("Spring Boot");
                    assertThat(t.isPublished()).isTrue();
                });
    }

    @Test
    @DisplayName("findById() devuelve vacio cuando el id no existe")
    void findByIdWhenNotFound() {
        assertThat(tutorialRepository.findById(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("findAll() devuelve todos los tutoriales")
    void findAll() {
        tutorialRepository.save(Tutorial.builder().title("Uno").published(true).build());
        tutorialRepository.save(Tutorial.builder().title("Dos").published(false).build());

        assertThat(tutorialRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("findByPublished() filtra por el estado de publicacion")
    void findByPublished() {
        tutorialRepository.save(Tutorial.builder().title("Publicado").published(true).build());
        tutorialRepository.save(Tutorial.builder().title("Borrador").published(false).build());

        List<Tutorial> published = tutorialRepository.findByPublished(true);

        assertThat(published).hasSize(1);
        assertThat(published.get(0).getTitle()).isEqualTo("Publicado");
        assertThat(tutorialRepository.findByPublished(false)).hasSize(1);
    }

    @Test
    @DisplayName("findByTitleContaining() busca por fragmento del titulo, sin distinguir mayusculas")
    void findByTitleContaining() {
        tutorialRepository.save(Tutorial.builder().title("Introduccion a Spring").published(true).build());
        tutorialRepository.save(Tutorial.builder().title("Introduccion a JPA").published(true).build());
        tutorialRepository.save(Tutorial.builder().title("Angular").published(true).build());

        List<Tutorial> found = tutorialRepository.findByTitleContaining("spring");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getTitle()).isEqualTo("Introduccion a Spring");
    }

    @Test
    @DisplayName("findTutorialsByTagsId() devuelve los tutoriales que llevan ese tag")
    void findTutorialsByTagsId() {
        Tag java = tagRepository.save(Tag.builder().name("java").build());
        Tag hibernate = tagRepository.save(Tag.builder().name("hibernate").build());

        Tutorial conJava = Tutorial.builder().title("Con java").published(true).build();
        conJava.addTag(java);
        tutorialRepository.save(conJava);

        Tutorial sinTags = Tutorial.builder().title("Sin tags").published(true).build();
        tutorialRepository.save(sinTags);

        List<Tutorial> resultado = tutorialRepository.findTutorialsByTagsId(java.getId());

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTitle()).isEqualTo("Con java");
        assertThat(tutorialRepository.findTutorialsByTagsId(hibernate.getId())).isEmpty();
    }

    @Test
    @DisplayName("deleteById() elimina el tutorial")
    void deleteById() {
        Tutorial saved = tutorialRepository.save(Tutorial.builder().title("Temporal").published(true).build());
        long id = saved.getId();

        tutorialRepository.deleteById(id);
        tutorialRepository.flush();

        assertThat(tutorialRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("deleteAll() vacia el repositorio")
    void deleteAll() {
        tutorialRepository.save(Tutorial.builder().title("Uno").published(true).build());
        tutorialRepository.save(Tutorial.builder().title("Dos").published(true).build());

        tutorialRepository.deleteAll();
        tutorialRepository.flush();

        assertThat(tutorialRepository.findAll()).isEmpty();
    }
}
