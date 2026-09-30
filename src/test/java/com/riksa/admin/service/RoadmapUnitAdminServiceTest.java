package com.riksa.admin.service;

import com.riksa.admin.dto.RoadmapUnitReorderRequest;
import com.riksa.admin.dto.RoadmapUnitResponse;
import com.riksa.common.audit.AuditLogService;
import com.riksa.learning.entity.RoadmapUnit;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.learning.repository.RoadmapUnitRepository;
import com.riksa.social.entity.Category;
import com.riksa.social.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoadmapUnitAdminServiceTest {

    @Mock private RoadmapUnitRepository roadmapUnitRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private RoadmapUnitAdminService service;

    @Test
    void reorder_changesPositionsAndReturnsTheRequestedOrder() {
        UUID actorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = category(categoryId);
        RoadmapUnit first = unit(UUID.randomUUID(), category, 1, "First");
        RoadmapUnit second = unit(UUID.randomUUID(), category, 2, "Second");
        RoadmapUnitReorderRequest request = new RoadmapUnitReorderRequest();
        request.setCategoryId(categoryId);
        request.setIds(List.of(second.getId(), first.getId()));

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(roadmapUnitRepository.findByCategoryIdOrderByPositionAsc(categoryId))
                .thenReturn(List.of(first, second));
        when(lessonRepository.countByRoadmapUnit_Id(any())).thenReturn(0L);
        when(roadmapUnitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<RoadmapUnitResponse> result = service.reorder(actorId, request);

        assertThat(result).extracting(RoadmapUnitResponse::id)
                .containsExactly(second.getId(), first.getId());
        assertThat(second.getPosition()).isEqualTo(1);
        assertThat(first.getPosition()).isEqualTo(2);
        verify(roadmapUnitRepository, times(2)).flush();
        verify(auditLogService).record(eq(actorId), eq("ROADMAP_UNIT_REORDER"), eq("CATEGORY"),
                eq(categoryId), eq(Map.of("ids", request.getIds())));
    }

    @Test
    void deleteDetachesLessonsBeforeRemovingTheUnitAndAuditsTheCount() {
        UUID actorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        RoadmapUnit unit = unit(UUID.randomUUID(), category(categoryId), 1, "Unit A");
        when(roadmapUnitRepository.findById(unit.getId())).thenReturn(Optional.of(unit));
        when(lessonRepository.countByRoadmapUnit_Id(unit.getId())).thenReturn(3L);
        when(lessonRepository.clearRoadmapUnit(unit.getId())).thenReturn(3);

        service.delete(actorId, unit.getId());

        verify(lessonRepository).clearRoadmapUnit(unit.getId());
        verify(roadmapUnitRepository).delete(unit);
        verify(auditLogService).record(eq(actorId), eq("ROADMAP_UNIT_DELETE"), eq("ROADMAP_UNIT"),
                eq(unit.getId()), eq(Map.of(
                        "categoryId", categoryId.toString(),
                        "title", "Unit A",
                        "detachedLessonCount", 3L)));
    }

    private Category category(UUID id) {
        Category category = new Category();
        category.setId(id);
        category.setOfficial(true);
        return category;
    }

    private RoadmapUnit unit(UUID id, Category category, int position, String title) {
        RoadmapUnit unit = new RoadmapUnit();
        unit.setId(id);
        unit.setCategory(category);
        unit.setPosition(position);
        unit.setTitle(title);
        return unit;
    }
}
