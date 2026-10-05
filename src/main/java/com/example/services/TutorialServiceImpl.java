package com.example.services;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entities.Tutorial;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.TutorialRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TutorialServiceImpl implements TutorialService {

    private final TutorialRepository tutorialRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Tutorial> findAll(String title) {
        if (title == null || title.isBlank()) {
            return tutorialRepository.findAll();
        }

        return tutorialRepository.findByTitleContaining(title);
    }

    @Override
    @Transactional(readOnly = true)
    public Tutorial findById(long id) {
        return tutorialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Tutorial with id = " + id));
    }

    @Override
    @Transactional
    public Tutorial create(Tutorial tutorial) {
        // Se respeta el campo published que llega en el cuerpo de la peticion.
        return tutorialRepository.save(
                Tutorial.builder()
                        .title(tutorial.getTitle())
                        .description(tutorial.getDescription())
                        .published(tutorial.isPublished())
                        .build());
    }

    @Override
    @Transactional
    public Tutorial update(long id, Tutorial tutorial) {
        Tutorial existing = tutorialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Tutorial with id = " + id));

        existing.setTitle(tutorial.getTitle());
        existing.setDescription(tutorial.getDescription());
        existing.setPublished(tutorial.isPublished());

        return tutorialRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(long id) {
        Tutorial tutorial = tutorialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Tutorial with id = " + id));

        // Desacoplar cada tag antes de borrar deja limpias las filas de la
        // tabla puente tutorial_tags, tambien en el lado inverso.
        for (var tag : new HashSet<>(tutorial.getTags())) {
            tutorial.removeTag(tag.getId());
        }

        tutorialRepository.delete(tutorial);
    }

    @Override
    @Transactional
    public void deleteAll() {
        List<Tutorial> tutorials = tutorialRepository.findAll();

        for (Tutorial tutorial : tutorials) {
            for (var tag : new HashSet<>(tutorial.getTags())) {
                tutorial.removeTag(tag.getId());
            }
        }

        tutorialRepository.deleteAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tutorial> findByPublished(boolean published) {
        return tutorialRepository.findByPublished(published);
    }
}
