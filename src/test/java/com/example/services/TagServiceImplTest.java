package com.example.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.entities.Tag;
import com.example.entities.Tutorial;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.TagRepository;
import com.example.repository.TutorialRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagServiceImpl")
class TagServiceImplTest {

    @Mock
    private TutorialRepository tutorialRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagServiceImpl tagService;

    private static Tutorial tutorial(long id) {
        Tutorial tutorial = Tutorial.builder().title("Tutorial " + id).published(true).build();
        tutorial.setId(id);
        return tutorial;
    }

    private static Tag tag(long id, String name) {
        Tag tag = Tag.builder().name(name).build();
        tag.setId(id);
        return tag;
    }

    @Test
    @DisplayName("findAll() devuelve todos los tags")
    void findAll() {
        when(tagRepository.findAll()).thenReturn(List.of(tag(1L, "java"), tag(2L, "sql")));

        assertThat(tagService.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("findByTutorialId() devuelve los tags del tutorial")
    void findByTutorialId() {
        when(tutorialRepository.existsById(1L)).thenReturn(true);
        when(tagRepository.findTagsByTutorialsId(1L)).thenReturn(List.of(tag(1L, "java")));

        assertThat(tagService.findByTutorialId(1L)).hasSize(1);
    }

    @Test
    @DisplayName("findByTutorialId() lanza ResourceNotFoundException si el tutorial no existe")
    void findByTutorialIdWhenTutorialMissing() {
        when(tutorialRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> tagService.findByTutorialId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("findById() devuelve el tag existente")
    void findById() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag(1L, "java")));

        assertThat(tagService.findById(1L).getName()).isEqualTo("java");
    }

    @Test
    @DisplayName("findById() lanza ResourceNotFoundException si no existe")
    void findByIdWhenNotFound() {
        when(tagRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.findById(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("findTutorialsByTagId() devuelve los tutoriales del tag")
    void findTutorialsByTagId() {
        when(tagRepository.existsById(3L)).thenReturn(true);
        when(tutorialRepository.findTutorialsByTagsId(3L)).thenReturn(List.of(tutorial(1L)));

        assertThat(tagService.findTutorialsByTagId(3L)).hasSize(1);
    }

    @Test
    @DisplayName("findTutorialsByTagId() lanza ResourceNotFoundException si el tag no existe")
    void findTutorialsByTagIdWhenTagMissing() {
        when(tagRepository.existsById(3L)).thenReturn(false);

        assertThatThrownBy(() -> tagService.findTutorialsByTagId(3L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("addTagToTutorial() reutiliza el tag existente cuando llega con id")
    void addExistingTag() {
        Tutorial existente = tutorial(1L);
        Tag java = tag(3L, "java");
        when(tutorialRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(tagRepository.findById(3L)).thenReturn(Optional.of(java));

        Tag resultado = tagService.addTagToTutorial(1L, tag(3L, "nombre-ignorado"));

        assertThat(resultado).isSameAs(java);
        assertThat(resultado.getId()).isEqualTo(3L);
        assertThat(existente.getTags()).extracting(Tag::getId).containsExactly(3L);
        verify(tutorialRepository).save(existente);
        // No debe crear un tag nuevo: el id informado ya identifica uno existente.
        verify(tagRepository, never()).save(any());
    }

    @Test
    @DisplayName("addTagToTutorial() lanza ResourceNotFoundException si el id del tag no existe")
    void addExistingTagWhenMissing() {
        when(tutorialRepository.findById(1L)).thenReturn(Optional.of(tutorial(1L)));
        when(tagRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.addTagToTutorial(1L, tag(3L, "java")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("addTagToTutorial() crea el tag si llega con id 0 y lo asocia al tutorial")
    void addNewTag() {
        Tutorial existente = tutorial(1L);
        when(tutorialRepository.findById(1L)).thenReturn(Optional.of(existente));
        ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
        when(tagRepository.save(captor.capture())).thenAnswer(invocation -> {
            Tag guardada = invocation.getArgument(0);
            guardada.setId(77L);
            return guardada;
        });

        Tag resultado = tagService.addTagToTutorial(1L, Tag.builder().name("nuevo").build());

        assertThat(captor.getValue().getName()).isEqualTo("nuevo");
        assertThat(resultado.getId()).isEqualTo(77L);
        // La asociacion debe quedar en el tutorial para que se escriba la
        // fila de la tabla puente tutorial_tags.
        assertThat(existente.getTags()).extracting(Tag::getId).containsExactly(77L);
        verify(tutorialRepository).save(existente);
    }

    @Test
    @DisplayName("addTagToTutorial() lanza ResourceNotFoundException si el tutorial no existe")
    void addTagWhenTutorialMissing() {
        when(tutorialRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.addTagToTutorial(9L, Tag.builder().name("x").build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update() cambia el nombre del tag")
    void update() {
        Tag existente = tag(1L, "java");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Tag resultado = tagService.update(1L, Tag.builder().name("kotlin").build());

        assertThat(resultado.getName()).isEqualTo("kotlin");
    }

    @Test
    @DisplayName("update() lanza ResourceNotFoundException si no existe")
    void updateWhenNotFound() {
        when(tagRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.update(1L, Tag.builder().name("x").build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("removeTagFromTutorial() desasocia el tag del tutorial")
    void removeTagFromTutorial() {
        Tutorial existente = tutorial(1L);
        Tag existenteTag = tag(3L, "java");
        existente.addTag(existenteTag);
        when(tutorialRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(tagRepository.findById(3L)).thenReturn(Optional.of(existenteTag));

        tagService.removeTagFromTutorial(1L, 3L);

        assertThat(existente.getTags()).isEmpty();
        assertThat(existenteTag.getTutorials()).isEmpty();
        verify(tutorialRepository).save(existente);
    }

    @Test
    @DisplayName("removeTagFromTutorial() lanza ResourceNotFoundException si no estaban asociados")
    void removeTagWhenNotAssociated() {
        Tutorial existente = tutorial(1L);
        when(tutorialRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(tagRepository.findById(3L)).thenReturn(Optional.of(tag(3L, "java")));

        assertThatThrownBy(() -> tagService.removeTagFromTutorial(1L, 3L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no esta asociado");
    }

    @Test
    @DisplayName("removeTagFromTutorial() lanza ResourceNotFoundException si el tag no existe")
    void removeTagWhenTagMissing() {
        when(tutorialRepository.findById(1L)).thenReturn(Optional.of(tutorial(1L)));
        when(tagRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.removeTagFromTutorial(1L, 3L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("removeTagFromTutorial() lanza ResourceNotFoundException si el tutorial no existe")
    void removeTagWhenTutorialMissing() {
        when(tutorialRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.removeTagFromTutorial(1L, 3L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete() desacopla el tag de cada tutorial antes de borrarlo")
    void deleteDetachesFromTutorials() {
        Tag objetivo = tag(3L, "java");
        Tutorial conTag = tutorial(1L);
        conTag.addTag(objetivo);
        when(tagRepository.findById(3L)).thenReturn(Optional.of(objetivo));

        tagService.delete(3L);

        assertThat(conTag.getTags()).isEmpty();
        verify(tutorialRepository).save(conTag);
        verify(tagRepository).delete(objetivo);
    }

    @Test
    @DisplayName("delete() borra un tag sin tutoriales asociados")
    void deleteWithoutTutorials() {
        Tag objetivo = tag(3L, "java");
        when(tagRepository.findById(3L)).thenReturn(Optional.of(objetivo));

        tagService.delete(3L);

        verify(tagRepository).delete(objetivo);
        verify(tutorialRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete() lanza ResourceNotFoundException si no existe")
    void deleteWhenNotFound() {
        when(tagRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.delete(3L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}