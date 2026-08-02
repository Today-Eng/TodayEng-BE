package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.client.GoogleTtsClient;
import com.example.todayEng.domain.diary.dto.response.ReflectionSessionResponse;
import com.example.todayEng.domain.diary.dto.sse.DiarySsePayload;
import com.example.todayEng.domain.diary.dto.tts.QuestionTtsCommand;
import com.example.todayEng.domain.diary.sse.DiarySseEmitterManager;
import com.example.todayEng.domain.diary.storage.AudioFileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionTtsService {

    private final QuestionTtsPersistenceService persistenceService;
    private final GoogleTtsClient googleTtsClient;
    private final AudioFileStorage audioFileStorage;
    private final DiarySseEmitterManager emitterManager;

    public void generateFirstQuestion(
            Long userId,
            ReflectionSessionResponse response
    ) {
        if (response.questions().isEmpty()) {
            return;
        }

        ReflectionSessionResponse.Question firstQuestion =
                response.questions().get(0);
        generateQuestion(
                userId,
                response.diaryId(),
                firstQuestion.questionId(),
                firstQuestion.koreanTranslation()
        );
    }

    public void generateQuestion(
            Long userId,
            Long diaryId,
            Long questionId,
            String koreanTranslation
    ) {
        generateQuestion(userId, diaryId, questionId, koreanTranslation, false);
    }

    public void generateFollowUpQuestion(Long userId, Long diaryId, Long questionId, String koreanTranslation) {
        generateQuestion(userId, diaryId, questionId, koreanTranslation, true);
    }

    private void generateQuestion(Long userId, Long diaryId, Long questionId,
            String koreanTranslation, boolean followUp) {
        QuestionTtsCommand command;
        try {
            command = persistenceService.claim(userId, diaryId, questionId);
        } catch (RuntimeException exception) {
            log.warn(
                    "Unable to claim question TTS: userId={}, diaryId={}, questionId={}, error={}",
                    userId,
                    diaryId,
                    questionId,
                    exception.getClass().getSimpleName()
            );
            return;
        }

        String audioKey = null;
        try {
            byte[] audio = googleTtsClient.synthesize(command.questionText());
            audioKey = audioFileStorage.store(diaryId, questionId, audio);
            persistenceService.complete(command, audioKey);

            DiarySsePayload.QuestionReady payload = new DiarySsePayload.QuestionReady(
                            questionId,
                            command.questionText(),
                            koreanTranslation,
                            audioFileStorage.publicUrl(audioKey)
                    );
            if (followUp) emitterManager.sendFollowUpReady(userId, diaryId, payload);
            else emitterManager.sendQuestionReady(userId, diaryId, payload);
        } catch (RuntimeException exception) {
            if (audioKey != null) {
                audioFileStorage.deleteQuietly(audioKey);
            }
            markFailedQuietly(command, exception);
            log.error(
                    "Question TTS generation failed: userId={}, diaryId={}, questionId={}, error={}",
                    userId,
                    diaryId,
                    questionId,
                    exception.getClass().getSimpleName()
            );
        }
    }

    private void markFailedQuietly(
            QuestionTtsCommand command,
            RuntimeException originalException
    ) {
        try {
            persistenceService.fail(command, originalException);
        } catch (RuntimeException persistenceException) {
            log.error(
                    "Failed to persist TTS failure: questionId={}, error={}",
                    command.questionId(),
                    persistenceException.getClass().getSimpleName()
            );
        }
    }
}
