package com.riksa.blog.controller;

import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.service.BlogEngagementService;
import com.riksa.blog.service.BlogService;
import com.riksa.buddy.service.BuddyService;
import com.riksa.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class BlogControllerBuddyCareTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private BlogEngagementService engagementService;
    private BuddyService buddyService;
    private BlogService blogService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        blogService = mock(BlogService.class);
        engagementService = mock(BlogEngagementService.class);
        buddyService = mock(BuddyService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new BlogController(blogService, engagementService, buddyService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(USER_ID.toString(), "test", List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void completedReadAwardsCareWithoutChangingBlogViewsAndIsIdempotentAtTheService() throws Exception {
        UUID blogId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setStatus(BlogStatus.APPROVED);
        post.setViewCount(42);
        when(blogService.getPost(blogId)).thenReturn(post);

        mockMvc.perform(post("/api/v1/blog/{id}/read-complete", blogId))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/blog/{id}/read-complete", blogId))
                .andExpect(status().isNoContent());

        verify(buddyService, times(2)).recordCare(USER_ID, "BLOG_READ", blogId);
        verify(engagementService, never()).trackView(any(), any());
        org.assertj.core.api.Assertions.assertThat(post.getViewCount()).isEqualTo(42);
    }

    @Test
    void ownerPutReturnsTheExistingBlogPostResponse() throws Exception {
        UUID postId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setAuthorId(USER_ID);
        post.setTitle("Updated question");
        post.setContent("A useful answerable question.");
        post.setStatus(BlogStatus.APPROVED);
        post.setModerationScore(3);
        post.setModerationReasons("DUPLICATE");
        when(blogService.updatePost(eq(USER_ID), eq(postId), eq("Updated question"),
                eq("A useful answerable question."), eq("https://example.test/image.png"), eq(categoryId)))
                .thenReturn(post);

        mockMvc.perform(put("/api/v1/blog/{id}", postId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"Updated question","content":"A useful answerable question.",
                                 "coverUrl":"https://example.test/image.png","categoryId":"%s"}
                                """.formatted(categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.moderationScore").doesNotExist())
                .andExpect(jsonPath("$.moderationReasons").doesNotExist());
    }

    @Test
    void publicListAndDetailOmitModerationSignals() throws Exception {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setAuthorId(USER_ID);
        post.setTitle("Public post");
        post.setContent("A clean question.");
        post.setStatus(BlogStatus.APPROVED);
        post.setModerationScore(4);
        post.setModerationReasons("DUPLICATE,UNKNOWN_LINK");
        when(blogService.listPosts(BlogStatus.APPROVED, null, 0, 20))
                .thenReturn(new PageImpl<>(List.of(post)));
        when(blogService.getPost(postId)).thenReturn(post);
        when(engagementService.getPostForDetail(postId, USER_ID, false)).thenReturn(post);

        mockMvc.perform(get("/api/v1/blog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(postId.toString()))
                .andExpect(jsonPath("$.content[0].moderationScore").doesNotExist())
                .andExpect(jsonPath("$.content[0].moderationReasons").doesNotExist());

        mockMvc.perform(get("/api/v1/blog/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId.toString()))
                .andExpect(jsonPath("$.moderationScore").doesNotExist())
                .andExpect(jsonPath("$.moderationReasons").doesNotExist());
        verify(engagementService).getPostForDetail(postId, USER_ID, false);
    }

    @Test
    void hiddenPostIsNotReturnedToOrdinaryUser() throws Exception {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.HIDDEN);
        when(engagementService.getPostForDetail(postId, USER_ID, false))
                .thenThrow(new com.riksa.common.exception.ResourceNotFoundException(
                        "Blog post not found: " + postId));
        when(blogService.getPost(postId)).thenReturn(post);

        mockMvc.perform(get("/api/v1/blog/{id}", postId))
                .andExpect(status().isNotFound());

        verify(engagementService, never()).trackView(any(), any());
    }

    @Test
    void nonOwnerPutIsForbidden() throws Exception {
        UUID postId = UUID.randomUUID();
        when(blogService.updatePost(eq(USER_ID), eq(postId), anyString(), anyString(), any(), any()))
                .thenThrow(new AccessDeniedException("Only the author can edit or delete this blog post"));

        mockMvc.perform(put("/api/v1/blog/{id}", postId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"Updated question","content":"A useful question.",
                                 "coverUrl":null,"categoryId":"%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerDeleteReturnsNoContent() throws Exception {
        UUID postId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/blog/{id}", postId))
                .andExpect(status().isNoContent());

        verify(blogService).deletePost(USER_ID, postId);
    }

    @Test
    void nonOwnerDeleteIsForbidden() throws Exception {
        UUID postId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new AccessDeniedException("Only the author can edit or delete this blog post"))
                .when(blogService).deletePost(USER_ID, postId);

        mockMvc.perform(delete("/api/v1/blog/{id}", postId))
                .andExpect(status().isForbidden());
    }
}
