package com.be.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static User of(String provider, String providerId, String nickname) {
        User u = new User();
        u.provider = provider;
        u.providerId = providerId;
        u.nickname = nickname;
        u.createdAt = LocalDateTime.now();
        return u;
    }
}
