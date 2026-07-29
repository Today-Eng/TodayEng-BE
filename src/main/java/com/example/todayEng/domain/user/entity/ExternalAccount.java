package com.example.todayEng.domain.user.entity;

import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "external_account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_external_account_user_provider",
                        columnNames = {"user_id", "provider"}
                ),
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_external_account_user"
            )
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private ExternalServiceProvider provider;

    /*
     * 계정 연동은 유지하면서 해당 외부 서비스의 데이터를
     * 회고 질문 생성에 사용할지를 나타냅니다.
     */
    @Column(name = "use_enabled", nullable = false)
    private boolean useEnabled;

    /*
     * 외부 서비스가 발급한 계정 고유 식별값입니다.
     * Google Calendar는 Google 계정의 sub,
     * Spotify는 Spotify 사용자 ID를 저장합니다.
     *
     * TodayEng 로그인에 사용한 계정의 식별값과는 별개입니다.
     */
    @Column(
            name = "provider_account_id",
            nullable = false,
            length = 255
    )
    private String providerAccountId;

    /*
     * 연동 관리 화면에 표시할 계정 정보입니다.
     * Google Calendar는 이메일,
     * Spotify는 이메일 또는 표시 이름을 저장할 수 있습니다.
     */
    @Column(name = "account_identifier", length = 255)
    private String accountIdentifier;

    @Column(
            name = "access_token",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    private ExternalAccount(
            User user,
            ExternalServiceProvider provider,
            String providerAccountId,
            String accountIdentifier,
            String accessToken,
            String refreshToken,
            LocalDateTime tokenExpiresAt
    ) {
        this.user = user;
        this.provider = provider;
        this.providerAccountId = providerAccountId;
        this.accountIdentifier = accountIdentifier;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiresAt = tokenExpiresAt;
        this.connectedAt = LocalDateTime.now();
        this.useEnabled = true;
    }

    public static ExternalAccount createGoogleCalendarAccount(
            User user,
            String providerAccountId,
            String accountIdentifier,
            String accessToken,
            String refreshToken,
            LocalDateTime tokenExpiresAt
    ) {
        return new ExternalAccount(
                user,
                ExternalServiceProvider.GOOGLE_CALENDAR,
                providerAccountId,
                accountIdentifier,
                accessToken,
                refreshToken,
                tokenExpiresAt
        );
    }

    public static ExternalAccount createSpotifyAccount(
            User user,
            String providerAccountId,
            String accountIdentifier,
            String accessToken,
            String refreshToken,
            LocalDateTime tokenExpiresAt
    ) {
        return new ExternalAccount(
                user,
                ExternalServiceProvider.SPOTIFY,
                providerAccountId,
                accountIdentifier,
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

    public void reconnect(
            String providerAccountId,
            String accountIdentifier,
            String accessToken,
            String refreshToken,
            LocalDateTime tokenExpiresAt
    ) {
        boolean isAccountSwitched =
                !this.providerAccountId.equals(providerAccountId);

        this.providerAccountId = providerAccountId;
        this.accountIdentifier = accountIdentifier;

        if (isAccountSwitched) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.tokenExpiresAt = tokenExpiresAt;
        } else {
            updateTokens(accessToken, refreshToken, tokenExpiresAt);
        }

        this.connectedAt = LocalDateTime.now();
        this.useEnabled = true;
    }

    public void updateUseEnabled(boolean useEnabled) {
        this.useEnabled = useEnabled;
    }

    public void enableUse() {
        this.useEnabled = true;
    }

    public void disableUse() {
        this.useEnabled = false;
    }
}