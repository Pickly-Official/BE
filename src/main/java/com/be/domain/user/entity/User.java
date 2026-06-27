package com.be.domain.user.entity;

import com.be.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 500)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    @Column(nullable = false, length = 255)
    private String providerId;

    /**
     * 일반(LOCAL) 가입 사용자의 BCrypt 인코딩된 비밀번호.
     * 소셜 로그인 사용자는 비밀번호가 없으므로 null 이다.
     */
    @Column(length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    public static User of(String email, String nickname, Provider provider, String providerId) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.provider = provider;
        user.providerId = providerId;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    /**
     * 일반 이메일/비밀번호 회원가입용 정적 팩토리.
     * provider = LOCAL, providerId 는 email 을 그대로 사용한다.
     *
     * @param encodedPassword PasswordEncoder 로 인코딩된 비밀번호 (평문 금지)
     */
    public static User ofLocal(String email, String nickname, String encodedPassword) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.password = encodedPassword;
        user.provider = Provider.LOCAL;
        user.providerId = email;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
    }
}
