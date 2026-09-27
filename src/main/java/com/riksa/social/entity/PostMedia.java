package com.riksa.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "post_media")
@Getter
@Setter
@NoArgsConstructor
public class PostMedia {

    @EmbeddedId
    private PostMediaId id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    private Integer width;

    private Integer height;

    public PostMedia(PostMediaId id, String url, Integer width, Integer height) {
        this.id = id;
        this.url = url;
        this.width = width;
        this.height = height;
    }
}
