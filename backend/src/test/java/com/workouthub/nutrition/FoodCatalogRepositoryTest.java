package com.workouthub.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.workouthub.nutrition.domain.FoodItem;
import com.workouthub.nutrition.domain.FoodItemRepository;
import com.workouthub.nutrition.domain.NutritionEntry;
import com.workouthub.nutrition.domain.NutritionEntryRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class FoodCatalogRepositoryTest extends AbstractIntegrationTest {

    private static final String SECRET = "NutritionSecret1!";

    @Autowired FoodItemRepository foods;
    @Autowired NutritionEntryRepository entries;
    @Autowired TestAuthHelpers helpers;

    @Test
    void seedContainsAtLeast50FoodItems() {
        assertThat(foods.count()).isGreaterThanOrEqualTo(50L);
    }

    @Test
    void searchByNameMatchesTurkishAndEnglish() {
        List<FoodItem> tr = foods.searchByName("muz", PageRequest.of(0, 10));
        assertThat(tr).anyMatch(f -> f.getNameEn().equalsIgnoreCase("Banana"));

        List<FoodItem> en = foods.searchByName("rice", PageRequest.of(0, 10));
        assertThat(en).anyMatch(f -> f.getNameEn().toLowerCase().contains("rice"));
    }

    @Test
    void findByUserBetweenWindowsCorrectly() {
        SeededUser u = helpers.seed(
                "nu-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        FoodItem anyFood = foods.findAll().get(0);

        Instant now = Instant.now();
        NutritionEntry today = new NutritionEntry();
        today.setUserId(u.id());
        today.setFoodId(anyFood.getId());
        today.setServingG(new BigDecimal("150"));
        today.setConsumedAt(now);
        entries.save(today);

        NutritionEntry weekAgo = new NutritionEntry();
        weekAgo.setUserId(u.id());
        weekAgo.setFoodId(anyFood.getId());
        weekAgo.setServingG(new BigDecimal("120"));
        weekAgo.setConsumedAt(now.minus(8, ChronoUnit.DAYS));
        entries.save(weekAgo);

        List<NutritionEntry> today24h = entries
                .findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc(
                        u.id(),
                        now.minus(1, ChronoUnit.DAYS),
                        now.plus(1, ChronoUnit.DAYS));
        assertThat(today24h).hasSize(1);
        assertThat(today24h.get(0).getServingG())
                .isEqualByComparingTo("150");
    }
}
