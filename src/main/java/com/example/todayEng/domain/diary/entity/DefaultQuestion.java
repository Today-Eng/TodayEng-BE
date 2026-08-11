package com.example.todayEng.domain.diary.entity;

import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.domain.user.entity.InterestTag;
import com.example.todayEng.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "default_question",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_default_question_code",
                columnNames = "question_code"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DefaultQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "default_question_id")
    private Long id;

    @Column(name = "question_code", nullable = false, length = 100)
    private String questionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_tag_id")
    private InterestTag interestTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "korean_translation", nullable = false, columnDefinition = "TEXT")
    private String koreanTranslation;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Builder(access = AccessLevel.PRIVATE)
    private DefaultQuestion(
            String questionCode,
            InterestTag interestTag,
            QuestionType questionType,
            String questionText,
            String koreanTranslation
    ) {
        if (questionCode == null || questionCode.isBlank()) {
            throw new IllegalArgumentException("Default question code is required.");
        }
        if (questionType == QuestionType.MAIN && interestTag == null) {
            throw new IllegalArgumentException("MAIN default question requires an interest tag.");
        }
        this.questionCode = questionCode;
        this.interestTag = interestTag;
        this.questionType = questionType;
        this.questionText = questionText;
        this.koreanTranslation = koreanTranslation;
        this.active = true;
    }

    public static DefaultQuestion createMain(
            String questionCode,
            InterestTag interestTag,
            String questionText,
            String koreanTranslation
    ) {
        return DefaultQuestion.builder()
                .questionCode(questionCode)
                .interestTag(interestTag)
                .questionType(QuestionType.MAIN)
                .questionText(questionText)
                .koreanTranslation(koreanTranslation)
                .build();
    }

    public static DefaultQuestion createFollowUp(
            String questionCode,
            InterestTag interestTag,
            String questionText,
            String koreanTranslation
    ) {
        return DefaultQuestion.builder()
                .questionCode(questionCode)
                .interestTag(interestTag)
                .questionType(QuestionType.FOLLOW_UP)
                .questionText(questionText)
                .koreanTranslation(koreanTranslation)
                .build();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
