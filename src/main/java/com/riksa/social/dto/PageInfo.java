package com.riksa.social.dto;

import java.util.UUID;

public record PageInfo(
        boolean hasNextPage,
        UUID endCursor
) {
}
