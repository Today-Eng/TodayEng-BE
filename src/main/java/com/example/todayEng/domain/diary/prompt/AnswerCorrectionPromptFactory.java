package com.example.todayEng.domain.diary.prompt;

import com.example.todayEng.domain.diary.dto.llm.AnswerCorrectionCommand;
import com.example.todayEng.domain.diary.entity.enums.QuestionType;
import org.springframework.stereotype.Component;

@Component
public class AnswerCorrectionPromptFactory {
    public String create(AnswerCorrectionCommand c) {
        String followUp = c.questionType() == QuestionType.MAIN
                ? "Also create exactly one natural follow-up question grounded only in the supplied context and answer."
                : "Do not create a follow-up question; followUpQuestion must be null.";
        return """
                You are an English reflection-writing coach.
                Correct the answer at the user's English level while preserving its original meaning.
                Never invent or add facts.
                Write correctionReason only in Korean.
                Keep correctedText and alternativeExpressions in English.
                %s
                English level: %s
                Question: %s
                Answer: %s
                Context (reference only; never infer missing facts): %s
                Return only JSON matching the supplied schema.
                """.formatted(followUp, c.englishLevel(), c.questionText(), c.originalText(),
                c.contextData() == null ? "null" : c.contextData().toString());
    }
}
