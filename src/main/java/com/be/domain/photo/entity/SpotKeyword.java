package com.be.domain.photo.entity;

import com.be.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "spot_keywords")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotKeyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_spot_id", nullable = false)
    private PhotoSpot photoSpot;

    @Column(nullable = false, length = 50)
    private String keyword;

    public static SpotKeyword of(PhotoSpot photoSpot, String keyword) {
        SpotKeyword sk = new SpotKeyword();
        sk.photoSpot = photoSpot;
        sk.keyword = keyword;
        return sk;
    }
}
