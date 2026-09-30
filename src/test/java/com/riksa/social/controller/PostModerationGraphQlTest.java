package com.riksa.social.controller;

import com.riksa.common.config.GraphQlConfig;
import com.riksa.common.exception.GraphQlExceptionResolver;
import com.riksa.moderation.ContentBlockedException;
import com.riksa.moderation.ModerationReason;
import com.riksa.moderation.ModerationReasonCode;
import com.riksa.social.repository.PostMediaRepository;
import com.riksa.social.service.PostService;
import com.riksa.social.service.ShareService;
import com.riksa.social.service.SocialResponseResolver;
import graphql.GraphQLError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.ExecutionGraphQlResponse;
import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerConfigurer;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.graphql.execution.DefaultBatchLoaderRegistry;
import org.springframework.graphql.execution.DefaultExecutionGraphQlService;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.support.DefaultExecutionGraphQlRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostModerationGraphQlTest {

    private final PostService postService = mock(PostService.class);
    private final UUID userId = UUID.randomUUID();
    private AnnotationConfigApplicationContext context;
    private DefaultExecutionGraphQlService graphQl;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.register(GraphQlConfig.class);
        context.registerBean(BatchLoaderRegistry.class, () -> new DefaultBatchLoaderRegistry());
        context.registerBean(PostController.class, () -> new PostController(
                postService, mock(ShareService.class), mock(SocialResponseResolver.class),
                mock(PostMediaRepository.class)));
        context.refresh();

        AnnotatedControllerConfigurer controllers = new AnnotatedControllerConfigurer();
        controllers.setApplicationContext(context);
        controllers.afterPropertiesSet();

        GraphQlSource source = GraphQlSource.schemaResourceBuilder()
                .schemaResources(new ClassPathResource("graphql/social.graphqls"))
                .configureRuntimeWiring(context.getBean(RuntimeWiringConfigurer.class))
                .configureRuntimeWiring(controllers)
                .exceptionResolvers(List.of(new GraphQlExceptionResolver()))
                .build();
        graphQl = new DefaultExecutionGraphQlService(source);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void createPostBlockedByModerationExposesContentBlockedExtensionCode() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userId.toString(), null, AuthorityUtils.createAuthorityList("ROLE_USER")));
        UUID categoryId = UUID.randomUUID();
        when(postService.createPost(eq(userId), anyString(), anyString(), any(), any(), eq(categoryId)))
                .thenThrow(new ContentBlockedException(List.of(
                        new ModerationReason(ModerationReasonCode.PROFANITY, "A configured term matched."))));

        ExecutionGraphQlResponse response = graphQl.execute(new DefaultExecutionGraphQlRequest(
                """
                        mutation CreatePost($input: CreatePostInput!) {
                          createPost(input: $input) { id }
                        }
                        """,
                null,
                Map.of("input", Map.of("title", "Classroom", "content", "blocked text",
                        "categoryId", categoryId.toString())),
                Map.of(), UUID.randomUUID().toString(), null)).block();

        List<GraphQLError> errors = response.getExecutionResult().getErrors();
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().getExtensions()).containsEntry("code", "CONTENT_BLOCKED");
        verify(postService).createPost(eq(userId), anyString(), anyString(), any(), any(), eq(categoryId));
    }
}
