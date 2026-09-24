package com.riksa.resource.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.gamification.dto.CursorPage;
import com.riksa.gamification.service.XpLedgerService;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.resource.dto.CreateResourceRequest;
import com.riksa.resource.dto.ResourceAuthorResponse;
import com.riksa.resource.dto.ResourceResponse;
import com.riksa.resource.entity.LessonResource;
import com.riksa.resource.repository.LessonResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@Service
@RequiredArgsConstructor
public class LessonResourceService {
    private static final ZoneId CAMBODIA = ZoneId.of("Asia/Phnom_Penh");
    private static final int MAX_PER_DAY = 10;

    private final LessonResourceRepository resourceRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final XpLedgerService xpLedgerService;
    private final ObjectMapper objectMapper;

    private record ResourceCursor(long saveCount, OffsetDateTime createdAt, UUID id) {}

    @Transactional(readOnly = true)
    public CursorPage<ResourceResponse> list(UUID lessonId, UUID viewerId, String cursorText, int requestedLimit) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new ResourceNotFoundException("Lesson not found: " + lessonId);
        }
        int limit = Math.min(Math.max(requestedLimit, 1), 50);
        ResourceCursor cursor = decodeCursor(cursorText);
        List<LessonResource> rows = cursor == null
                ? resourceRepository.findFirstPage(lessonId, org.springframework.data.domain.PageRequest.of(0, limit + 1))
                : resourceRepository.findAfterCursor(lessonId, cursor.saveCount(), cursor.createdAt(),
                        cursor.id(), org.springframework.data.domain.PageRequest.of(0, limit + 1));
        boolean hasNext = rows.size() > limit;
        List<LessonResource> page = rows.stream().limit(limit).toList();
        List<ResourceResponse> edges = map(page, viewerId);
        String endCursor = hasNext && !page.isEmpty()
                ? encodeCursor(new ResourceCursor(
                        saveCount(page.getLast().getId()),
                        page.getLast().getCreatedAt(),
                        page.getLast().getId()))
                : null;
        return new CursorPage<>(edges, hasNext, endCursor);
    }

    @Transactional
    public ResourceResponse create(UUID authorId, UUID lessonId, CreateResourceRequest request) {
        // Serializes a user's daily quota check with other concurrent creates.
        jdbcTemplate.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", UUID.class, authorId);
        if (!lessonRepository.existsById(lessonId)) {
            throw new ResourceNotFoundException("Lesson not found: " + lessonId);
        }
        String title = request.getTitle() == null ? "" : request.getTitle().trim();
        if (title.length() < 3 || title.length() > 120) {
            throw new IllegalArgumentException("Title must be between 3 and 120 characters");
        }
        URI parsed;
        try {
            parsed = URI.create(request.getUrl().trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("URL must be a valid http or https address", e);
        }
        if (!("http".equalsIgnoreCase(parsed.getScheme()) || "https".equalsIgnoreCase(parsed.getScheme()))
                || parsed.getHost() == null || parsed.getUserInfo() != null) {
            throw new IllegalArgumentException("URL must be a valid http or https address");
        }
        OffsetDateTime start = LocalDate.now(CAMBODIA).atStartOfDay(CAMBODIA).toOffsetDateTime();
        if (resourceRepository.countByAuthorIdAndCreatedAtGreaterThanEqual(authorId, start) >= MAX_PER_DAY) {
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "You can share up to 10 resources per day");
        }

        LessonResource resource = new LessonResource();
        resource.setLessonId(lessonId);
        resource.setAuthorId(authorId);
        resource.setTitle(title);
        resource.setUrl(parsed.toASCIIString());
        resource.setKind(request.getKind());
        return map(List.of(resourceRepository.saveAndFlush(resource)), authorId).getFirst();
    }

    @Transactional
    public ResourceResponse save(UUID userId, UUID resourceId) {
        LessonResource resource = getResource(resourceId);
        int inserted = jdbcTemplate.update("""
                INSERT INTO lesson_resource_save (id, resource_id, user_id, created_at)
                VALUES (uuid_generate_v4(), ?, ?, now())
                ON CONFLICT (resource_id, user_id) DO NOTHING
                """, resourceId, userId);
        if (inserted > 0 && !resource.getAuthorId().equals(userId)) {
            UUID rewardId = jdbcTemplate.query("""
                    INSERT INTO lesson_resource_save_reward (id, resource_id, saver_id, created_at)
                    VALUES (uuid_generate_v4(), ?, ?, now())
                    ON CONFLICT (resource_id, saver_id) DO NOTHING
                    RETURNING id
                    """, rs -> rs.next() ? rs.getObject("id", UUID.class) : null, resourceId, userId);
            if (rewardId != null) {
                xpLedgerService.awardXp(resource.getAuthorId(), 5, "RESOURCE_SAVED", rewardId);
            }
        }
        return map(List.of(resource), userId).getFirst();
    }

    @Transactional
    public ResourceResponse unsave(UUID userId, UUID resourceId) {
        LessonResource resource = getResource(resourceId);
        jdbcTemplate.update("DELETE FROM lesson_resource_save WHERE resource_id = ? AND user_id = ?",
                resourceId, userId);
        return map(List.of(resource), userId).getFirst();
    }

    @Transactional
    public void delete(UUID userId, UUID resourceId) {
        LessonResource resource = getResource(resourceId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (!resource.getAuthorId().equals(userId) && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(FORBIDDEN, "Only the author or an admin can delete this resource");
        }
        resourceRepository.delete(resource);
    }

    private LessonResource getResource(UUID resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));
    }

    private List<ResourceResponse> map(List<LessonResource> resources, UUID viewerId) {
        if (resources.isEmpty()) return List.of();
        Map<UUID, User> authors = userRepository.findAllById(
                resources.stream().map(LessonResource::getAuthorId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return resources.stream().map(resource -> {
            User author = authors.get(resource.getAuthorId());
            return new ResourceResponse(
                    resource.getId(),
                    resource.getLessonId(),
                    resource.getTitle(),
                    resource.getUrl(),
                    resource.getKind(),
                    author == null ? null : new ResourceAuthorResponse(
                            author.getId(), author.getDisplayName(), author.getAvatarUrl()),
                    saveCount(resource.getId()),
                    viewerId != null && isSaved(resource.getId(), viewerId),
                    resource.getCreatedAt());
        }).toList();
    }

    private long saveCount(UUID resourceId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lesson_resource_save WHERE resource_id = ?", Long.class, resourceId);
        return count == null ? 0 : count;
    }

    private boolean isSaved(UUID resourceId, UUID userId) {
        Boolean saved = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM lesson_resource_save WHERE resource_id = ? AND user_id = ?)",
                Boolean.class, resourceId, userId);
        return Boolean.TRUE.equals(saved);
    }

    private String encodeCursor(ResourceCursor cursor) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(cursor));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to encode resource cursor", e);
        }
    }

    private ResourceCursor decodeCursor(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return objectMapper.readValue(Base64.getUrlDecoder().decode(text), ResourceCursor.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid resource cursor", e);
        }
    }
}
