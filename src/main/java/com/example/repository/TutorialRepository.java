package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.entities.Tutorial;
import java.util.List;


public interface TutorialRepository extends JpaRepository<Tutorial, Long> {

    List<Tutorial> findByPublished(boolean published);

    /**
     * Busqueda por fragmento del titulo, insensible a mayusculas.
     *
     * Se escribe como @Query con LOWER en lugar de como query derivada
     * "Containing" porque el comportamiento de LIKE depende de la collation
     * de la base de datos: en MySQL la collation por defecto lo hace
     * insensible, en H2 sensible. Con LOWER el resultado es el mismo en ambas.
     */
    @Query("SELECT t FROM Tutorial t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Tutorial> findByTitleContaining(@Param("title") String title);

    List<Tutorial> findTutorialsByTagsId(Long tagId);
}
