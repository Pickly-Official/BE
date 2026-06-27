package com.be.domain.photo.entity;

import com.be.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "photo_spots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoSpot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false)
    private Integer recommendRate = 0;

    @Column(nullable = false)
    private Integer photoCount = 0;

    @OneToMany(mappedBy = "photoSpot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpotKeyword> keywords = new ArrayList<>();

    public static PhotoSpot of(String name, BigDecimal latitude, BigDecimal longitude) {
        PhotoSpot spot = new PhotoSpot();
        spot.name = name;
        spot.latitude = latitude;
        spot.longitude = longitude;
        spot.recommendRate = 0;
        spot.photoCount = 0;
        return spot;
    }

    public void increasePhotoCount() {
        this.photoCount++;
    }

    public void addKeyword(String keyword) {
        this.keywords.add(SpotKeyword.of(this, keyword));
    }
}
