package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.entity.enums.TranscriptionStatus;
import com.example.todayEng.domain.diary.storage.AudioFileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AnswerUploadServiceTest {
    @Mock AudioUploadValidator validator;
    @Mock AnswerPersistenceService persistenceService;
    @Mock AudioFileStorage storage;
    @Mock SpeechTranscriptionAsyncService asyncService;
    @InjectMocks AnswerUploadService service;

    @Test
    void storesCreatesAndStartsAsyncTranscription() {
        var file = new MockMultipartFile("audio", new byte[]{1});
        var valid = new AudioUploadValidator.ValidatedAudio(new byte[]{1});
        DiaryAnswer answer = mock(DiaryAnswer.class);
        when(answer.getId()).thenReturn(9L);
        when(answer.getTranscriptionStatus()).thenReturn(TranscriptionStatus.UPLOADED);
        when(validator.validate(file)).thenReturn(valid);
        when(storage.storeAnswer(1L, 2L, valid.bytes())).thenReturn("key");
        when(persistenceService.prepareUploaded(3L, 1L, 2L, "key"))
                .thenReturn(new AnswerPersistenceService.UploadPreparation(answer, null, null, false));

        var response = service.upload(3L, 1L, 2L, file);

        assertThat(response.status()).isEqualTo(TranscriptionStatus.UPLOADED);
        verify(asyncService).transcribe(3L, 1L, 2L, 9L);
    }

    @Test
    void replacesFailedAnswerAudioAndStartsTranscriptionAgain() {
        var file = new MockMultipartFile("audio", new byte[]{2});
        var valid = new AudioUploadValidator.ValidatedAudio(new byte[]{2});
        DiaryAnswer answer = mock(DiaryAnswer.class);
        when(answer.getId()).thenReturn(9L);
        when(answer.getTranscriptionStatus()).thenReturn(TranscriptionStatus.UPLOADED);
        when(validator.validate(file)).thenReturn(valid);
        when(storage.storeAnswer(1L, 2L, valid.bytes())).thenReturn("new-key");
        var preparation = new AnswerPersistenceService.UploadPreparation(
                answer, "failed-key", "previous STT error", true);
        when(persistenceService.prepareUploaded(3L, 1L, 2L, "new-key")).thenReturn(preparation);

        var response = service.upload(3L, 1L, 2L, file);

        assertThat(response.answerId()).isEqualTo(9L);
        verify(asyncService).transcribe(3L, 1L, 2L, 9L);
        verify(storage).deleteQuietly("failed-key");
    }

    @Test
    void deletesAnswerAndAudioWhenAsyncSubmissionIsRejected() {
        var file = new MockMultipartFile("audio", new byte[]{1});
        var valid = new AudioUploadValidator.ValidatedAudio(new byte[]{1});
        DiaryAnswer answer = mock(DiaryAnswer.class);
        when(answer.getId()).thenReturn(9L);
        when(validator.validate(file)).thenReturn(valid);
        when(storage.storeAnswer(1L, 2L, valid.bytes())).thenReturn("key");
        var preparation = new AnswerPersistenceService.UploadPreparation(answer, null, null, false);
        when(persistenceService.prepareUploaded(3L, 1L, 2L, "key")).thenReturn(preparation);
        doThrow(new TaskRejectedException("queue full"))
                .when(asyncService).transcribe(3L, 1L, 2L, 9L);

        assertThatThrownBy(() -> service.upload(3L, 1L, 2L, file))
                .isInstanceOf(TaskRejectedException.class);

        var inOrder = inOrder(persistenceService, storage);
        inOrder.verify(persistenceService).rollbackUploaded(preparation);
        inOrder.verify(storage).deleteQuietly("key");
    }

    @Test
    void preservesSubmissionFailureAndAudioWhenDatabaseCleanupFails() {
        var file = new MockMultipartFile("audio", new byte[]{1});
        var valid = new AudioUploadValidator.ValidatedAudio(new byte[]{1});
        DiaryAnswer answer = mock(DiaryAnswer.class);
        TaskRejectedException submissionFailure = new TaskRejectedException("queue full");
        RuntimeException cleanupFailure = new RuntimeException("database unavailable");
        when(answer.getId()).thenReturn(9L);
        when(validator.validate(file)).thenReturn(valid);
        when(storage.storeAnswer(1L, 2L, valid.bytes())).thenReturn("key");
        var preparation = new AnswerPersistenceService.UploadPreparation(answer, null, null, false);
        when(persistenceService.prepareUploaded(3L, 1L, 2L, "key")).thenReturn(preparation);
        doThrow(submissionFailure).when(asyncService).transcribe(3L, 1L, 2L, 9L);
        doThrow(cleanupFailure).when(persistenceService).rollbackUploaded(preparation);

        assertThatThrownBy(() -> service.upload(3L, 1L, 2L, file))
                .isSameAs(submissionFailure)
                .satisfies(exception -> assertThat(exception.getSuppressed())
                        .containsExactly(cleanupFailure));

        verify(storage, never()).deleteQuietly("key");
    }
}
