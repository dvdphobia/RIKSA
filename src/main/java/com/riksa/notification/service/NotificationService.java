package com.riksa.notification.service;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.notification.dto.NotificationPageResponse;
import com.riksa.notification.dto.NotificationResponse;
import com.riksa.notification.entity.Notification;
import com.riksa.notification.entity.NotificationType;
import com.riksa.notification.repository.NotificationRepository;
import com.riksa.realtime.RealtimeEvent;
import com.riksa.realtime.RealtimeTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Writes and reads the in-app inbox.
 *
 * <p>{@link #notify} runs inside the caller's transaction, so a comment or
 * reaction that rolls back leaves no notification behind. The live push to the
 * recipient's open app is sent only after that transaction commits.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final int MAX_PAGE_SIZE = 50;
    private static final int PREVIEW_LENGTH = 140;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Records that {@code actorId} did something to {@code recipientId}'s content.
     * Does nothing without a recipient, or when people act on their own content.
     */
    @Transactional
    public void notify(UUID recipientId, UUID actorId, NotificationType type,
                       String targetType, UUID targetId, UUID postId, String previewText) {
        if (recipientId == null || targetId == null || Objects.equals(recipientId, actorId)) {
            return;
        }
        Notification notification = null;
        if (type.collapsesRepeats() && actorId != null) {
            notification = notificationRepository
                    .findFirstByRecipientIdAndActorIdAndTypeAndTargetId(recipientId, actorId, type, targetId)
                    .orElse(null);
        }
        if (notification == null) {
            notification = new Notification();
            notification.setRecipientId(recipientId);
            notification.setActorId(actorId);
            notification.setType(type);
            notification.setTargetType(targetType);
            notification.setTargetId(targetId);
            notification.setPostId(postId);
        }
        notification.setPreview(preview(previewText));
        notification.setReadAt(null);
        notification.setCreatedAt(OffsetDateTime.now());
        notification = notificationRepository.save(notification);

        long unread = notificationRepository.countByRecipientIdAndReadAtIsNull(recipientId);
        eventPublisher.publishEvent(RealtimeEvent.toUser(this, recipientId,
                RealtimeTypes.NOTIFICATION_CREATED,
                Map.of("id", notification.getId(), "unreadCount", unread)));
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse list(UUID recipientId, String cursor, int limit) {
        int size = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        // One extra row tells us whether another page exists.
        PageRequest page = PageRequest.of(0, size + 1);
        List<Notification> rows;
        if (cursor == null || cursor.isBlank()) {
            rows = notificationRepository.findByRecipientIdOrderByCreatedAtDescIdDesc(recipientId, page);
        } else {
            Cursor after = Cursor.decode(cursor);
            rows = notificationRepository.findPageAfter(recipientId, after.createdAt(), after.id(), page);
        }
        boolean more = rows.size() > size;
        if (more) rows = rows.subList(0, size);

        Set<UUID> actorIds = new HashSet<>();
        rows.forEach(n -> {
            if (n.getActorId() != null) actorIds.add(n.getActorId());
        });
        Map<UUID, User> actors = userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<NotificationResponse> items = rows.stream()
                .map(n -> toResponse(n, n.getActorId() == null ? null : actors.get(n.getActorId())))
                .toList();
        String next = null;
        if (more) {
            Notification last = rows.get(rows.size() - 1);
            next = new Cursor(last.getCreatedAt(), last.getId()).encode();
        }
        return new NotificationPageResponse(items,
                notificationRepository.countByRecipientIdAndReadAtIsNull(recipientId), next);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID recipientId) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(recipientId);
    }

    /** Marks one of the recipient's notifications read and returns the new unread count. */
    @Transactional
    public long markRead(UUID recipientId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (notification.getReadAt() == null) {
            notification.setReadAt(OffsetDateTime.now());
            notificationRepository.save(notification);
        }
        return notificationRepository.countByRecipientIdAndReadAtIsNull(recipientId);
    }

    @Transactional
    public void markAllRead(UUID recipientId) {
        notificationRepository.markAllRead(recipientId, OffsetDateTime.now());
    }

    private NotificationResponse toResponse(Notification n, User actor) {
        NotificationResponse.Actor actorView = actor == null ? null
                : new NotificationResponse.Actor(actor.getId(), actor.getUsername(),
                        actor.getDisplayName(), actor.getAvatarUrl());
        return new NotificationResponse(n.getId(), n.getType().name(), actorView, n.getTargetType(),
                n.getTargetId(), n.getPostId(), n.getPreview(), n.getReadAt() != null, n.getCreatedAt());
    }

    static String preview(String text) {
        if (text == null) return null;
        String flat = text.replaceAll("\\s+", " ").trim();
        if (flat.isEmpty()) return null;
        return flat.length() <= PREVIEW_LENGTH ? flat : flat.substring(0, PREVIEW_LENGTH - 1).trim() + "…";
    }

    /** Opaque keyset cursor: the last row's createdAt and id. */
    record Cursor(OffsetDateTime createdAt, UUID id) {
        String encode() {
            String raw = createdAt + "|" + id;
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }

        static Cursor decode(String value) {
            try {
                String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
                int bar = raw.indexOf('|');
                return new Cursor(OffsetDateTime.parse(raw.substring(0, bar)), UUID.fromString(raw.substring(bar + 1)));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Invalid cursor");
            }
        }
    }
}
