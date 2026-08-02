package com.example.todayEng.domain.diary.service;

import static org.mockito.Mockito.*;

import com.example.todayEng.domain.diary.client.GoogleSpeechToTextClient;
import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.sse.DiarySseEmitterManager;
import com.example.todayEng.domain.diary.storage.AudioFileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpeechTranscriptionAsyncServiceTest {
    @Mock AnswerPersistenceService persistenceService;
    @Mock AudioFileStorage storage;
    @Mock GoogleSpeechToTextClient client;
    @Mock DiarySseEmitterManager emitterManager;
    @Mock AnswerCorrectionPipelineService correctionPipelineService;
    @InjectMocks SpeechTranscriptionAsyncService service;

    @Test
    void savesTranscriptAndSendsSse() {
        DiaryAnswer answer = mock(DiaryAnswer.class);
        when(persistenceService.claim(4L)).thenReturn(true);
        when(persistenceService.getOwned(1L, 2L, 3L, 4L)).thenReturn(answer);
        when(answer.getAudioKey()).thenReturn("key");
        when(storage.read("key")).thenReturn(new byte[]{1});
        when(client.transcribe(any())).thenReturn("hello");

        service.transcribe(1L, 2L, 3L, 4L);

        verify(persistenceService).complete(4L, "hello");
        verify(storage).deleteQuietly("key");
        verify(emitterManager).sendAnswerTranscribed(eq(1L), eq(2L), any());
        verify(correctionPipelineService).process(1L, 2L, 3L, 4L);
    }

    @Test
    void marksFailedAndDoesNotPropagateClientFailure() {
        DiaryAnswer answer = mock(DiaryAnswer.class);
        when(persistenceService.claim(4L)).thenReturn(true);
        when(persistenceService.getOwned(1L, 2L, 3L, 4L)).thenReturn(answer);
        when(answer.getAudioKey()).thenReturn("key");
        when(storage.read("key")).thenReturn(new byte[]{1});
        when(client.transcribe(any())).thenThrow(new RuntimeException("failed"));

        service.transcribe(1L, 2L, 3L, 4L);

        verify(persistenceService).fail(4L, "failed");
        verify(storage, never()).deleteQuietly("key");
        verifyNoInteractions(emitterManager);
    }
}
