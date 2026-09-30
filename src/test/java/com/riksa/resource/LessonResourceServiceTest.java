package com.riksa.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.gamification.service.XpLedgerService;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.resource.dto.CreateResourceRequest;
import com.riksa.resource.dto.ResourceResponse;
import com.riksa.resource.entity.LessonResource;
import com.riksa.resource.entity.ResourceKind;
import com.riksa.resource.repository.LessonResourceRepository;
import com.riksa.resource.service.LessonResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonResourceServiceTest {
    private static final UUID AUTHOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SAVER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID LESSON_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID RESOURCE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID REWARD_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

    @Mock private LessonResourceRepository resourceRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private UserRepository userRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private XpLedgerService xpLedgerService;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private LessonResourceService service;

    private User author;

    @BeforeEach
    void setUp() {
        author = user(AUTHOR_ID, Role.USER);
        lenient().doAnswer(invocation -> {
            Class<?> resultType = invocation.getArgument(1);
            if (resultType == UUID.class) return AUTHOR_ID;
            if (resultType == Long.class) return 0L;
            if (resultType == Boolean.class) return false;
            return null;
        }).when(jdbcTemplate).queryForObject(anyString(), any(Class.class), any(Object[].class));
    }

    @Test
    void createValidResourceReturnsMappedLessonResource() {
        when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);
        when(resourceRepository.countByAuthorIdAndCreatedAtGreaterThanEqual(eq(AUTHOR_ID), any()))
                .thenReturn(0L);
        when(resourceRepository.saveAndFlush(any(LessonResource.class))).thenAnswer(invocation -> {
            LessonResource resource = invocation.getArgument(0);
            resource.setId(RESOURCE_ID);
            resource.setCreatedAt(OffsetDateTime.parse("2026-09-24T10:00:00Z"));
            return resource;
        });
        when(userRepository.findAllById(any())).thenReturn(List.of(author));

        ResourceResponse response = service.create(AUTHOR_ID, LESSON_ID, request("Past paper", "https://example.edu/paper.pdf"));

        assertEquals(RESOURCE_ID, response.id());
        assertEquals(LESSON_ID, response.lessonId());
        assertEquals("Past paper", response.title());
        assertEquals("https://example.edu/paper.pdf", response.url());
        assertEquals(ResourceKind.PDF, response.kind());
        assertFalse(response.savedByMe());
        verify(resourceRepository).saveAndFlush(any(LessonResource.class));
    }

    @Test
    void createRejectsInvalidUrlTitleLessonAndDailyQuota() {
        when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> service.create(AUTHOR_ID, LESSON_ID, request("Good title", "javascript:alert(1)")));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(AUTHOR_ID, LESSON_ID, request("No", "https://example.edu")));
        verify(resourceRepository, never()).saveAndFlush(any());

        when(lessonRepository.existsById(LESSON_ID)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class,
                () -> service.create(AUTHOR_ID, LESSON_ID, request("Good title", "https://example.edu")));

        when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);
        when(resourceRepository.countByAuthorIdAndCreatedAtGreaterThanEqual(eq(AUTHOR_ID), any()))
                .thenReturn(10L);
        ResponseStatusException limited = assertThrows(ResponseStatusException.class,
                () -> service.create(AUTHOR_ID, LESSON_ID, request("Good title", "https://example.edu")));
        assertEquals(429, limited.getStatusCode().value());
        verify(resourceRepository, never()).saveAndFlush(any());
    }

    @Test
    void publicListMapsGuestSavedStateAsFalse() {
        LessonResource resource = resource(RESOURCE_ID, AUTHOR_ID);
        when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);
        when(resourceRepository.findFirstPage(eq(LESSON_ID), any())).thenReturn(List.of(resource));
        when(userRepository.findAllById(any())).thenReturn(List.of(author));

        var page = service.list(LESSON_ID, null, null, 20);

        assertEquals(1, page.getEdges().size());
        assertFalse(page.getEdges().getFirst().savedByMe());
        verify(jdbcTemplate).queryForObject(contains("COUNT(*)"), eq(Long.class), eq(RESOURCE_ID));
    }

    @Test
    void saveAndResaveAwardsAuthorOnlyOnceForEachSaver() {
        LessonResource resource = resource(RESOURCE_ID, AUTHOR_ID);
        when(resourceRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(userRepository.findAllById(any())).thenReturn(List.of(author));
        when(jdbcTemplate.update(contains("INSERT INTO lesson_resource_save"), eq(RESOURCE_ID), eq(SAVER_ID))).thenReturn(1, 1);
        when(jdbcTemplate.update(startsWith("DELETE FROM lesson_resource_save"), eq(RESOURCE_ID), eq(SAVER_ID))).thenReturn(1);
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), any(Object[].class)))
                .thenReturn(REWARD_ID, null);

        service.save(SAVER_ID, RESOURCE_ID);
        service.unsave(SAVER_ID, RESOURCE_ID);
        service.save(SAVER_ID, RESOURCE_ID);

        verify(xpLedgerService, times(1)).awardXp(AUTHOR_ID, 5, "RESOURCE_SAVED", REWARD_ID);
        verify(jdbcTemplate, times(2)).update(argThat((String statement) -> statement.contains("INSERT INTO lesson_resource_save")
                && statement.contains("ON CONFLICT (resource_id, user_id) DO NOTHING")),
                eq(RESOURCE_ID), eq(SAVER_ID));
    }

    @Test
    void onlyAuthorOrAdminCanDelete() {
        LessonResource resource = resource(RESOURCE_ID, AUTHOR_ID);
        when(resourceRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(userRepository.findById(SAVER_ID)).thenReturn(Optional.of(user(SAVER_ID, Role.USER)));
        assertThrows(ResponseStatusException.class, () -> service.delete(SAVER_ID, RESOURCE_ID));
        verify(resourceRepository, never()).delete(any(LessonResource.class));

        User admin = user(SAVER_ID, Role.ADMIN);
        when(userRepository.findById(SAVER_ID)).thenReturn(Optional.of(admin));
        service.delete(SAVER_ID, RESOURCE_ID);
        verify(resourceRepository).delete(resource);
    }

    private static CreateResourceRequest request(String title, String url) {
        CreateResourceRequest request = new CreateResourceRequest();
        request.setTitle(title);
        request.setUrl(url);
        request.setKind(ResourceKind.PDF);
        return request;
    }

    private static LessonResource resource(UUID id, UUID authorId) {
        LessonResource resource = new LessonResource();
        resource.setId(id);
        resource.setLessonId(LESSON_ID);
        resource.setAuthorId(authorId);
        resource.setTitle("Paper");
        resource.setUrl("https://example.edu/paper.pdf");
        resource.setKind(ResourceKind.PDF);
        resource.setCreatedAt(OffsetDateTime.parse("2026-09-24T10:00:00Z"));
        return resource;
    }

    private static User user(UUID id, Role role) {
        User user = new User();
        user.setId(id);
        user.setDisplayName("Dara");
        user.setRole(role);
        return user;
    }
}
