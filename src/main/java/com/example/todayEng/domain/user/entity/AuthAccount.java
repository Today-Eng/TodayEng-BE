package com.example.todayEng.domain.user.entity;

import com.example.todayEng.domain.user.entity.enums.AuthProvider;
import com.example.todayEng.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "auth_account", uniqueConstraints = {
        @UniqueConstraint(name = "uk_auth_account_provider_subject", columnNames = {"provider", "provider_subject"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthAccount extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_account_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    private AuthAccount(User user, AuthProvider provider, String providerSubject) {
        this.user = user;
        this.provider = provider;
        this.providerSubject = providerSubject;
    }

    public static AuthAccount google(User user, String subject) {
        return new AuthAccount(user, AuthProvider.GOOGLE, subject);
    }

    public static AuthAccount test(User user, String socialUid) {
        return new AuthAccount(user, AuthProvider.TEST, socialUid);
    }
}
