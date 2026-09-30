package com.riksa.social.repository;

import com.riksa.social.entity.PostMedia;
import com.riksa.social.entity.PostMediaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PostMediaRepository extends JpaRepository<PostMedia, PostMediaId> {

    @Query("""
            SELECT media FROM PostMedia media
            WHERE media.id.postId IN :postIds
            ORDER BY media.id.postId, media.id.position
            """)
    List<PostMedia> findAllByPostIds(@Param("postIds") List<UUID> postIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM PostMedia media WHERE media.id.postId = :postId")
    void deleteByPostId(@Param("postId") UUID postId);
}
