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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.entities.Tag;
import com.example.entities.Tutorial;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.TutorialRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TutorialServiceImpl")
class TutorialServiceImplTest {

    @Mock
    private TutorialRepository tutorialRepository;

    @InjectMocks
    private TutorialServiceImpl tutorialService;

    private static Tutorial tutorial(long id, String title, boolean published) {
        Tutorial tutorial = Tutorial.builder()
                .title(title)
                .description("descripcion de " + title)
                .published(published)
                .build();
        tutorial.setId(id);
        return tutorial;
    }

    @Test
    @DisplayName("findAll(null) devuelve todos los tutoriales")
    void findAllWithoutFilter() {
        List<Tutorial> all = List.of(tutorial(1L, "Uno", true), tutorial(2L, "Dos", false));
        when(tutorialRepository.findAll()).thenReturn(all);

        assertThat(tutorialService.findAll(null)).hasSize(2);
        verify(tutorialRepository).findAll();
        verify(tutorialRepository, never()).findByTitleContaining(any());
    }

    @Test
    @DisplayName("findAll(\"\") trata el titulo vacio como ausencia de filtro")
    void findAllWithBlankTitleIsTreatedAsNoFilter() {
        when(tutorialRepository.findAll()).thenReturn(List.of(tutorial(1L, "Uno", true)));

        assertThat(tutorialService.findAll("   ")).hasSize(1);
        verify(tutorialRepository).findAll();
    }

    @Test
    @DisplayName("findAll(titulo) delega en la busqueda por fragmento")
    void findAllWithFilter() {
        when(tutorialRepository.findByTitleContaining("spring"))
                .thenReturn(List.of(tutorial(1L, "Introduccion a Spring", true)));

        assertThat(tutorialService.findAll("spring")).hasSize(1);
        verify(tutorialRepository).findByTitleContaining("spring");
    }

    @Test
    @DisplayName("findById() devuelve el tutorial existente")
    void findById() {
        when(tutorialRepository.findById(1L)).thenReturn(Optional.of(tutorial(1L, "Uno", true)));

        assertThat(tutorialService.findById(1L).getTitle()).isEqualTo("Uno");
    }

    @Test
    @DisplayName("findById() lanza ResourceNotFoundException si no existe")
    void findByIdWhenNotFound() {
        when(tutorialRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tutorialService.findById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    @DisplayName("create() respeta el campo published del cuerpo de la peticion")
    void createHonoursPublished() {
        ArgumentCaptor<Tutorial> captor = ArgumentCaptor.forClass(Tutorial.class);
        when(tutorialRepository.save(any(Tutorial.class))).thenAnswer(inv -> inv.getArgument(0));

        Tutorial resultado = tutorialService.create(
                Tutorial.builder().title("Nuevo").description("d").published(false).build());

        verify(tutorialRepository).save(captor.capture());
        assertThat(captor.getValue().isPublished()).isFalse();
        assertThat(resultado.isPublished()).isFalse();
    }

    @Test
    @DisplayName("create() guarda tambien el titulo y la descripcion")
    void createSavesFields() {
        ArgumentCaptor<Tutorial> captor = ArgumentCaptor.forClass(Tutorial.class);
        when(tutorialRepository.save(any(Tutorial.class))).thenAnswer(inv -> inv.getArgument(0));

        tutorialService.create(Tutorial.builder().title("Titulo").description("Desc").published(true).build());

        verify(tutorialRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Titulo");
        assertThat(captor.getValue().getDescription()).isEqualTo("Desc");
    }

    @Test
    @DisplayName("update() modifica el tutorial existente y lo guarda")
    void update() {
        Tutorial existente = tutorial(1L, "Titulo viejo", false);
        when(tutorialRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(tutorialRepository.save(any(Tutorial.class))).thenAnswer(inv -> inv.getArgument(0));

        Tutorial resultado = tutorialService.update(1L,
                Tutorial.builder().title("Titulo nuevo").description("Nueva desc").published(true).build());

        assertThat(resultado.getTitle()).isEqualTo("Titulo nuevo");
        assertThat(resultado.getDescription()).isEqualTo("Nueva desc");
        assertThat(resultado.isPublished()).isTrue();
        assertThat(resultado.getId()).isEqualTo(1L);
        verify(tutorialRepository).save(existente);
    }

    @Test
    @DisplayName("update() lanza ResourceNotFoundException si no existe")
    void updateWhenNotFound() {
        when(tutorialRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tutorialService.update(7L, Tutorial.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("borra el tutorial")
        void deletesTutorial() {
            Tutorial existente = tutorial(1L, "Uno", true);
            when(tutorialRepository.findById(1L)).thenReturn(Optional.of(existente));

            tutorialService.delete(1L);

            verify(tutorialRepository).delete(existente);
        }

        @Test
        @DisplayName("desacopla antes los tags para limpiar la tabla puente")
        void detachesTagsFirst() {
            Tutorial existente = tutorial(1L, "Uno", true);
            Tag tag = Tag.builder().name("java").build();
            tag.setId(9L);
            existente.addTag(tag);

            when(tutorialRepository.findById(1L)).thenReturn(Optional.of(existente));

            tutorialService.delete(1L);

            assertThat(existente.getTags()).isEmpty();
            assertThat(tag.getTutorials()).isEmpty();
            verify(tutorialRepository).delete(existente);
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException si no existe")
        void whenNotFound() {
            when(tutorialRepository.findById(3L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tutorialService.delete(3L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    @DisplayName("deleteAll() desacopla los tags de todos los tutoriales antes de borrar")
    void deleteAll() {
        Tutorial uno = tutorial(1L, "Uno", true);
        Tag tag = Tag.builder().name("java").build();
        tag.setId(5L);
        uno.addTag(tag);

        Tutorial dos = tutorial(2L, "Dos", true);
        when(tutorialRepository.findAll()).thenReturn(List.of(uno, dos));

        tutorialService.deleteAll();

        assertThat(uno.getTags()).isEmpty();
        assertThat(tag.getTutorials()).isEmpty();
        verify(tutorialRepository).deleteAll();
    }

    @Test
    @DisplayName("findByPublished() delega en el repositorio")
    void findByPublished() {
        when(tutorialRepository.findByPublished(true)).thenReturn(List.of(tutorial(1L, "Uno", true)));

        assertThat(tutorialService.findByPublished(true)).hasSize(1);
    }
}