package com.example.todayEng.domain.diary.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import com.example.todayEng.domain.diary.client.GoogleTtsClient;
import com.example.todayEng.domain.diary.dto.response.ReflectionSessionResponse;
import com.example.todayEng.domain.diary.dto.sse.DiarySsePayload;
import com.example.todayEng.domain.diary.dto.tts.QuestionTtsCommand;
import com.example.todayEng.domain.diary.sse.DiarySseEmitterManager;
import com.example.todayEng.domain.diary.storage.AudioFileStorage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionTtsServiceTest {

    @Mock private QuestionTtsPersistenceService persistenceService;
    @Mock private GoogleTtsClient googleTtsClient;
    @Mock private AudioFileStorage audioFileStorage;
    @Mock private DiarySseEmitterManager emitterManager;
    @InjectMocks private QuestionTtsService service;

    @Test
    void generatesOnlyFirstQuestionAfterReflectionQuestionsAreCreated() {
        ReflectionSessionResponse.Question first = question(101L, 1);
        ReflectionSessionResponse.Question second = question(102L, 2);
        ReflectionSessionResponse response = new ReflectionSessionResponse(
                10L,
                List.of(first, second)
        );
        QuestionTtsCommand command = new QuestionTtsCommand(
                1L,
                10L,
                101L,
                first.questionText()
        );
        byte[] audio = new byte[]{1, 2, 3};
        given(persistenceService.claim(1L, 10L, 101L))
                .willReturn(command);
        given(googleTtsClient.synthesize(first.questionText()))
                .willReturn(audio);
        given(audioFileStorage.store(10L, 101L, audio))
                .willReturn("diaries/10/questions/101/audio.mp3");
        given(audioFileStorage.publicUrl(
                "diaries/10/questions/101/audio.mp3"
        )).willReturn(
                "/files/audio/diaries/10/questions/101/audio.mp3"
        );

        service.generateFirstQuestion(1L, response);

        verify(googleTtsClient).synthesize(first.questionText());
        verify(persistenceService).complete(
                command,
                "diaries/10/questions/101/audio.mp3"
        );
        verify(emitterManager).sendQuestionReady(
                1L,
                10L,
                new DiarySsePayload.QuestionReady(
                        101L,
                        first.questionText(),
                        first.koreanTranslation(),
                        "/files/audio/diaries/10/questions/101/audio.mp3"
                )
        );
        verify(persistenceService, never()).claim(1L, 10L, 102L);
    }

    @Test
    void marksQuestionFailedWhenGoogleTtsFails() {
        QuestionTtsCommand command = new QuestionTtsCommand(
                1L,
                10L,
                101L,
                "How was your day?"
        );
        RuntimeException failure = new RuntimeException("tts unavailable");
        given(persistenceService.claim(1L, 10L, 101L))
                .willReturn(command);
        given(googleTtsClient.synthesize(command.questionText()))
                .willThrow(failure);

        service.generateQuestion(1L, 10L, 101L, "오늘은 어땠나요?");

        verify(persistenceService).fail(command, failure);
        verify(audioFileStorage, never()).store(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(emitterManager, never()).sendQuestionReady(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void failedQuestionCanBeClaimedAgainForRetry() {
        QuestionTtsCommand command = new QuestionTtsCommand(
                1L,
                10L,
                101L,
                "How was your day?"
        );
        byte[] audio = new byte[]{1};
        given(persistenceService.claim(1L, 10L, 101L))
                .willReturn(command);
        given(googleTtsClient.synthesize(command.questionText()))
                .willReturn(audio);
        given(audioFileStorage.store(10L, 101L, audio))
                .willReturn("retry.mp3");
        given(audioFileStorage.publicUrl("retry.mp3"))
                .willReturn("/files/audio/retry.mp3");

        service.generateQuestion(1L, 10L, 101L, "오늘은 어땠나요?");

        verify(persistenceService).claim(1L, 10L, 101L);
        verify(persistenceService).complete(command, "retry.mp3");
    }

    @Test
    void notificationFailureDoesNotRevertCompletedTtsOrDeleteAudio() {
        QuestionTtsCommand command = new QuestionTtsCommand(1L, 10L, 101L, "Question");
        byte[] audio = new byte[]{1};
        given(persistenceService.claim(1L, 10L, 101L)).willReturn(command);
        given(googleTtsClient.synthesize("Question")).willReturn(audio);
        given(audioFileStorage.store(10L, 101L, audio)).willReturn("audio.mp3");
        given(audioFileStorage.publicUrl("audio.mp3")).willReturn("/audio.mp3");
        doThrow(new RuntimeException("disconnected")).when(emitterManager)
                .sendQuestionReady(eq(1L), eq(10L), any());

        service.generateQuestion(1L, 10L, 101L, "질문");

        verify(persistenceService).complete(command, "audio.mp3");
        verify(persistenceService, never()).fail(any(), any());
        verify(audioFileStorage, never()).deleteQuietly("audio.mp3");
    }

    private ReflectionSessionResponse.Question question(Long id, int order) {
        return new ReflectionSessionResponse.Question(
                id,
                order,
                "Question " + order,
                "질문 " + order,
                "keyword",
                100L
        );
    }
}
