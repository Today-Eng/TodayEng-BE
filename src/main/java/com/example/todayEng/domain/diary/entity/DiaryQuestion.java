package com.example.todayEng.domain.diary.entity;

import com.example.todayEng.domain.diary.entity.enums.QuestionGenerationType;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.domain.diary.entity.enums.TtsStatus;
import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "diary_question",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_diary_question_diary_type_order",
                columnNames = {"diary_id", "question_type", "question_order"}
        )
)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_question_id")
    private DefaultQuestion defaultQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "context_id")
    private DiaryContext context;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "english_level_snapshot", length = 30)
    private EnglishLevel englishLevelSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "interest_snapshot", columnDefinition = "json")
    private JsonNode interestSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "tts_status", nullable = false, length = 20)
    @org.hibernate.annotations.ColumnDefault("'PENDING'")
    private TtsStatus ttsStatus;

    @Column(name = "tts_audio_key", length = 500)
    private String ttsAudioKey;

    @Column(name = "tts_error_message", length = 500)
    private String ttsErrorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private DiaryQuestion(
            Diary diary,
            DiaryQuestion parentQuestion,
            DefaultQuestion defaultQuestion,
            QuestionType questionType,
            Integer questionOrder,
            String questionText,
            QuestionGenerationType generationType,
            String koreanTranslation,
            String keyword,
            DiaryContext context,
            EnglishLevel englishLevelSnapshot,
            JsonNode interestSnapshot
    ) {
        validateParentQuestion(questionType, parentQuestion);
        validateKeyword(questionType, keyword);
        validateDefaultQuestion(generationType, defaultQuestion);

        this.diary = diary;
        this.parentQuestion = parentQuestion;
        this.defaultQuestion = defaultQuestion;
        this.questionType = questionType;
        this.questionOrder = questionOrder;
        this.questionText = questionText;
        this.generationType = generationType;
        this.koreanTranslation = koreanTranslation;
        this.keyword = keyword;
        this.context = context;
        this.englishLevelSnapshot = englishLevelSnapshot;
        this.interestSnapshot = interestSnapshot;
        this.ttsStatus = TtsStatus.PENDING;
    }

    public void completeTts(String audioKey) {
        this.ttsStatus = TtsStatus.SUCCEEDED;
        this.ttsAudioKey = audioKey;
        this.ttsErrorMessage = null;
    }

    public void failTts(String errorMessage) {
        this.ttsStatus = TtsStatus.FAILED;
        this.ttsAudioKey = null;
        this.ttsErrorMessage = errorMessage;
    }

    public static DiaryQuestion createGeneratedMainQuestion(
            Diary diary,
            DiaryContext context,
            Integer questionOrder,
            String questionText,
            String koreanTranslation,
            String keyword,
            EnglishLevel englishLevelSnapshot,
            JsonNode interestSnapshot
    ) {
        return DiaryQuestion.builder()
                .diary(diary)
                .context(context)
                .questionType(QuestionType.MAIN)
                .questionOrder(questionOrder)
                .questionText(questionText)
                .generationType(QuestionGenerationType.AI)
                .koreanTranslation(koreanTranslation)
                .keyword(keyword)
                .englishLevelSnapshot(englishLevelSnapshot)
                .interestSnapshot(interestSnapshot)
                .build();
    }

    public static DiaryQuestion createMainQuestion(
            Diary diary,
            Integer questionOrder,
            String questionText,
            QuestionGenerationType generationType,
            String koreanTranslation,
            String keyword,
            DefaultQuestion defaultQuestion
    ) {
        return DiaryQuestion.builder()
                .diary(diary)
                .questionType(QuestionType.MAIN)
                .questionOrder(questionOrder)
                .questionText(questionText)
                .generationType(generationType)
                .koreanTranslation(koreanTranslation)
                .keyword(keyword)
                .defaultQuestion(defaultQuestion)
                .build();
    }

    public static DiaryQuestion createFollowUpQuestion(
            Diary diary,
            DiaryQuestion parentQuestion,
            String questionText,
            String koreanTranslation
    ) {
        return DiaryQuestion.builder()
                .diary(diary)
                .parentQuestion(parentQuestion)
                .questionType(QuestionType.FOLLOW_UP)
                .questionOrder(parentQuestion.getQuestionOrder() + 1)
                .questionText(questionText)
                .generationType(QuestionGenerationType.AI)
                .koreanTranslation(koreanTranslation)
                .context(parentQuestion.getContext())
                .englishLevelSnapshot(parentQuestion.getEnglishLevelSnapshot())
                .interestSnapshot(parentQuestion.getInterestSnapshot())
                .build();
    }

    public static DiaryQuestion createDefaultFollowUpQuestion(
            Diary diary,
            DiaryQuestion parentQuestion,
            DefaultQuestion defaultQuestion
    ) {
        return DiaryQuestion.builder()
                .diary(diary)
                .parentQuestion(parentQuestion)
                .defaultQuestion(defaultQuestion)
                .questionType(QuestionType.FOLLOW_UP)
                .questionOrder(parentQuestion.getQuestionOrder() + 1)
                .questionText(defaultQuestion.getQuestionText())
                .generationType(QuestionGenerationType.DEFAULT)
                .koreanTranslation(defaultQuestion.getKoreanTranslation())
                .context(parentQuestion.getContext())
                .englishLevelSnapshot(parentQuestion.getEnglishLevelSnapshot())
                .interestSnapshot(parentQuestion.getInterestSnapshot())
                .build();
    }

    private static void validateParentQuestion(
            QuestionType questionType,
            DiaryQuestion parentQuestion
    ) {
        if (questionType == QuestionType.MAIN && parentQuestion != null) {
            throw new BaseException(ErrorCode.MAIN_QUESTION_CANNOT_HAVE_PARENT);
        }

        if (questionType == QuestionType.FOLLOW_UP
                && parentQuestion == null) {
            throw new BaseException(ErrorCode.FOLLOW_UP_QUESTION_REQUIRES_PARENT);
        }

        if (parentQuestion != null
                && parentQuestion.getQuestionType() != QuestionType.MAIN) {
            throw new BaseException(ErrorCode.FOLLOW_UP_PARENT_MUST_BE_MAIN_QUESTION);
        }
    }

    private static void validateKeyword(
            QuestionType questionType,
            String keyword
    ) {
        if (questionType == QuestionType.FOLLOW_UP && keyword != null) {
            throw new BaseException(ErrorCode.FOLLOW_UP_QUESTION_CANNOT_HAVE_KEYWORD);
        }
    }

    private static void validateDefaultQuestion(
            QuestionGenerationType generationType,
            DefaultQuestion defaultQuestion
    ) {
        if (generationType == QuestionGenerationType.DEFAULT && defaultQuestion == null) {
            throw new BaseException(ErrorCode.DEFAULT_GENERATION_REQUIRES_DEFAULT_QUESTION);
        }

        if (generationType == QuestionGenerationType.AI && defaultQuestion != null) {
            throw new BaseException(ErrorCode.AI_GENERATION_CANNOT_HAVE_DEFAULT_QUESTION);
        }
    }
}
