package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.entity.enums.TtsStatus;
import com.example.todayEng.domain.diary.entity.enums.TranscriptionStatus;
import com.example.todayEng.domain.diary.repository.DiaryAnswerRepository;
import com.example.todayEng.domain.diary.repository.DiaryQuestionRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnswerPersistenceService {
    private final DiaryQuestionRepository questionRepository;
    private final DiaryAnswerRepository answerRepository;

    @Transactional(readOnly = true)
    public void validateAnswerable(Long userId, Long diaryId, Long questionId) {
        DiaryQuestion question = ownedQuestion(userId, diaryId, questionId);
        if (question.getDiary().getStatus() != DiaryStatus.IN_PROGRESS
                || question.getTtsStatus() != TtsStatus.SUCCEEDED) {
            throw new BaseException(ErrorCode.QUESTION_NOT_ANSWERABLE);
        }
    }

    @Transactional
    public UploadPreparation prepareUploaded(Long userId, Long diaryId, Long questionId, String key) {
        validateAnswerable(userId, diaryId, questionId);
        var existing = answerRepository.findByQuestionIdForUpdate(questionId);
        if (existing.isPresent()) {
            DiaryAnswer answer = existing.get();
            if (answer.getTranscriptionStatus() != TranscriptionStatus.FAILED) {
                throw new BaseException(ErrorCode.ANSWER_ALREADY_EXISTS);
            }
            String previousAudioKey = answer.getAudioKey();
            answer.retryTranscription(key);
            return new UploadPreparation(answer, previousAudioKey, true);
        }
        try {
            DiaryAnswer answer = answerRepository.saveAndFlush(DiaryAnswer.createUploaded(
                    ownedQuestion(userId, diaryId, questionId), key));
            return new UploadPreparation(answer, null, false);
        } catch (DataIntegrityViolationException exception) {
            throw new BaseException(ErrorCode.ANSWER_ALREADY_EXISTS);
        }
    }

    @Transactional
    public void rollbackUploaded(UploadPreparation preparation) {
        if (preparation.retry()) {
            answerRepository.findById(preparation.answer().getId())
                    .ifPresent(answer -> answer.restoreFailedTranscription(preparation.previousAudioKey()));
        } else {
            answerRepository.deleteById(preparation.answer().getId());
            answerRepository.flush();
        }
    }

    @Transactional
    public void deleteUploaded(Long answerId) {
        answerRepository.deleteById(answerId);
        answerRepository.flush();
    }

    @Transactional
    public boolean claim(Long answerId) { return answerRepository.claimTranscription(answerId) == 1; }

    @Transactional(readOnly = true)
    public DiaryAnswer getOwned(Long userId, Long diaryId, Long questionId, Long answerId) {
        return answerRepository.findByIdAndQuestionIdAndQuestionDiaryIdAndQuestionDiaryUserId(
                answerId, questionId, diaryId, userId).orElseThrow(() -> new BaseException(ErrorCode.ANSWER_NOT_FOUND));
    }

    @Transactional
    public void complete(Long answerId, String text) {
        answerRepository.findById(answerId).orElseThrow(() -> new BaseException(ErrorCode.ANSWER_NOT_FOUND))
                .completeTranscription(text);
    }

    @Transactional
    public void fail(Long answerId, String message) {
        answerRepository.findById(answerId).ifPresent(answer -> answer.failTranscription(truncate(message)));
    }

    private DiaryQuestion ownedQuestion(Long userId, Long diaryId, Long questionId) {
        return questionRepository.findByIdAndDiaryIdAndDiaryUserId(questionId, diaryId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.QUESTION_NOT_FOUND));
    }

    private String truncate(String message) {
        String safe = message == null ? "STT processing failed" : message;
        return safe.substring(0, Math.min(500, safe.length()));
    }

    public record UploadPreparation(DiaryAnswer answer, String previousAudioKey, boolean retry) { }
}
