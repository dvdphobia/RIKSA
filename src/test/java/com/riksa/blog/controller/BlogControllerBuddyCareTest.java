package com.riksa.blog.controller;

import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.service.BlogEngagementService;
import com.riksa.blog.service.BlogService;
import com.riksa.buddy.service.BuddyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                new BlogController(blogService, engagementService, buddyService)).build();
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
}
