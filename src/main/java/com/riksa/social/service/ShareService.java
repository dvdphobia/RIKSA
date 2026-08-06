package com.riksa.social.service;

import com.riksa.social.entity.Post;
import com.riksa.social.entity.Share;
import com.riksa.social.enums.ContentStatus;
import com.riksa.social.enums.SharePlatform;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.ShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareRepository shareRepository;
    private final PostRepository postRepository;

    @Transactional
    public Post sharePost(UUID userId, UUID postId, SharePlatform platform) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));

        if (post.getStatus() == ContentStatus.HIDDEN) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }

        Share share = new Share();
        share.setUserId(userId);
        share.setPostId(postId);
        share.setPlatform(platform != null ? platform : SharePlatform.COPY_LINK);
        shareRepository.save(share);

        post.setShareCount(post.getShareCount() + 1);
        return postRepository.save(post);
    }
}