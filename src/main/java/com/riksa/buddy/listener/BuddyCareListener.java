package com.riksa.buddy.listener;

import com.riksa.buddy.service.BuddyCareRules;
import com.riksa.buddy.service.BuddyService;
import com.riksa.blog.event.BlogPostApprovedEvent;
import com.riksa.learning.event.LessonCompletedEvent;
import com.riksa.social.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class BuddyCareListener {

    private final BuddyService buddyService;

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLessonCompleted(LessonCompletedEvent event) {
        record("LESSON", event.getUserId(), event.getLessonId());
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostCreated(PostCreatedEvent event) {
        record("POST", event.getAuthorId(), event.getPostId());
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBlogPostApproved(BlogPostApprovedEvent event) {
        record(BuddyCareRules.BLOG_PUBLISHED, event.getAuthorId(), event.getBlogPostId());
    }

    private void record(String sourceKey, java.util.UUID userId, java.util.UUID sourceId) {
        try {
            buddyService.recordCare(userId, sourceKey, sourceId);
        } catch (Exception exception) {
            log.error("Buddy care failed for user {} and source {}:{}", userId, sourceKey, sourceId, exception);
        }
    }
}
