package com.riksa.admin.repository;

import com.riksa.admin.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    List<Announcement> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT announcement FROM Announcement announcement
            WHERE announcement.startsAt <= :now
              AND (announcement.endsAt IS NULL OR announcement.endsAt > :now)
            ORDER BY announcement.startsAt DESC, announcement.createdAt DESC
            """)
    List<Announcement> findActiveAt(@Param("now") OffsetDateTime now);
}
