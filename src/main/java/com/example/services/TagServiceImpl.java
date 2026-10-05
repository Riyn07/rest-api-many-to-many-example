package com.example.services;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entities.Tag;
import com.example.entities.Tutorial;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.TagRepository;
import com.example.repository.TutorialRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TutorialRepository tutorialRepository;
    private final TagRepository tagRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Tag> findAll() {
        return tagRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> findByTutorialId(long tutorialId) {
        if (!tutorialRepository.existsById(tutorialId)) {
            throw new ResourceNotFoundException("Not found Tutorial with id = " + tutorialId);
        }

        return tagRepository.findTagsByTutorialsId(tutorialId);
    }

    @Override
    @Transactional(readOnly = true)
    public Tag findById(long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Tag with id = " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tutorial> findTutorialsByTagId(long tagId) {
        if (!tagRepository.existsById(tagId)) {
            throw new ResourceNotFoundException("Not found Tag with id = " + tagId);
        }

        return tutorialRepository.findTutorialsByTagsId(tagId);
    }

    @Override
    @Transactional
    public Tag addTagToTutorial(long tutorialId, Tag tagRequest) {
        Tutorial tutorial = tutorialRepository.findById(tutorialId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Tutorial with id = " + tutorialId));

        Tag tag;

        if (tagRequest.getId() != 0L) {
            // El tag ya existe: se reutiliza el de la base de datos.
            tag = tagRepository.findById(tagRequest.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Not found Tag with id = " + tagRequest.getId()));
        } else {
            tag = tagRepository.save(Tag.builder().name(tagRequest.getName()).build());
        }

        tutorial.addTag(tag);
        // Sin esta llamada la fila de la tabla puente tutorial_tags no se
        // escribia al crear un tag nuevo.
        tutorialRepository.save(tutorial);

        return tag;
    }

    @Override
    @Transactional
    public Tag update(long id, Tag tagRequest) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TagId " + id + "not found"));

        tag.setName(tagRequest.getName());

        return tagRepository.save(tag);
    }

    @Override
    @Transactional
    public void removeTagFromTutorial(long tutorialId, long tagId) {
        Tutorial tutorial = tutorialRepository.findById(tutorialId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Tutorial with id = " + tutorialId));

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Tag with id = " + tagId));

        if (!tutorial.getTags().contains(tag)) {
            throw new ResourceNotFoundException("El Tag with id = " + tagId
                    + " no esta asociado al Tutorial with id = " + tutorialId);
        }

        tutorial.removeTag(tagId);
        tutorialRepository.save(tutorial);
    }

    @Override
    @Transactional
    public void delete(long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Tag with id = " + id));

        // El lado inverso de un @ManyToMany no borra por si solo las filas de
        // la tabla puente, asi que hay que desacoplar el tag de cada tutorial.
        for (Tutorial tutorial : new HashSet<>(tag.getTutorials())) {
            tutorial.removeTag(tag.getId());
            tutorialRepository.save(tutorial);
        }

        tagRepository.delete(tag);
    }
}