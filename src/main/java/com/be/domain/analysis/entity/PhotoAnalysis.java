package com.be.domain.analysis.entity;

import com.be.domain.photo.entity.Photo;
import com.be.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "photo_analyses",
       uniqueConstraints = @UniqueConstraint(columnNames = {"photo_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id", nullable = false)
    private Photo photo;

    @Column(length = 10)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String composition;

    @Column(columnDefinition = "TEXT")
    private String expression;

    @Column(columnDefinition = "TEXT")
    private String lighting;

    @Column(columnDefinition = "TEXT")
    private String color;

    @Column(columnDefinition = "TEXT")
    private String background;

    public static PhotoAnalysis of(Photo photo, String composition, String expression,
                                   String lighting, String color, String background) {
        PhotoAnalysis pa = new PhotoAnalysis();
        pa.photo = photo;
        pa.composition = composition;
        pa.expression = expression;
        pa.lighting = lighting;
        pa.color = color;
        pa.background = background;
        return pa;
    }

    public static PhotoAnalysis ofPerson(Photo photo, String composition, String expression, String lighting) {
        PhotoAnalysis pa = new PhotoAnalysis();
        pa.photo = photo;
        pa.type = "person";
        pa.composition = composition;
        pa.expression = expression;
        pa.lighting = lighting;
        return pa;
    }

    public static PhotoAnalysis ofScene(Photo photo, String mood, String lighting) {
        PhotoAnalysis pa = new PhotoAnalysis();
        pa.photo = photo;
        pa.type = "scene";
        pa.composition = mood;
        pa.lighting = lighting;
        return pa;
    }
}
