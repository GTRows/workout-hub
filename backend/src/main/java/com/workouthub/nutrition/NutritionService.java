package com.workouthub.nutrition;

import com.workouthub.common.web.NotFoundException;
import com.workouthub.nutrition.domain.FoodItem;
import com.workouthub.nutrition.domain.FoodItemRepository;
import com.workouthub.nutrition.domain.NutritionEntry;
import com.workouthub.nutrition.domain.NutritionEntryRepository;
import com.workouthub.nutrition.dto.CreateNutritionEntryRequest;
import com.workouthub.nutrition.dto.FoodItemDto;
import com.workouthub.nutrition.dto.NutritionEntryDto;
import com.workouthub.nutrition.dto.UpdateNutritionEntryRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NutritionService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final FoodItemRepository foods;
    private final NutritionEntryRepository entries;

    public NutritionService(FoodItemRepository foods, NutritionEntryRepository entries) {
        this.foods = foods;
        this.entries = entries;
    }

    @Transactional(readOnly = true)
    public List<FoodItemDto> searchFoods(String q, int size) {
        String trimmed = q == null ? "" : q.trim();
        List<FoodItem> hits = trimmed.isEmpty()
                ? foods.findAll(PageRequest.of(0, Math.min(size, 100))).getContent()
                : foods.searchByName(trimmed, PageRequest.of(0, Math.min(size, 100)));
        return hits.stream().map(NutritionService::toFoodDto).toList();
    }

    @Transactional(readOnly = true)
    public List<NutritionEntryDto> listForUserBetween(UUID userId, Instant from, Instant to) {
        List<NutritionEntry> rows = entries
                .findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc(userId, from, to);
        if (rows.isEmpty()) return List.of();

        Map<UUID, FoodItem> foodIndex = foods
                .findAllById(rows.stream().map(NutritionEntry::getFoodId).distinct().toList())
                .stream().collect(Collectors.toMap(FoodItem::getId, f -> f));

        return rows.stream()
                .map(e -> toEntryDto(e, foodIndex.get(e.getFoodId())))
                .toList();
    }

    public NutritionEntryDto create(UUID userId, CreateNutritionEntryRequest req) {
        FoodItem food = foods.findById(req.foodId())
                .orElseThrow(() -> new NotFoundException("Food not found: " + req.foodId()));
        NutritionEntry e = new NutritionEntry();
        e.setUserId(userId);
        e.setFoodId(food.getId());
        e.setServingG(req.servingG());
        e.setConsumedAt(req.consumedAt());
        e.setNotes(req.notes());
        return toEntryDto(entries.save(e), food);
    }

    public NutritionEntryDto update(UUID userId, UUID id, UpdateNutritionEntryRequest req) {
        NutritionEntry e = entries.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Nutrition entry not found: " + id));
        if (req.foodId() != null) {
            foods.findById(req.foodId())
                    .orElseThrow(() -> new NotFoundException("Food not found: " + req.foodId()));
            e.setFoodId(req.foodId());
        }
        if (req.servingG() != null) e.setServingG(req.servingG());
        if (req.consumedAt() != null) e.setConsumedAt(req.consumedAt());
        if (req.notes() != null) e.setNotes(req.notes());
        FoodItem food = foods.findById(e.getFoodId()).orElseThrow();
        return toEntryDto(e, food);
    }

    public void delete(UUID userId, UUID id) {
        NutritionEntry e = entries.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Nutrition entry not found: " + id));
        entries.delete(e);
    }

    private static FoodItemDto toFoodDto(FoodItem f) {
        return new FoodItemDto(
                f.getId(), f.getNameTr(), f.getNameEn(),
                f.getKcalPer100g(), f.getProteinG(),
                f.getCarbsG(), f.getFatG(), f.getDefaultServingG());
    }

    private static NutritionEntryDto toEntryDto(NutritionEntry e, FoodItem food) {
        BigDecimal scaler = e.getServingG().divide(HUNDRED, 4, RoundingMode.HALF_UP);
        return new NutritionEntryDto(
                e.getId(),
                e.getFoodId(),
                food == null ? null : food.getNameTr(),
                food == null ? null : food.getNameEn(),
                e.getServingG(),
                food == null ? null : food.getKcalPer100g().multiply(scaler).setScale(1, RoundingMode.HALF_UP),
                food == null ? null : food.getProteinG().multiply(scaler).setScale(1, RoundingMode.HALF_UP),
                food == null ? null : food.getCarbsG().multiply(scaler).setScale(1, RoundingMode.HALF_UP),
                food == null ? null : food.getFatG().multiply(scaler).setScale(1, RoundingMode.HALF_UP),
                e.getConsumedAt(),
                e.getNotes());
    }
}
