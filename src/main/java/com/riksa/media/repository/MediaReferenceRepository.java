package com.riksa.media.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class MediaReferenceRepository {

    private static final String REFERENCE_QUERY = """
            SELECT EXISTS (
                SELECT 1 FROM posts WHERE media_url = :url OR content LIKE '%' || :url || '%'
                UNION ALL SELECT 1 FROM post_media WHERE url = :url
                UNION ALL SELECT 1 FROM comments WHERE media_url = :url OR content LIKE '%' || :url || '%'
                UNION ALL SELECT 1 FROM blog_posts
                    WHERE cover_url = :url OR banner_url = :url OR content LIKE '%' || :url || '%'
                UNION ALL SELECT 1 FROM categories
                    WHERE icon_url = :url OR banner_url = :url OR description LIKE '%' || :url || '%'
                UNION ALL SELECT 1 FROM lesson_resource WHERE url = :url
                UNION ALL SELECT 1 FROM content_reports WHERE details LIKE '%' || :url || '%'
            )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public boolean isReferenced(String url) {
        Boolean referenced = jdbcTemplate.queryForObject(
                REFERENCE_QUERY, Map.of("url", url), Boolean.class);
        return Boolean.TRUE.equals(referenced);
    }
}
