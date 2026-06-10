package com.riksa.social.dto;

import java.util.List;

public record FeedPage(
        List<PostResponse> edges,
        PageInfo pageInfo
) {
}
