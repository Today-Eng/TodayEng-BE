package com.example.todayEng.domain.diary.client;

import com.example.todayEng.domain.diary.dto.llm.AnswerCorrectionCommand;
import com.example.todayEng.domain.diary.dto.llm.AnswerCorrectionLlmResponse;

public interface AnswerCorrectionLlmClient {
    AnswerCorrectionLlmResponse correct(AnswerCorrectionCommand command);
}
