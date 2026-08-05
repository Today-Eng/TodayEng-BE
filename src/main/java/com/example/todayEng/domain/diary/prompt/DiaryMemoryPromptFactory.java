package com.example.todayEng.domain.diary.prompt;

import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DiaryMemoryPromptFactory {

    private final ObjectMapper objectMapper;

    public String create(DiaryMemoryAnalysisCommand command) {
        try {
            return """
                    Analyze the completed diaries below and extract only useful memories
                    for generating a reflection question for the current diary.

                    Rules:
                    - Use only facts explicitly stated in the supplied diaries.
                    - Never identify unnamed people or infer sensitive traits.
                    - Never invent, merge, or embellish facts.
                    - Write every value in Korean.
                    - people, places, themes, and ongoingStories require evidence from
                      at least two different diaries.
                    - recentEmotions may use one or more diaries, but must describe only
                      explicitly expressed emotions.
                    - sourceDiaryIds must contain only diaryId values that directly support
                      the corresponding value.
                    - Return an empty array when a category has no meaningful item.
                    - Keep each category to at most five concise items.

                    currentDiaryId: %d
                    completedDiaries: %s
                    """.formatted(
                    command.currentDiaryId(),
                    objectMapper.writeValueAsString(command.diaries())
            );
        } catch (JsonProcessingException exception) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
