package com.example.services;

import java.util.List;

import com.example.entities.Tag;
import com.example.entities.Tutorial;

public interface TagService {

    List<Tag> findAll();

    /** Tags asociados a un tutorial. */
    List<Tag> findByTutorialId(long tutorialId);

    Tag findById(long id);

    /** Tutorials que llevan asociado un tag. */
    List<Tutorial> findTutorialsByTagId(long tagId);

    /**
     * Asocia un tag a un tutorial. Si el tag recibido trae id distinto de 0 se
     * reutiliza el existente; si no, se crea uno nuevo.
     */
    Tag addTagToTutorial(long tutorialId, Tag tagRequest);

    Tag update(long id, Tag tagRequest);

    /** Desasocia un tag de un tutorial. */
    void removeTagFromTutorial(long tutorialId, long tagId);

    void delete(long id);
}
