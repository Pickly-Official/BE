package com.be.domain.participant.entity;

import com.be.domain.photo.entity.Photo;
import com.be.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "swipes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"participant_id", "photo_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Swipe extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id", nullable = false)
    private Photo photo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SwipeChoice choice;

    public static Swipe of(Participant participant, Photo photo, SwipeChoice choice) {
        Swipe swipe = new Swipe();
        swipe.participant = participant;
        swipe.photo = photo;
        swipe.choice = choice;
        return swipe;
    }
}
