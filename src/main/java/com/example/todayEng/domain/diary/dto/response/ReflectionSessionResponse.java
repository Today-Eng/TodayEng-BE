package com.example.todayEng.domain.diary.dto.response;

import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import java.util.List;

public record ReflectionSessionResponse(
        Long diaryId,
        List<Question> questions
) {

    public record Question(
            Long questionId,
            Integer order,
            String questionText,
            String koreanTranslation,
            String keyword,
            Long contextId
    ) {

        public static Question from(DiaryQuestion question) {
            return new Question(
                    question.getId(),
                    question.getQuestionOrder(),
                    question.getQuestionText(),
                    question.getKoreanTranslation(),
                    question.getKeyword(),
                    question.getContext().getId()
            );
        }
    }
}
