package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.tts.QuestionTtsCommand;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.repository.DiaryQuestionRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionTtsPersistenceService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final DiaryQuestionRepository questionRepository;

    @Transactional
    public QuestionTtsCommand claim(
            Long userId,
            Long diaryId,
            Long questionId
    ) {
        int updated = questionRepository.claimTtsGeneration(
                userId,
                diaryId,
                questionId
        );
        if (updated != 1) {
            if (questionRepository
                    .findByIdAndDiaryIdAndDiaryUserId(
                            questionId,
                            diaryId,
                            userId
                    )
                    .isEmpty()) {
                throw new BaseException(ErrorCode.QUESTION_NOT_FOUND);
            }
            throw new BaseException(
                    ErrorCode.QUESTION_TTS_ALREADY_PROCESSING
            );
        }

        DiaryQuestion question = questionRepository
                .findByIdAndDiaryIdAndDiaryUserId(
                        questionId,
                        diaryId,
                        userId
                )
                .orElseThrow(() ->
                        new BaseException(ErrorCode.QUESTION_NOT_FOUND)
                );
        return new QuestionTtsCommand(
                userId,
                diaryId,
                questionId,
                question.getQuestionText()
        );
    }

    @Transactional
    public void complete(QuestionTtsCommand command, String audioKey) {
        getQuestion(command).completeTts(audioKey);
    }

    @Transactional
    public void fail(QuestionTtsCommand command, Throwable throwable) {
        getQuestion(command).failTts(sanitizeErrorMessage(throwable));
    }

    private DiaryQuestion getQuestion(QuestionTtsCommand command) {
        return questionRepository.findByIdAndDiaryIdAndDiaryUserId(
                        command.questionId(),
                        command.diaryId(),
                        command.userId()
                )
                .orElseThrow(() ->
                        new BaseException(ErrorCode.QUESTION_NOT_FOUND)
                );
    }

    private String sanitizeErrorMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getClass().getSimpleName();
        }
        message = message.replaceAll("[\\r\\n\\t]", " ").trim();
        return message.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
