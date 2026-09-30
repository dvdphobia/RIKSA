package com.riksa.admin.service;

import com.riksa.admin.dto.RoadmapUnitRequest;
import com.riksa.admin.dto.RoadmapUnitReorderRequest;
import com.riksa.admin.dto.RoadmapUnitResponse;
import com.riksa.common.audit.AuditLogService;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.learning.entity.RoadmapUnit;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.learning.repository.RoadmapUnitRepository;
import com.riksa.social.entity.Category;
import com.riksa.social.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoadmapUnitAdminService {

    private final RoadmapUnitRepository roadmapUnitRepository;
    private final LessonRepository lessonRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<RoadmapUnitResponse> list(UUID categoryId) {
        List<RoadmapUnit> units = categoryId == null
                ? roadmapUnitRepository.findAllByOrderByCategory_IdAscPositionAsc()
                : roadmapUnitRepository.findByCategoryIdOrderByPositionAsc(categoryId);
        return units.stream().map(this::toResponse).toList();
    }

    @Transactional
    public RoadmapUnitResponse create(UUID actorId, RoadmapUnitRequest request) {
        Category category = resolveCategory(request.getCategoryId());
        RoadmapUnit unit = new RoadmapUnit();
        unit.setCategory(category);
        unit.setTitle(request.getTitle().trim());
        unit.setDescription(request.getDescription());
        unit.setPosition(nextPosition(category.getId()));
        unit = roadmapUnitRepository.save(unit);

        auditLogService.record(actorId, "ROADMAP_UNIT_CREATE", "ROADMAP_UNIT", unit.getId(),
                Map.of("categoryId", category.getId().toString(), "title", unit.getTitle(),
                        "position", unit.getPosition()));
        return toResponse(unit);
    }

    @Transactional
    public RoadmapUnitResponse update(UUID actorId, UUID id, RoadmapUnitRequest request) {
        RoadmapUnit unit = roadmapUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Roadmap unit not found: " + id));
        UUID previousCategoryId = unit.getCategory().getId();
        Category category = resolveCategory(request.getCategoryId());
        if (!previousCategoryId.equals(category.getId())) {
            throw new IllegalArgumentException("Roadmap units cannot be moved to another category");
        }
        unit.setTitle(request.getTitle().trim());
        unit.setDescription(request.getDescription());
        unit = roadmapUnitRepository.save(unit);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("previousCategoryId", previousCategoryId.toString());
        details.put("categoryId", category.getId().toString());
        details.put("title", unit.getTitle());
        details.put("position", unit.getPosition());
        auditLogService.record(actorId, "ROADMAP_UNIT_UPDATE", "ROADMAP_UNIT", id, details);
        return toResponse(unit);
    }

    @Transactional
    public void delete(UUID actorId, UUID id) {
        RoadmapUnit unit = roadmapUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Roadmap unit not found: " + id));
        long lessonCount = lessonRepository.countByRoadmapUnit_Id(id);
        UUID categoryId = unit.getCategory().getId();
        String title = unit.getTitle();
        lessonRepository.clearRoadmapUnit(id);
        roadmapUnitRepository.delete(unit);
        auditLogService.record(actorId, "ROADMAP_UNIT_DELETE", "ROADMAP_UNIT", id,
                Map.of("categoryId", categoryId.toString(),
                        "title", title, "detachedLessonCount", lessonCount));
    }

    @Transactional
    public List<RoadmapUnitResponse> reorder(UUID actorId, RoadmapUnitReorderRequest request) {
        UUID categoryId = request.getCategoryId();
        resolveCategory(categoryId);
        List<RoadmapUnit> existing = roadmapUnitRepository.findByCategoryIdOrderByPositionAsc(categoryId);
        List<UUID> ids = request.getIds();
        if (ids.size() != existing.size() || new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("Reorder ids must include each roadmap unit in the category exactly once");
        }

        Map<UUID, RoadmapUnit> byId = new LinkedHashMap<>();
        existing.forEach(unit -> byId.put(unit.getId(), unit));
        if (!byId.keySet().equals(new HashSet<>(ids))) {
            throw new IllegalArgumentException("Reorder ids must include each roadmap unit in the category exactly once");
        }

        int maxPosition = existing.stream().mapToInt(RoadmapUnit::getPosition).max().orElse(0);
        int offset = maxPosition + existing.size() + 1;
        for (int i = 0; i < existing.size(); i++) {
            existing.get(i).setPosition(offset + i);
        }
        roadmapUnitRepository.saveAll(existing);
        roadmapUnitRepository.flush();

        List<RoadmapUnit> reordered = new ArrayList<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            RoadmapUnit unit = byId.get(ids.get(i));
            unit.setPosition(i + 1);
            reordered.add(unit);
        }
        roadmapUnitRepository.saveAll(reordered);
        roadmapUnitRepository.flush();

        auditLogService.record(actorId, "ROADMAP_UNIT_REORDER", "CATEGORY", categoryId,
                Map.of("ids", List.copyOf(ids)));
        return reordered.stream().map(this::toResponse).toList();
    }

    private int nextPosition(UUID categoryId) {
        return roadmapUnitRepository.findFirstByCategoryIdOrderByPositionDesc(categoryId)
                .map(unit -> unit.getPosition() + 1)
                .orElse(1);
    }

    private Category resolveCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        if (!category.isOfficial()) {
            throw new IllegalArgumentException("Roadmap units can only use official categories");
        }
        return category;
    }

    private RoadmapUnitResponse toResponse(RoadmapUnit unit) {
        return new RoadmapUnitResponse(
                unit.getId(), unit.getCategory().getId(), unit.getTitle(), unit.getDescription(),
                unit.getPosition(), lessonRepository.countByRoadmapUnit_Id(unit.getId()), unit.getUpdatedAt());
    }
}
