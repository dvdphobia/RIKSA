package com.riksa.gamification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.Base64;
import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CursorPage<T> {
    private final List<T> edges;
    private final boolean hasNextPage;
    private final String endCursor;

    public CursorPage(List<T> edges, boolean hasNextPage, String endCursor) {
        this.edges = edges;
        this.hasNextPage = hasNextPage;
        this.endCursor = endCursor;
    }

    public static <T> CursorPage<T> of(List<T> all, int limit) {
        if (all.size() <= limit) {
            return new CursorPage<>(all, false, null);
        }
        List<T> edges = all.subList(0, limit);
        return new CursorPage<>(edges, true, null);
    }

    public static <T> CursorPage<T> of(List<T> all, int limit, String endCursor) {
        if (all.size() <= limit) {
            return new CursorPage<>(all, false, null);
        }
        List<T> edges = all.subList(0, limit);
        return new CursorPage<>(edges, true, endCursor);
    }
}
