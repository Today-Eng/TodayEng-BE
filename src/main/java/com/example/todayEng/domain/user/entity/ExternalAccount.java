package com.example.todayEng.domain.user.entity;

import com.example.todayEng.domain.user.entity.enums.AuthProvider;
import com.example.todayEng.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "external_account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_external_account_provider_account",
                        columnNames = {"provider", "provider_account_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "external_account_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_external_account_user")
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private AuthProvider provider;

    /*
     * Google이 발급한 사용자 고유 식별자(sub).
     * 이메일보다 변경 가능성이 낮으므로 로그인 식별값으로 사용한다.
     */
    @Column(name = "provider_account_id", nullable = false, length = 255)
    private String providerAccountId;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    private ExternalAccount(
            User user,
            AuthProvider provider,
            String providerAccountId,
            String accessToken,
            String refreshToken,
            LocalDateTime tokenExpiresAt
    ) {
        this.user = user;
        this.provider = provider;
        this.providerAccountId = providerAccountId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiresAt = tokenExpiresAt;
        this.connectedAt = LocalDateTime.now();
    }

    public static ExternalAccount createGoogleAccount(
            User user,
            String providerAccountId,
            String accessToken,
            String refreshToken,
            LocalDateTime tokenExpiresAt
    ) {
        return new ExternalAccount(
                user,
                AuthProvider.GOOGLE,
                providerAccountId,
                accessToken,
                refreshToken,
                tokenExpiresAt
        );
    }

    public void updateTokens(
            String accessToken,
            String refreshToken,
            LocalDateTime tokenExpiresAt
    ) {
        this.accessToken = accessToken;

        if (refreshToken != null && !refreshToken.isBlank()) {
            this.refreshToken = refreshToken;
        }

        this.tokenExpiresAt = tokenExpiresAt;
    }

    public void clearTokens() {
        this.accessToken = null;
        this.refreshToken = null;
        this.tokenExpiresAt = null;
    }
}