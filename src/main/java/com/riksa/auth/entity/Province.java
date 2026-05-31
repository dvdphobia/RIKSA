package com.riksa.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "provinces")
@Getter
@Setter

public class Province {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "name_khmer", unique = true, nullable = false, length = 100)
    private String nameKhmer;

    @Column(name = "name_latin", unique = true, nullable = false, length = 100)
    private String nameLatin;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
