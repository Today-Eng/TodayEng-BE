package com.example.todayEng.domain.diary.dto.llm;

import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record ReflectionQuestionGenerationCommand(
        Long userId,
        Long diaryId,
        long leaseVersion,
        String nickname,
        EnglishLevel englishLevel,
        List<String> interests,
        List<ContextInput> contexts
) {

    public ReflectionQuestionGenerationCommand {
        nickname = nickname == null ? "" : nickname;
    }

    public record ContextInput(
            Long contextId,
            DiaryContextType contextType,
            JsonNode contextData
    ) {
    }
}
