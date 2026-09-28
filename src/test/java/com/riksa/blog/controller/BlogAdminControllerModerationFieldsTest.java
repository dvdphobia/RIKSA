package com.riksa.blog.controller;

import com.riksa.admin.controller.BlogAdminController;
import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.service.BlogModerationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BlogAdminControllerModerationFieldsTest {

    private static final UUID ADMIN_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private BlogModerationService moderationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        moderationService = mock(BlogModerationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new BlogAdminController(moderationService)).build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(ADMIN_ID.toString(), "test", List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminListIncludesModerationSignalsWithoutChangingExistingFields() throws Exception {
        BlogPost post = post(BlogStatus.PENDING);
        when(moderationService.list(BlogStatus.PENDING, 0, 20))
                .thenReturn(new PageImpl<>(List.of(post)));

        mockMvc.perform(get("/api/v1/admin/blog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(post.getId().toString()))
                .andExpect(jsonPath("$.content[0].title").value("A flagged post"))
                .andExpect(jsonPath("$.content[0].moderationScore").value(3))
                .andExpect(jsonPath("$.content[0].moderationReasons").value("DUPLICATE"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void adminDetailIncludesModerationSignals() throws Exception {
        BlogPost post = post(BlogStatus.PENDING);
        when(moderationService.getPost(post.getId())).thenReturn(post);

        mockMvc.perform(get("/api/v1/admin/blog/{id}", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(post.getId().toString()))
                .andExpect(jsonPath("$.title").value("A flagged post"))
                .andExpect(jsonPath("$.moderationScore").value(3))
                .andExpect(jsonPath("$.moderationReasons").value("DUPLICATE"));
    }

    @Test
    void adminModerationResponseIncludesModerationSignals() throws Exception {
        BlogPost post = post(BlogStatus.APPROVED);
        when(moderationService.approve(eq(post.getId()), eq(ADMIN_ID))).thenReturn(post);

        mockMvc.perform(patch("/api/v1/admin/blog/{id}/status", post.getId())
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(post.getId().toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.moderationScore").value(3))
                .andExpect(jsonPath("$.moderationReasons").value("DUPLICATE"));
    }

    private BlogPost post(BlogStatus status) {
        BlogPost post = new BlogPost();
        post.setId(UUID.randomUUID());
        post.setAuthorId(UUID.randomUUID());
        post.setTitle("A flagged post");
        post.setContent("A body held for review.");
        post.setStatus(status);
        post.setModerationScore(3);
        post.setModerationReasons("DUPLICATE");
        return post;
    }
}
