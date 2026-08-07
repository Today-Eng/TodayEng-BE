package com.example.todayEng.domain.user.entity;

import com.example.todayEng.domain.user.entity.enums.TermsType;
import com.example.todayEng.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "terms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_terms_type_version",
                columnNames = {"terms_type", "version"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Terms extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "term_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "terms_type", nullable = false, length = 60)
    private TermsType termsType;

    @Column(name = "term", nullable = false, length = 255)
    private String term;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "active", nullable = false)
    private boolean active;

    private Terms(TermsType termsType, String content, int version) {
        this.termsType = termsType;
        this.term = termsType.getDisplayName();
        this.content = content;
        this.version = version;
        this.active = true;
    }

    public static Terms create(TermsType termsType, String content, int version) {
        return new Terms(termsType, content, version);
    }

    public static Terms create(TermsType termsType, String content) {
        return create(termsType, content, 1);
    }

    public String getDisplayName() {
        return termsType.getDisplayName();
    }

    public boolean isRequired() {
        return termsType.isRequired();
    }

    public int getDisplayOrder() {
        return termsType.getDisplayOrder();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
