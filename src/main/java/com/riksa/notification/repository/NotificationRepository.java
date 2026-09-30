package com.riksa.notification.repository;

import com.riksa.notification.entity.Notification;
import com.riksa.notification.entity.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientIdOrderByCreatedAtDescIdDesc(UUID recipientId, Pageable pageable);

    @Query("""
            select n from Notification n
            where n.recipientId = :recipientId
              and (n.createdAt < :createdAt or (n.createdAt = :createdAt and n.id < :id))
            order by n.createdAt desc, n.id desc
            """)
    List<Notification> findPageAfter(@Param("recipientId") UUID recipientId,
                                     @Param("createdAt") OffsetDateTime createdAt,
                                     @Param("id") UUID id,
                                     Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(UUID recipientId);

    Optional<Notification> findFirstByRecipientIdAndActorIdAndTypeAndTargetId(
            UUID recipientId, UUID actorId, NotificationType type, UUID targetId);

    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.recipientId = :recipientId and n.readAt is null")
    int markAllRead(@Param("recipientId") UUID recipientId, @Param("now") OffsetDateTime now);
}
