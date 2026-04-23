package com.workouthub.exercises.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    boolean existsByNameEnIgnoreCase(String nameEn);

    Page<Exercise> findByCategory(Category category, Pageable pageable);

    Page<Exercise> findByEquipment(Equipment equipment, Pageable pageable);

    Page<Exercise> findByDifficulty(Difficulty difficulty, Pageable pageable);

    @Query("""
            SELECT e FROM Exercise e
            WHERE (:category  IS NULL OR e.category   = :category)
              AND (:equipment IS NULL OR e.equipment  = :equipment)
              AND (:difficulty IS NULL OR e.difficulty = :difficulty)
            """)
    Page<Exercise> search(
            @Param("category") Category category,
            @Param("equipment") Equipment equipment,
            @Param("difficulty") Difficulty difficulty,
            Pageable pageable);

    @Query("""
            SELECT e FROM Exercise e
            WHERE LOWER(e.nameTr) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(e.nameEn) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<Exercise> searchByName(@Param("q") String q, Pageable pageable);
}
