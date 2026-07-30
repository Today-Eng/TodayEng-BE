package com.example.todayEng.domain.user.entity;

import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "oauth_authorization_request",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_oauth_authorization_request_state_hash",
                        columnNames = "state_hash"
                )
        },
        indexes = {
                @Index(
                        name = "idx_oauth_authorization_request_expires_at",
                        columnList = "expires_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthAuthorizationRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_authorization_request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_oauth_authorization_request_user"
            )
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private ExternalServiceProvider provider;

    @Column(
            name = "state_hash",
            nullable = false,
            length = 64,
            updatable = false
    )
    private String stateHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    private OAuthAuthorizationRequest(
            User user,
            ExternalServiceProvider provider,
            String stateHash,
            LocalDateTime expiresAt
    ) {
        this.user = user;
        this.provider = provider;
        this.stateHash = stateHash;
        this.expiresAt = expiresAt;
    }

    public static OAuthAuthorizationRequest create(
            User user,
            ExternalServiceProvider provider,
            String stateHash,
            LocalDateTime expiresAt
    ) {
        return new OAuthAuthorizationRequest(
                user,
                provider,
                stateHash,
                expiresAt
        );
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}