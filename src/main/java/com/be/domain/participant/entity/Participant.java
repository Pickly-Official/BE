package com.be.domain.participant.entity;

import com.be.domain.vote.entity.Vote;
import com.be.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "participants",
       uniqueConstraints = @UniqueConstraint(columnNames = {"vote_id", "device_token"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id", nullable = false)
    private Vote vote;

    @Column(name = "device_token", nullable = false, length = 64)
    private String voterUuid;

    @Column(name = "user_id")
    private Long userId;

    private LocalDateTime completedAt;

    public static Participant of(Vote vote, String voterUuid) {
        Participant p = new Participant();
        p.vote = vote;
        p.voterUuid = voterUuid;
        return p;
    }

    public void updateUserId(Long userId) {
        this.userId = userId;
    }

    public void complete() {
        this.completedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return this.completedAt != null;
    }
}
