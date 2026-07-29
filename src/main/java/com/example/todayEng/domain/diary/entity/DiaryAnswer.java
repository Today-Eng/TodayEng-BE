package com.example.todayEng.domain.diary.entity;

import com.example.todayEng.domain.diary.entity.enums.CorrectionStatus;
import com.example.todayEng.global.common.BaseTimeEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "diary_answer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "question_id",
            nullable = false,
            unique = true
    )
    private DiaryQuestion question;

    @Column(name = "original_text", nullable = false, columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "corrected_text", columnDefinition = "TEXT")
    private String correctedText;

    @Column(name = "correction_reason", columnDefinition = "TEXT")
    private String correctionReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alternative_expression", columnDefinition = "json")
    private JsonNode alternativeExpression;

    @Enumerated(EnumType.STRING)
    @Column(name = "correction_status", nullable = false, length = 20)
    private CorrectionStatus correctionStatus;

    @Builder(access = AccessLevel.PRIVATE)
    private DiaryAnswer(
            DiaryQuestion question,
            String originalText
    ) {
        this.question = question;
        this.originalText = originalText;
        this.correctionStatus = CorrectionStatus.PENDING;
    }

    public static DiaryAnswer create(
            DiaryQuestion question,
            String originalText
    ) {
        return DiaryAnswer.builder()
                .question(question)
                .originalText(originalText)
                .build();
    }

    public void updateOriginalText(String originalText) {
        this.originalText = originalText;
        this.correctedText = null;
        this.correctionReason = null;
        this.alternativeExpression = null;
        this.correctionStatus = CorrectionStatus.PENDING;
    }

    public void startCorrection() {
        this.correctionStatus = CorrectionStatus.PENDING;
    }

    public void completeCorrection(
            String correctedText,
            String correctionReason,
            JsonNode alternativeExpression
    ) {
        this.correctedText = correctedText;
        this.correctionReason = correctionReason;
        this.alternativeExpression = alternativeExpression;
        this.correctionStatus = CorrectionStatus.SUCCEEDED;
    }

    public void failCorrection() {
        this.correctionStatus = CorrectionStatus.FAILED;
    }
}