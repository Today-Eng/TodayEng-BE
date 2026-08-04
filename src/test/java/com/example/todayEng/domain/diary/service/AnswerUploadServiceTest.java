package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(persistenceService.createUploaded(3L, 1L, 2L, "key")).thenReturn(answer);

        var response = service.upload(3L, 1L, 2L, file);

        assertThat(response.status()).isEqualTo(TranscriptionStatus.UPLOADED);
        verify(asyncService).transcribe(3L, 1L, 2L, 9L);
    }

    @Test
    void deletesAnswerAndAudioWhenAsyncSubmissionIsRejected() {
        var file = new MockMultipartFile("audio", new byte[]{1});
        var valid = new AudioUploadValidator.ValidatedAudio(new byte[]{1});
        DiaryAnswer answer = mock(DiaryAnswer.class);
        when(answer.getId()).thenReturn(9L);
        when(validator.validate(file)).thenReturn(valid);
        when(storage.storeAnswer(1L, 2L, valid.bytes())).thenReturn("key");
        when(persistenceService.createUploaded(3L, 1L, 2L, "key")).thenReturn(answer);
        doThrow(new TaskRejectedException("queue full"))
                .when(asyncService).transcribe(3L, 1L, 2L, 9L);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.upload(3L, 1L, 2L, file))
                .isInstanceOf(TaskRejectedException.class);

        var inOrder = inOrder(persistenceService, storage);
        inOrder.verify(persistenceService).deleteUploaded(9L);
        inOrder.verify(storage).deleteQuietly("key");
    }
}
