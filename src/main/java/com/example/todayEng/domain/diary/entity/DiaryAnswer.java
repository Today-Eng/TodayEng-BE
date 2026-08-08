package com.example.todayEng.domain.diary.entity;

import com.example.todayEng.domain.diary.entity.enums.CorrectionStatus;
import com.example.todayEng.domain.diary.entity.enums.TranscriptionStatus;
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

    @Column(name = "original_text", columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "audio_key", length = 500)
    private String audioKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "transcription_status", nullable = false, length = 20)
    private TranscriptionStatus transcriptionStatus;

    @Column(name = "transcription_error", length = 500)
    private String transcriptionError;

    @Column(name = "correction_error", length = 500)
    private String correctionError;

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
            String originalText,
            String audioKey,
            TranscriptionStatus transcriptionStatus
    ) {
        this.question = question;
        this.originalText = originalText;
        this.audioKey = audioKey;
        this.transcriptionStatus = transcriptionStatus;
        this.correctionStatus = CorrectionStatus.PENDING;
    }

    public static DiaryAnswer create(
            DiaryQuestion question,
            String originalText
    ) {
        return DiaryAnswer.builder()
                .question(question)
                .originalText(originalText)
                .transcriptionStatus(TranscriptionStatus.SUCCEEDED)
                .build();
    }

    public static DiaryAnswer createUploaded(DiaryQuestion question, String audioKey) {
        return DiaryAnswer.builder()
                .question(question)
                .audioKey(audioKey)
                .transcriptionStatus(TranscriptionStatus.UPLOADED)
                .build();
    }

    public void completeTranscription(String text) {
        this.originalText = text;
        this.audioKey = null;
        this.transcriptionStatus = TranscriptionStatus.SUCCEEDED;
        this.transcriptionError = null;
    }

    public void failTranscription(String error) {
        this.transcriptionStatus = TranscriptionStatus.FAILED;
        this.transcriptionError = error;
    }

    public void retryTranscription(String audioKey) {
        this.originalText = null;
        this.audioKey = audioKey;
        this.transcriptionStatus = TranscriptionStatus.UPLOADED;
        this.transcriptionError = null;
        this.correctedText = null;
        this.correctionReason = null;
        this.alternativeExpression = null;
        this.correctionStatus = CorrectionStatus.PENDING;
        this.correctionError = null;
    }

    public void restoreFailedTranscription(String audioKey) {
        this.audioKey = audioKey;
        this.transcriptionStatus = TranscriptionStatus.FAILED;
    }

    public void updateOriginalText(String originalText) {
        this.originalText = originalText;
        this.correctedText = null;
        this.correctionReason = null;
        this.alternativeExpression = null;
        this.correctionStatus = CorrectionStatus.PENDING;
        this.correctionError = null;
    }

    public void startCorrection() {
        this.correctionStatus = CorrectionStatus.PROCESSING;
        this.correctionError = null;
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
        this.correctionError = null;
    }

    public void failCorrection() {
        this.correctionStatus = CorrectionStatus.FAILED;
    }

    public void failCorrection(String error) {
        if (this.correctionStatus != CorrectionStatus.SUCCEEDED) {
            this.correctionStatus = CorrectionStatus.FAILED;
            this.correctionError = error;
        }
    }
}
