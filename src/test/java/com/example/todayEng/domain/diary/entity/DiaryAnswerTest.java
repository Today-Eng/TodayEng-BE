package com.example.todayEng.domain.diary.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.entity.enums.TranscriptionStatus;
import org.junit.jupiter.api.Test;

class DiaryAnswerTest {

    @Test
    void restoresPreviousTranscriptionErrorWhenRetrySubmissionIsRolledBack() {
        DiaryAnswer answer = DiaryAnswer.createUploaded(null, "old-audio-key");
        answer.failTranscription("original STT failure");

        answer.retryTranscription("new-audio-key");
        answer.restoreFailedTranscription("old-audio-key", "original STT failure");

        assertThat(answer.getTranscriptionStatus()).isEqualTo(TranscriptionStatus.FAILED);
        assertThat(answer.getAudioKey()).isEqualTo("old-audio-key");
        assertThat(answer.getTranscriptionError()).isEqualTo("original STT failure");
    }
}
