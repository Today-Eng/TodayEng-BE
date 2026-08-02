package com.example.todayEng.domain.diary.prompt;

import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReflectionQuestionPromptFactory {

    private final ObjectMapper objectMapper;

    public String create(ReflectionQuestionGenerationCommand command) {
        try {
            String contexts = objectMapper.writeValueAsString(command.contexts());
            String interests = objectMapper.writeValueAsString(command.interests());

            if (command.contexts().isEmpty()) {
                return """
                        You create exactly three personalized English reflection questions.

                        There is no diary context for this day. Use the user's interests only as broad conversation topics.
                        Rules:
                        - Never claim or imply that the user did an activity related to an interest.
                        - Ask open questions such as whether an interest played any role in the day, or invite a general reflection connected to it.
                        - Never invent, infer, or embellish facts.
                        - Every question must return contextId as null.
                        - Adjust English sentence difficulty to the supplied englishLevel.
                        - Make the three questions meaningfully different.
                        - Return question orders 1, 2, and 3 exactly once.
                        - Korean translation must faithfully translate the English question.
                        - keyword must be a short English phrase based on one supplied interest.

                        englishLevel: %s
                        interests: %s
                        contexts: []
                        """.formatted(command.englishLevel(), interests);
            }

            return """
                    You create exactly three personalized English reflection questions.

                    Rules:
                    - Use only facts explicitly present in the provided contexts.
                    - Never invent, infer, or embellish facts that are not present.
                    - Each question must be grounded in exactly one context and must return that contextId.
                    - Interests are only ranking signals for choosing among contexts. Interests are not facts and must not appear as events in questions unless a context states them.
                    - Adjust English sentence difficulty to the supplied englishLevel.
                    - Make the three questions meaningfully different.
                    - Return question orders 1, 2, and 3 exactly once.
                    - Korean translation must faithfully translate the English question.
                    - keyword must be a short English phrase grounded in the selected context.

                    englishLevel: %s
                    interests: %s
                    contexts: %s
                    """.formatted(
                    command.englishLevel(),
                    interests,
                    contexts
            );
        } catch (JsonProcessingException exception) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
