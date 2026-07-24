package com.example.todayEng.domain.user.entity;

import com.example.todayEng.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "user_terms",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_terms_user_term",
                        columnNames = {
                                "user_id",
                                "term_id"
                        }
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTerms extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_term_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_user_terms_user"
            )
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "term_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_user_terms_term"
            )
    )
    private Terms terms;

    @Column(name = "agree", nullable = false)
    private boolean agree;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

    private UserTerms(
            User user,
            Terms terms,
            boolean agree
    ) {
        this.user = user;
        this.terms = terms;
        updateAgreement(agree);
    }

    public static UserTerms create(
            User user,
            Terms terms,
            boolean agree
    ) {
        return new UserTerms(
                user,
                terms,
                agree
        );
    }

    public void updateAgreement(boolean agree) {
        this.agree = agree;
        this.agreedAt = agree
                ? LocalDateTime.now()
                : null;
    }
}