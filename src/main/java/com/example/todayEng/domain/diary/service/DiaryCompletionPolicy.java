package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.CorrectionStatus;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DiaryCompletionPolicy {
    private static final int EXPECTED_MAIN_COUNT = 3;
    private static final int EXPECTED_FOLLOW_UP_COUNT = 3;
    private static final int EXPECTED_TOTAL_COUNT = 6;

    public void validate(List<DiaryQuestion> questions, List<DiaryAnswer> answers) {
        long mains = questions.stream().filter(q -> q.getQuestionType() == QuestionType.MAIN).count();
        long followUps = questions.stream().filter(q -> q.getQuestionType() == QuestionType.FOLLOW_UP).count();
        boolean validOrders = questions.size() == EXPECTED_TOTAL_COUNT;
        for (int index = 0; validOrders && index < EXPECTED_TOTAL_COUNT; index++) {
            validOrders = questions.get(index).getQuestionOrder() == index + 1;
        }
        if (mains != EXPECTED_MAIN_COUNT || followUps != EXPECTED_FOLLOW_UP_COUNT || !validOrders) {
            throw new BaseException(ErrorCode.INVALID_REFLECTION_QUESTION_COMPOSITION);
        }
        if (answers.size() != EXPECTED_TOTAL_COUNT
                || answers.stream().anyMatch(a -> a.getOriginalText() == null || a.getOriginalText().isBlank())) {
            throw new BaseException(ErrorCode.INCOMPLETE_REFLECTION_ANSWERS);
        }
        // 현재 파이프라인은 교정 성공 후에만 다음 질문/완료 이벤트를 발행하므로 같은 정책을 적용한다.
        if (answers.stream().anyMatch(a -> a.getCorrectionStatus() != CorrectionStatus.SUCCEEDED)) {
            throw new BaseException(ErrorCode.REFLECTION_CORRECTION_NOT_FINISHED);
        }
    }
}
