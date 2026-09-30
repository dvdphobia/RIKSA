package com.riksa.notification.service;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.notification.dto.NotificationPageResponse;
import com.riksa.notification.entity.Notification;
import com.riksa.notification.entity.NotificationType;
import com.riksa.notification.repository.NotificationRepository;
import com.riksa.realtime.RealtimeEvent;
import com.riksa.realtime.RealtimeTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository repository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private NotificationService service;

    private final UUID owner = UUID.randomUUID();
    private final UUID actor = UUID.randomUUID();
    private final UUID post = UUID.randomUUID();

    @Test
    void recordsANewReactionAndPushesItToTheOwner() {
        when(repository.findFirstByRecipientIdAndActorIdAndTypeAndTargetId(
                owner, actor, NotificationType.POST_REACTION, post)).thenReturn(Optional.empty());
        when(repository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
        when(repository.countByRecipientIdAndReadAtIsNull(owner)).thenReturn(3L);

        service.notify(owner, actor, NotificationType.POST_REACTION, "POST", post, post, "My first post");

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getRecipientId()).isEqualTo(owner);
        assertThat(saved.getValue().getActorId()).isEqualTo(actor);
        assertThat(saved.getValue().getPostId()).isEqualTo(post);
        assertThat(saved.getValue().getPreview()).isEqualTo("My first post");
        assertThat(saved.getValue().getReadAt()).isNull();

        ArgumentCaptor<RealtimeEvent> event = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        RealtimeEvent realtime = event.getValue();
        assertThat(realtime.getRecipientId()).isEqualTo(owner);
        assertThat(realtime.getType()).isEqualTo(RealtimeTypes.NOTIFICATION_CREATED);
        assertThat(realtime.getData()).containsEntry("unreadCount", 3L);
    }

    @Test
    void ignoresPeopleActingOnTheirOwnContent() {
        service.notify(owner, owner, NotificationType.POST_COMMENT, "COMMENT", post, post, "hi");

        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void ignoresContentWithoutAnOwner() {
        service.notify(null, actor, NotificationType.POST_REACTION, "POST", post, post, null);

        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void aRepeatReactionRefreshesTheSameRowAsUnread() {
        Notification existing = new Notification();
        existing.setId(UUID.randomUUID());
        existing.setRecipientId(owner);
        existing.setReadAt(OffsetDateTime.now().minusDays(1));
        existing.setCreatedAt(OffsetDateTime.now().minusDays(1));
        when(repository.findFirstByRecipientIdAndActorIdAndTypeAndTargetId(
                owner, actor, NotificationType.POST_REACTION, post)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.notify(owner, actor, NotificationType.POST_REACTION, "POST", post, post, "My first post");

        assertThat(existing.getReadAt()).isNull();
        assertThat(existing.getCreatedAt()).isAfter(OffsetDateTime.now().minusMinutes(1));
        verify(repository).save(existing);
    }

    @Test
    void commentsNeverCollapse() {
        when(repository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        service.notify(owner, actor, NotificationType.POST_COMMENT, "COMMENT", UUID.randomUUID(), post, "Nice");

        verify(repository, never()).findFirstByRecipientIdAndActorIdAndTypeAndTargetId(any(), any(), any(), any());
    }

    @Test
    void previewsAreFlattenedAndShortened() {
        assertThat(NotificationService.preview("  two\n\nlines  ")).isEqualTo("two lines");
        assertThat(NotificationService.preview("   ")).isNull();
        String longText = "x".repeat(300);
        String preview = NotificationService.preview(longText);
        assertThat(preview).hasSize(140).endsWith("…");
    }

    @Test
    void listPagesWithACursorAndAttachesActors() {
        List<Notification> rows = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (int i = 0; i < 3; i++) {
            Notification n = new Notification();
            n.setId(UUID.randomUUID());
            n.setRecipientId(owner);
            n.setActorId(actor);
            n.setType(NotificationType.POST_COMMENT);
            n.setTargetType("COMMENT");
            n.setTargetId(UUID.randomUUID());
            n.setPostId(post);
            n.setCreatedAt(now.minusMinutes(i));
            rows.add(n);
        }
        when(repository.findByRecipientIdOrderByCreatedAtDescIdDesc(eq(owner), any(Pageable.class))).thenReturn(rows);
        User user = new User();
        user.setId(actor);
        user.setUsername("dara");
        user.setDisplayName("Dara");
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user));
        when(repository.countByRecipientIdAndReadAtIsNull(owner)).thenReturn(2L);

        NotificationPageResponse page = service.list(owner, null, 2);

        assertThat(page.items()).hasSize(2);
        assertThat(page.items().get(0).actor().displayName()).isEqualTo("Dara");
        assertThat(page.items().get(0).type()).isEqualTo("POST_COMMENT");
        assertThat(page.unreadCount()).isEqualTo(2L);
        assertThat(page.nextCursor()).isNotBlank();

        NotificationService.Cursor cursor = NotificationService.Cursor.decode(page.nextCursor());
        assertThat(cursor.id()).isEqualTo(rows.get(1).getId());
        assertThat(cursor.createdAt()).isEqualTo(rows.get(1).getCreatedAt());
    }

    @Test
    void aBadCursorIsABadRequest() {
        assertThatThrownBy(() -> service.list(owner, "not-a-cursor", 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void youCanOnlyMarkYourOwnNotificationRead() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndRecipientId(id, owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(owner, id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markReadReturnsTheNewUnreadCount() {
        UUID id = UUID.randomUUID();
        Notification n = new Notification();
        n.setId(id);
        n.setRecipientId(owner);
        when(repository.findByIdAndRecipientId(id, owner)).thenReturn(Optional.of(n));
        when(repository.countByRecipientIdAndReadAtIsNull(owner)).thenReturn(0L);

        assertThat(service.markRead(owner, id)).isZero();
        assertThat(n.getReadAt()).isNotNull();
    }
}
