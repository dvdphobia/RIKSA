package com.riksa.common.exception;

import com.riksa.moderation.ContentBlockedException;
import com.riksa.moderation.ModerationReason;
import com.riksa.moderation.RateLimitedException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Maps service-layer exceptions onto client-visible GraphQL errors.
 *
 * <p>{@link GlobalExceptionHandler} is a {@code @RestControllerAdvice}, so it
 * only ever sees REST requests. Without this resolver every GraphQL mutation
 * failure — including deliberate rejections such as "you must join this
 * category first" — reached the client as an opaque
 * {@code INTERNAL_ERROR for <uuid>} and the reason was lost.
 *
 * <p>Returning {@code null} for an unrecognised throwable keeps the default
 * behaviour: log it server-side and tell the client nothing.
 */
@Slf4j
@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        return switch (ex) {
            case ContentBlockedException e -> error(env, ErrorType.BAD_REQUEST,
                    "Content was blocked by moderation.", Map.of(
                            "code", "CONTENT_BLOCKED",
                            "reasons", reasonCodes(e.reasons())));
            case RateLimitedException e -> error(env, ErrorType.BAD_REQUEST,
                    "Please wait before posting again.", Map.of("code", "RATE_LIMITED"));
            case ResourceNotFoundException e -> error(env, ErrorType.NOT_FOUND, e.getMessage());
            case AccessDeniedException e -> error(env, ErrorType.FORBIDDEN, e.getMessage());
            case InvalidCursorException e -> error(env, ErrorType.BAD_REQUEST, e.getMessage());
            case RateLimitExceededException e -> error(env, ErrorType.BAD_REQUEST, e.getMessage());
            case ServiceUnavailableException e -> error(env, ErrorType.INTERNAL_ERROR, e.getMessage());
            // Author-write refusals and validation failures both land here.
            case IllegalStateException e -> error(env, ErrorType.BAD_REQUEST, e.getMessage());
            case IllegalArgumentException e -> error(env, ErrorType.BAD_REQUEST, e.getMessage());
            default -> {
                log.error("Unhandled GraphQL exception", ex);
                yield null;
            }
        };
    }

    private GraphQLError error(DataFetchingEnvironment env, ErrorType type, String message) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(type)
                .message(message)
                .build();
    }

    private GraphQLError error(DataFetchingEnvironment env, ErrorType type, String message,
                               Map<String, Object> extensions) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(type)
                .message(message)
                .extensions(extensions)
                .build();
    }

    private List<String> reasonCodes(List<ModerationReason> reasons) {
        return reasons.stream().map(ModerationReason::code).map(Enum::name).distinct().toList();
    }
}
