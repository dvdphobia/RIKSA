package com.riksa.common.exception;

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
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the fix for community posts failing with an opaque
 * {@code INTERNAL_ERROR}: deliberate refusals must reach the client with their
 * reason intact.
 */
class GraphQlExceptionResolverTest {

    private final GraphQlExceptionResolver resolver = new GraphQlExceptionResolver();

    private DataFetchingEnvironment env() {
        ExecutionStepInfo stepInfo = ExecutionStepInfo.newExecutionStepInfo()
                .type(GraphQLObjectType.newObject().name("Post").build())
                .path(ResultPath.rootPath().segment("createPost"))
                .build();
        return DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .executionStepInfo(stepInfo)
                .mergedField(MergedField.newMergedField(
                        Field.newField("createPost").build()).build())
                .build();
    }

    private GraphQLError resolve(Throwable ex) {
        return resolver.resolveToSingleError(ex, env());
    }

    @Test
    void membershipRefusalKeepsItsMessage() {
        GraphQLError error = resolve(
                new IllegalStateException("You must join this category before posting"));

        assertThat(error).isNotNull();
        assertThat(error.getMessage()).isEqualTo("You must join this category before posting");
        assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @Test
    void validationFailureKeepsItsMessage() {
        GraphQLError error = resolve(new IllegalArgumentException("Post not found: abc"));

        assertThat(error).isNotNull();
        assertThat(error.getMessage()).isEqualTo("Post not found: abc");
        assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @Test
    void notFoundMapsToNotFound() {
        GraphQLError error = resolve(new ResourceNotFoundException("No such post"));

        assertThat(error).isNotNull();
        assertThat(error.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
    }

    @Test
    void accessDeniedMapsToForbidden() {
        GraphQLError error = resolve(new AccessDeniedException("Denied"));

        assertThat(error).isNotNull();
        assertThat(error.getErrorType()).isEqualTo(ErrorType.FORBIDDEN);
    }

    @Test
    void unexpectedExceptionStaysOpaque() {
        // Returning null keeps the default behaviour: log server-side, tell the
        // client nothing about the internals.
        assertThat(resolve(new RuntimeException("boom"))).isNull();
    }
}
