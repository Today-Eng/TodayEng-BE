package com.example.todayEng.domain.diary.dto.llm;

import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import com.fasterxml.jackson.databind.JsonNode;

public record AnswerCorrectionCommand(
        QuestionType questionType,
        String questionText,
        String originalText,
        EnglishLevel englishLevel,
        JsonNode contextData
) { }
