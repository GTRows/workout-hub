package com.workouthub.nutrition.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodItemRepository extends JpaRepository<FoodItem, UUID> {

    @Query("""
            SELECT f FROM FoodItem f
            WHERE LOWER(f.nameTr) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(f.nameEn) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY f.nameTr ASC
            """)
    List<FoodItem> searchByName(@Param("q") String q, Pageable pageable);
}
