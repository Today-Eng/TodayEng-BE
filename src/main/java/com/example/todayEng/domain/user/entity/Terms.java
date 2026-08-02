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
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_terms_type",
                        columnNames = "terms_type"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Terms extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "term_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "terms_type",
            nullable = false,
            length = 60
    )
    private TermsType termsType;

    /*
     * 기존 로컬 DB 스키마에 남아 있는 NOT NULL `term` 컬럼과의 호환 필드입니다.
     * 약관의 실제 식별과 필수 여부는 termsType을 기준으로 사용합니다.
     */
    @Column(
            name = "term",
            nullable = false,
            length = 255
    )
    private String term;

    @Column(
            name = "content",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String content;

    private Terms(
            TermsType termsType,
            String content
    ) {
        this.termsType = termsType;
        this.term = termsType.getDisplayName();
        this.content = content;
    }

    public static Terms create(
            TermsType termsType,
            String content
    ) {
        return new Terms(termsType, content);
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

    public void updateContent(String content) {
        this.content = content;
    }
}
