package com.example.todayEng.domain.user.entity;

import com.example.todayEng.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "refresh_token", indexes = @Index(name = "idx_refresh_token_jti", columnList = "jti", unique = true))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 36)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    private RefreshToken(User user, String jti, LocalDateTime expiresAt) {
        this.user = user;
        this.jti = jti;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken create(User user, String jti, LocalDateTime expiresAt) {
        return new RefreshToken(user, jti, expiresAt);
    }
}
