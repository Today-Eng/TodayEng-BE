package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.llm.*;
import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.CorrectionStatus;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.domain.diary.repository.DiaryAnswerRepository;
import com.example.todayEng.domain.diary.repository.DiaryQuestionRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnswerCorrectionPersistenceService {
    private final DiaryAnswerRepository answerRepository;
    private final DiaryQuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AnswerCorrectionWork claim(Long userId, Long diaryId, Long questionId, Long answerId) {
        if (answerRepository.claimCorrection(answerId) != 1) {
            owned(userId, diaryId, questionId, answerId);
            throw new BaseException(ErrorCode.ANSWER_CORRECTION_ALREADY_PROCESSING);
        }
        DiaryAnswer answer = owned(userId, diaryId, questionId, answerId);
        DiaryQuestion question = answer.getQuestion();
        var level = question.getEnglishLevelSnapshot() != null
                ? question.getEnglishLevelSnapshot() : question.getDiary().getUser().getEnglishLevel();
        var context = question.getContext() == null ? null : question.getContext().getContextData();
        return new AnswerCorrectionWork(userId, diaryId, questionId, answerId,
                new AnswerCorrectionCommand(question.getQuestionType(), question.getQuestionText(),
                        answer.getOriginalText(), level, context));
    }

    @Transactional
    public AnswerCorrectionResult complete(AnswerCorrectionWork work, AnswerCorrectionLlmResponse response) {
        DiaryAnswer answer = owned(work.userId(), work.diaryId(), work.questionId(), work.answerId());
        if (answer.getCorrectionStatus() != CorrectionStatus.PROCESSING) {
            throw new BaseException(ErrorCode.ANSWER_CORRECTION_ALREADY_PROCESSING);
        }
        DiaryQuestion question = answer.getQuestion();
        AnswerCorrectionResult.NextQuestion next = null;
        boolean ready = false;
        if (question.getQuestionType() == QuestionType.MAIN) {
            if (questionRepository.existsByParentQuestionId(question.getId())) {
                throw new BaseException(ErrorCode.FOLLOW_UP_QUESTION_ALREADY_EXISTS);
            }
            var follow = response.followUpQuestion();
            if (follow == null || follow.questionText() == null || follow.questionText().isBlank()
                    || follow.koreanTranslation() == null || follow.koreanTranslation().isBlank()) {
                throw new BaseException(ErrorCode.INVALID_LLM_RESPONSE);
            }
            try {
                DiaryQuestion saved = questionRepository.saveAndFlush(DiaryQuestion.createFollowUpQuestion(
                        question.getDiary(), question, follow.questionText(),
                        follow.koreanTranslation()));
                next = new AnswerCorrectionResult.NextQuestion(saved.getId(), saved.getKoreanTranslation(), true);
            } catch (DataIntegrityViolationException exception) {
                throw new BaseException(ErrorCode.FOLLOW_UP_QUESTION_ALREADY_EXISTS);
            }
        } else {
            var nextMain = questionRepository
                    .findFirstByDiaryIdAndQuestionTypeAndQuestionOrderGreaterThanOrderByQuestionOrder(
                            work.diaryId(), QuestionType.MAIN, question.getQuestionOrder());
            if (nextMain.isPresent()) {
                DiaryQuestion q = nextMain.get();
                next = new AnswerCorrectionResult.NextQuestion(q.getId(), q.getKoreanTranslation(), false);
            } else ready = true;
        }
        answer.completeCorrection(response.correctedText(), response.correctionReason(),
                objectMapper.valueToTree(response.alternativeExpressions()));
        return new AnswerCorrectionResult(question.getQuestionType(), question.getId(), answer.getId(),
                response.correctedText(), response.correctionReason(), next, ready);
    }

    @Transactional
    public void fail(Long answerId, Throwable throwable) {
        answerRepository.findById(answerId).ifPresent(answer -> answer.failCorrection(sanitize(throwable)));
    }

    private DiaryAnswer owned(Long userId, Long diaryId, Long questionId, Long answerId) {
        return answerRepository.findByIdAndQuestionIdAndQuestionDiaryIdAndQuestionDiaryUserId(
                answerId, questionId, diaryId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.ANSWER_NOT_FOUND));
    }

    private String sanitize(Throwable throwable) {
        String value = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
        value = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return value.substring(0, Math.min(500, value.length()));
    }
}
