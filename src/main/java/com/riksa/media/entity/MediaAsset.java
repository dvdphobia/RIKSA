package com.riksa.media.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "media_assets")
@Getter
@Setter
@NoArgsConstructor
public class MediaAsset {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "public_url", nullable = false, columnDefinition = "TEXT")
    private String publicUrl;

    public MediaAsset(String id, UUID ownerId, String publicUrl) {
        this.id = id;
        this.ownerId = ownerId;
        this.publicUrl = publicUrl;
    }
}
