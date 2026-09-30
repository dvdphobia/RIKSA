package com.riksa.moderation;

import com.riksa.common.exception.GlobalExceptionHandler;
import com.riksa.common.exception.GraphQlExceptionResolver;
import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import graphql.schema.GraphQLObjectType;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationExceptionMappingTest {

    private final GlobalExceptionHandler restHandler = new GlobalExceptionHandler();
    private final ExposedGraphQlExceptionResolver graphQlResolver = new ExposedGraphQlExceptionResolver();

    @Test
    void restContentBlockedIncludesExistingFieldsCodeAndReasonCodes() {
        ContentBlockedException exception = new ContentBlockedException(List.of(
                new ModerationReason(ModerationReasonCode.PROFANITY, "A configured term matched."),
                new ModerationReason(ModerationReasonCode.UNKNOWN_LINK, "A host is not allowed.")));

        var response = restHandler.handleContentBlocked(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).containsEntry("status", 422)
                .containsEntry("error", "Unprocessable Entity")
                .containsEntry("message", "Content was blocked by moderation.")
                .containsEntry("code", "CONTENT_BLOCKED")
                .containsEntry("reasons", List.of("PROFANITY", "UNKNOWN_LINK"));
        assertThat(response.getBody()).containsKeys("timestamp");
    }

    @Test
    void restRateLimitIncludesCodeAnd429Status() {
        var response = restHandler.handleModerationRateLimit(new RateLimitedException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).containsEntry("status", 429)
                .containsEntry("code", "RATE_LIMITED");
    }

    @Test
    void graphQlContentBlockedMapsToBadRequestWithExtensions() {
        ContentBlockedException exception = new ContentBlockedException(List.of(
                new ModerationReason(ModerationReasonCode.PROFANITY, "A configured term matched.")));

        GraphQLError error = graphQlResolver.resolve(exception, env());

        assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(error.getExtensions()).containsEntry("code", "CONTENT_BLOCKED")
                .containsEntry("reasons", List.of("PROFANITY"));
        assertThat(error.getMessage()).isEqualTo("Content was blocked by moderation.");
    }

    @Test
    void graphQlRateLimitHasStableCodeExtension() {
        GraphQLError error = graphQlResolver.resolve(new RateLimitedException(), env());

        assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(error.getExtensions()).containsEntry("code", "RATE_LIMITED");
    }

    private DataFetchingEnvironment env() {
        ExecutionStepInfo stepInfo = ExecutionStepInfo.newExecutionStepInfo()
                .type(GraphQLObjectType.newObject().name("Moderation").build())
                .path(ResultPath.rootPath().segment("publish"))
                .build();
        return DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .executionStepInfo(stepInfo)
                .mergedField(MergedField.newMergedField(Field.newField("publish").build()).build())
                .build();
    }

    private static class ExposedGraphQlExceptionResolver extends GraphQlExceptionResolver {
        GraphQLError resolve(Throwable exception, DataFetchingEnvironment environment) {
            return resolveToSingleError(exception, environment);
        }
    }
}
