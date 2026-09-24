package com.riksa.social.controller;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.security.RateLimitDecision;
import com.riksa.auth.security.RedisRateLimiter;
import com.riksa.common.config.GraphQlConfig;
import com.riksa.common.exception.GraphQlExceptionResolver;
import com.riksa.social.entity.Category;
import com.riksa.social.repository.CategoryRepository;
import com.riksa.social.repository.UserCategoryMembershipRepository;
import com.riksa.social.service.CategoryService;
import com.riksa.social.service.CommunityCreationService;
import com.riksa.social.service.MediaUrlValidator;
import graphql.GraphQLError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.ExecutionGraphQlResponse;
import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerConfigurer;
import org.springframework.graphql.execution.DefaultExecutionGraphQlService;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.support.DefaultExecutionGraphQlRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs {@code createCommunity} through the real schema, controller, scalars and
 * error resolver, with only persistence and Redis mocked. The mutation text is
 * the one the Flutter app sends, so a drift between the two fails here.
 */
class CreateCommunityGraphQlTest {

    /** Copied from {@code SocialApi.createCommunity} in RIKSA-FLUTTER. */
    private static final String APP_MUTATION = """
            mutation CreateCommunity($input: CreateCommunityInput!) {
              createCommunity(input: $input) {
                id name slug description iconUrl memberCount joined
              }
            }
            """;

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final UserCategoryMembershipRepository membershipRepository =
            mock(UserCategoryMembershipRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RedisRateLimiter rateLimiter = mock(RedisRateLimiter.class);
    private final CategoryService categoryService = mock(CategoryService.class);

    private final UUID userId = UUID.randomUUID();
    private AnnotationConfigApplicationContext context;
    private DefaultExecutionGraphQlService graphQl;

    @BeforeEach
    void setUp() {
        CommunityCreationService creation = new CommunityCreationService(
                categoryRepository, membershipRepository, userRepository,
                new MediaUrlValidator(), rateLimiter);

        context = new AnnotationConfigApplicationContext();
        context.register(GraphQlConfig.class);
        context.registerBean(CategoryController.class,
                () -> new CategoryController(categoryService, creation));
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

        User user = new User();
        user.setId(userId);
        user.setEmailVerified(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(rateLimiter.acquire(anyString(), anyString(), anyLong(), any(Duration.class)))
                .thenReturn(new RateLimitDecision(true, 3, 2, 0));
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    private void signIn() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId.toString(), null, AuthorityUtils.createAuthorityList("ROLE_USER")));
    }

    private ExecutionGraphQlResponse run(String document, Map<String, Object> variables) {
        return graphQl.execute(new DefaultExecutionGraphQlRequest(
                document, null, variables, Map.of(), UUID.randomUUID().toString(), null)).block();
    }

    private ExecutionGraphQlResponse create(Map<String, Object> input) {
        return run(APP_MUTATION, Map.of("input", input));
    }

    private static String firstError(ExecutionGraphQlResponse response) {
        List<GraphQLError> errors = response.getExecutionResult().getErrors();
        assertThat(errors).isNotEmpty();
        return errors.getFirst().getMessage();
    }

    private static String firstErrorType(ExecutionGraphQlResponse response) {
        return String.valueOf(response.getExecutionResult().getErrors().getFirst().getErrorType());
    }

    @Test
    void appMutation_signedIn_createsCommunity() {
        signIn();

        ExecutionGraphQlResponse response = create(Map.of(
                "name", "Robotics Club",
                "description", "Build robots together",
                "iconUrl", "https://cdn.example.com/robot.png"));

        assertThat(response.getExecutionResult().getErrors()).isEmpty();
        Map<String, Object> created = response.field("createCommunity").getValue();
        assertThat(created)
                .containsEntry("name", "Robotics Club")
                .containsEntry("slug", "robotics-club")
                .containsEntry("description", "Build robots together")
                .containsEntry("iconUrl", "https://cdn.example.com/robot.png")
                .containsEntry("memberCount", 1)
                .containsEntry("joined", true);
        assertThat(created.get("id")).asString().matches("[0-9a-f-]{36}");
    }

    @Test
    void newFields_reportOwnershipAndOfficialFlag() {
        signIn();

        ExecutionGraphQlResponse response = run("""
                mutation { createCommunity(input: { name: "Chess Club" }) { official ownedByMe } }
                """, Map.of());

        assertThat(response.getExecutionResult().getErrors()).isEmpty();
        Map<String, Object> created = response.field("createCommunity").getValue();
        assertThat(created).containsEntry("official", false).containsEntry("ownedByMe", true);
    }

    @Test
    void signedOut_isForbidden() {
        ExecutionGraphQlResponse response = create(Map.of("name", "Robotics Club"));

        assertThat(firstErrorType(response)).isEqualTo("FORBIDDEN");
        assertThat(firstError(response)).isEqualTo("Sign in to create a community");
        verify(categoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void reservedName_isRejectedWithReason() {
        signIn();

        ExecutionGraphQlResponse response = create(Map.of("name", "RIKSA Support"));

        assertThat(firstErrorType(response)).isEqualTo("BAD_REQUEST");
        assertThat(firstError(response)).isEqualTo("This community name is reserved");
    }

    @Test
    void scriptInName_isRejectedWithReason() {
        signIn();

        ExecutionGraphQlResponse response = create(Map.of("name", "<img src=x onerror=alert(1)>"));

        assertThat(firstErrorType(response)).isEqualTo("BAD_REQUEST");
        assertThat(firstError(response)).startsWith("Community name can only use");
    }

    @Test
    void dailyLimit_isReportedToTheClient() {
        signIn();
        when(rateLimiter.acquire(anyString(), anyString(), anyLong(), any(Duration.class)))
                .thenReturn(new RateLimitDecision(false, 3, 0, 600));

        ExecutionGraphQlResponse response = create(Map.of("name", "Robotics Club"));

        assertThat(firstError(response))
                .isEqualTo("You can create at most 3 communities a day. Try again later.");
        verify(categoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void limiterOutage_failsClosedWithReadableMessage() {
        signIn();
        when(rateLimiter.acquire(anyString(), anyString(), anyLong(), any(Duration.class)))
                .thenThrow(new IllegalStateException("redis down"));

        ExecutionGraphQlResponse response = create(Map.of("name", "Robotics Club"));

        assertThat(firstError(response)).isEqualTo("Creating communities is temporarily unavailable");
        verify(categoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void missingName_failsSchemaValidation() {
        signIn();

        ExecutionGraphQlResponse response = create(Map.of("description", "No name"));

        assertThat(firstErrorType(response)).isEqualTo("ValidationError");
        verify(categoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void unknownInputField_isRejectedBySchema() {
        signIn();

        // Clients cannot smuggle in the fields the server owns.
        ExecutionGraphQlResponse response = create(Map.of(
                "name", "Robotics Club", "official", true, "slug", "admin"));

        assertThat(firstErrorType(response)).isEqualTo("ValidationError");
        verify(categoryRepository, never()).saveAndFlush(any());
    }
}
