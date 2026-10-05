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
@DisplayName("TagRepository")
class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TutorialRepository tutorialRepository;

    @BeforeEach
    void setUp() {
        tutorialRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    @DisplayName("save() persiste un tag y findById() lo recupera")
    void saveAndFindById() {
        Tag saved = tagRepository.save(Tag.builder().name("docker").build());

        assertThat(saved.getId()).isPositive();
        assertThat(tagRepository.findById(saved.getId()))
                .hasValueSatisfying(tag -> assertThat(tag.getName()).isEqualTo("docker"));
    }

    @Test
    @DisplayName("findAll() devuelve todos los tags")
    void findAll() {
        tagRepository.save(Tag.builder().name("java").build());
        tagRepository.save(Tag.builder().name("sql").build());

        assertThat(tagRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("findTagsByTutorialsId() devuelve los tags asociados a un tutorial")
    void findTagsByTutorialsId() {
        Tag java = tagRepository.save(Tag.builder().name("java").build());
        Tag sql = tagRepository.save(Tag.builder().name("sql").build());

        Tutorial tutorial = Tutorial.builder().title("JPA basico").published(true).build();
        tutorial.addTag(java);
        tutorial.addTag(sql);
        tutorialRepository.save(tutorial);

        List<Tag> tags = tagRepository.findTagsByTutorialsId(tutorial.getId());

        assertThat(tags).hasSize(2).extracting(Tag::getName)
                .containsExactlyInAnyOrder("java", "sql");
    }

    @Test
    @DisplayName("findTagsByTutorialsId() devuelve vacio si el tutorial no tiene tags")
    void findTagsByTutorialsIdWhenNoTags() {
        Tutorial tutorial = Tutorial.builder().title("Sin tags").published(true).build();
        tutorialRepository.save(tutorial);

        assertThat(tagRepository.findTagsByTutorialsId(tutorial.getId())).isEmpty();
    }

    @Test
    @DisplayName("deleteById() elimina el tag")
    void deleteById() {
        Tag saved = tagRepository.save(Tag.builder().name("temporal").build());
        long id = saved.getId();

        tagRepository.deleteById(id);
        tagRepository.flush();

        assertThat(tagRepository.findById(id)).isEmpty();
    }
}
