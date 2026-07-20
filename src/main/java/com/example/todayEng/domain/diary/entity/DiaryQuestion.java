package com.example.todayEng.domain.diary.entity;

import com.example.todayEng.domain.diary.entity.enums.QuestionGenerationType;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@Table(name = "diary_question")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_question_id")
    private DiaryQuestion parentQuestion;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_type", nullable = false, length = 20)
    private QuestionGenerationType generationType;

    @Column(
            name = "korean_translation",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String koreanTranslation;

    @Column(name = "keyword", length = 50)
    private String keyword;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private DiaryQuestion(
            Diary diary,
            DiaryQuestion parentQuestion,
            QuestionType questionType,
            Integer questionOrder,
            String questionText,
            QuestionGenerationType generationType,
            String koreanTranslation,
            String keyword
    ) {
        validateParentQuestion(questionType, parentQuestion);
        validateKeyword(questionType, keyword);

        this.diary = diary;
        this.parentQuestion = parentQuestion;
        this.questionType = questionType;
        this.questionOrder = questionOrder;
        this.questionText = questionText;
        this.generationType = generationType;
        this.koreanTranslation = koreanTranslation;
        this.keyword = keyword;
    }

    public static DiaryQuestion createMainQuestion(
            Diary diary,
            Integer questionOrder,
            String questionText,
            QuestionGenerationType generationType,
            String koreanTranslation,
            String keyword
    ) {
        return DiaryQuestion.builder()
                .diary(diary)
                .questionType(QuestionType.MAIN)
                .questionOrder(questionOrder)
                .questionText(questionText)
                .generationType(generationType)
                .koreanTranslation(koreanTranslation)
                .keyword(keyword)
                .build();
    }

    public static DiaryQuestion createFollowUpQuestion(
            Diary diary,
            DiaryQuestion parentQuestion,
            Integer questionOrder,
            String questionText,
            QuestionGenerationType generationType,
            String koreanTranslation
    ) {
        return DiaryQuestion.builder()
                .diary(diary)
                .parentQuestion(parentQuestion)
                .questionType(QuestionType.FOLLOW_UP)
                .questionOrder(questionOrder)
                .questionText(questionText)
                .generationType(generationType)
                .koreanTranslation(koreanTranslation)
                .build();
    }

    private static void validateParentQuestion(
            QuestionType questionType,
            DiaryQuestion parentQuestion
    ) {
        if (questionType == QuestionType.MAIN && parentQuestion != null) {
            throw new IllegalArgumentException(
                    "메인 질문에는 상위 질문을 지정할 수 없습니다."
            );
        }

        if (questionType == QuestionType.FOLLOW_UP
                && parentQuestion == null) {
            throw new IllegalArgumentException(
                    "후속 질문에는 메인 질문이 필요합니다."
            );
        }

        if (parentQuestion != null
                && parentQuestion.questionType != QuestionType.MAIN) {
            throw new IllegalArgumentException(
                    "후속 질문의 상위 질문은 메인 질문이어야 합니다."
            );
        }
    }

    private static void validateKeyword(
            QuestionType questionType,
            String keyword
    ) {
        if (questionType == QuestionType.FOLLOW_UP && keyword != null) {
            throw new IllegalArgumentException(
                    "후속 질문에는 키워드를 지정할 수 없습니다."
            );
        }
    }
}