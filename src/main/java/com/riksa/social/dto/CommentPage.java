package com.riksa.social.dto;

import java.util.List;

public record CommentPage(
        List<CommentResponse> edges,
        PageInfo pageInfo
) {
}
