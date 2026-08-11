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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "default_question")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DefaultQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "default_question_id")
    private Long id;

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
            InterestTag interestTag,
            QuestionType questionType,
            String questionText,
            String koreanTranslation
    ) {
        if (questionType == QuestionType.MAIN && interestTag == null) {
            throw new IllegalArgumentException("MAIN 기본 질문에는 관심 분야가 필요합니다.");
        }
        this.interestTag = interestTag;
        this.questionType = questionType;
        this.questionText = questionText;
        this.koreanTranslation = koreanTranslation;
        this.active = true;
    }

    public static DefaultQuestion createMain(
            InterestTag interestTag,
            String questionText,
            String koreanTranslation
    ) {
        return DefaultQuestion.builder()
                .interestTag(interestTag)
                .questionType(QuestionType.MAIN)
                .questionText(questionText)
                .koreanTranslation(koreanTranslation)
                .build();
    }

    public static DefaultQuestion createFollowUp(
            InterestTag interestTag,
            String questionText,
            String koreanTranslation
    ) {
        return DefaultQuestion.builder()
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
