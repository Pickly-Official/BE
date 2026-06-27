package com.be.domain.photo.entity;

import com.be.domain.vote.entity.Vote;
import com.be.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "photos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id", nullable = false)
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_spot_id")
    private PhotoSpot photoSpot;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Integer sequence;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    private LocalDateTime takenAt;

    public static Photo of(Vote vote, String imageUrl, int sequence) {
        Photo photo = new Photo();
        photo.vote = vote;
        photo.imageUrl = imageUrl;
        photo.sequence = sequence;
        return photo;
    }

    public static Photo ofWithLocation(Vote vote, String imageUrl, int sequence,
                                       BigDecimal latitude, BigDecimal longitude,
                                       LocalDateTime takenAt) {
        Photo photo = of(vote, imageUrl, sequence);
        photo.latitude = latitude;
        photo.longitude = longitude;
        photo.takenAt = takenAt;
        return photo;
    }

    public void assignSpot(PhotoSpot photoSpot) {
        this.photoSpot = photoSpot;
    }
}
