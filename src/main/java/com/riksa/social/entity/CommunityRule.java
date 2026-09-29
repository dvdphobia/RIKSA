package com.riksa.social.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "community_rules")
@Getter @Setter @NoArgsConstructor
public class CommunityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String body;
}
