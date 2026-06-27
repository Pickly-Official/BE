package com.be.domain.vote.entity;

import com.be.domain.user.entity.User;
import com.be.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Entity
@Table(name = "votes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private Boolean useLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DeadlineType deadlineType;

    @Column(nullable = false)
    private LocalDateTime closedAt;

    public static Vote of(User user, String title, boolean useLocation, DeadlineType deadlineType) {
        Vote vote = new Vote();
        vote.user = user;
        vote.title = title;
        vote.useLocation = useLocation;
        vote.deadlineType = deadlineType;
        vote.closedAt = calculateClosedAt(deadlineType);
        return vote;
    }

    private static LocalDateTime calculateClosedAt(DeadlineType deadlineType) {
        LocalDateTime now = LocalDateTime.now();
        return switch (deadlineType) {
            case H24 -> now.plusHours(24);
            case D3  -> now.plusDays(3);
            case D7  -> now.plusDays(7);
        };
    }

    public boolean isClosed() {
        return LocalDateTime.now().isAfter(this.closedAt);
    }

    public String getDDay() {
        if (isClosed()) return null;
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), this.closedAt);
        return "D-" + days;
    }
}
