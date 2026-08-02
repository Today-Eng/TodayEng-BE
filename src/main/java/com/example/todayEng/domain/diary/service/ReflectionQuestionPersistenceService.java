package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand;
import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionLlmResponse;
import com.example.todayEng.domain.diary.dto.response.ReflectionSessionResponse;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.DiaryContextRepository;
import com.example.todayEng.domain.diary.repository.DiaryQuestionRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.UserInterest;
import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import com.example.todayEng.domain.user.repository.UserInterestRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReflectionQuestionPersistenceService {

    private final DiaryRepository diaryRepository;
    private final DiaryContextRepository contextRepository;
    private final DiaryQuestionRepository questionRepository;
    private final UserInterestRepository userInterestRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ReflectionQuestionGenerationCommand prepare(
            Long userId,
            Long diaryId
    ) {
        Diary diary = diaryRepository.findByIdAndUserId(diaryId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.ACCESS_DENIED));

        if (diary.getStatus() != DiaryStatus.IN_PROGRESS) {
            throw new BaseException(ErrorCode.DIARY_NOT_IN_PROGRESS);
        }

        if (diaryRepository.claimQuestionGeneration(diaryId, userId) != 1) {
            throw new BaseException(
                    ErrorCode.REFLECTION_QUESTIONS_ALREADY_GENERATED
            );
        }

        Diary claimedDiary = diaryRepository.findByIdAndUserId(diaryId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.ACCESS_DENIED));
        EnglishLevel englishLevel = claimedDiary.getUser().getEnglishLevel();
        if (englishLevel == null) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<DiaryContext> contexts = contextRepository
                .findAllByDiaryIdAndSuccessTrueOrderById(diaryId);
        if (contexts.isEmpty()) {
            claimedDiary.failQuestionGeneration();
            throw new BaseException(ErrorCode.DIARY_CONTEXT_NOT_FOUND);
        }
        if (contexts.stream().anyMatch(context -> context.getContextData() == null)) {
            claimedDiary.failQuestionGeneration();
            throw new BaseException(ErrorCode.DIARY_CONTEXT_NOT_FOUND);
        }

        List<String> interests = userInterestRepository
                .findAllByUserIdOrderByInterestTagId(userId)
                .stream()
                .map(UserInterest::getInterestTag)
                .map(tag -> tag.getTagName().name())
                .toList();

        return new ReflectionQuestionGenerationCommand(
                userId,
                diaryId,
                englishLevel,
                List.copyOf(interests),
                contexts.stream()
                        .map(context -> new ReflectionQuestionGenerationCommand.ContextInput(
                                context.getId(),
                                context.getContextType(),
                                context.getContextData().deepCopy()
                        ))
                        .toList()
        );
    }

    @Transactional
    public ReflectionSessionResponse saveQuestions(
            ReflectionQuestionGenerationCommand command,
            ReflectionQuestionLlmResponse llmResponse
    ) {
        List<ReflectionQuestionLlmResponse.GeneratedQuestion> generated =
                validateAndSort(command, llmResponse);
        Diary diary = diaryRepository
                .findByIdAndUserId(command.diaryId(), command.userId())
                .orElseThrow(() -> new BaseException(ErrorCode.ACCESS_DENIED));

        List<Long> contextIds = generated.stream()
                .map(ReflectionQuestionLlmResponse.GeneratedQuestion::contextId)
                .distinct()
                .toList();
        Map<Long, DiaryContext> contexts = contextRepository
                .findAllByDiaryIdAndIdIn(command.diaryId(), contextIds)
                .stream()
                .collect(Collectors.toMap(DiaryContext::getId, Function.identity()));
        if (contexts.size() != contextIds.size()) {
            throw new BaseException(ErrorCode.INVALID_QUESTION_CONTEXT);
        }

        JsonNode interestSnapshot = objectMapper.valueToTree(command.interests());
        List<DiaryQuestion> questions = generated.stream()
                .map(item -> DiaryQuestion.createGeneratedMainQuestion(
                        diary,
                        contexts.get(item.contextId()),
                        item.order(),
                        item.questionText().trim(),
                        item.koreanTranslation().trim(),
                        item.keyword().trim(),
                        command.englishLevel(),
                        interestSnapshot.deepCopy()
                ))
                .toList();

        List<DiaryQuestion> saved = questionRepository.saveAllAndFlush(questions);
        diary.completeQuestionGeneration();

        return new ReflectionSessionResponse(
                command.diaryId(),
                saved.stream()
                        .sorted(Comparator.comparing(DiaryQuestion::getQuestionOrder))
                        .map(ReflectionSessionResponse.Question::from)
                        .toList()
        );
    }

    @Transactional
    public void markFailed(Long userId, Long diaryId) {
        diaryRepository.findByIdAndUserId(diaryId, userId)
                .ifPresent(Diary::failQuestionGeneration);
    }

    private List<ReflectionQuestionLlmResponse.GeneratedQuestion> validateAndSort(
            ReflectionQuestionGenerationCommand command,
            ReflectionQuestionLlmResponse response
    ) {
        if (response == null || response.questions() == null
                || response.questions().size() != 3) {
            throw new BaseException(ErrorCode.INVALID_LLM_RESPONSE);
        }

        Set<Long> allowedContextIds = command.contexts().stream()
                .map(ReflectionQuestionGenerationCommand.ContextInput::contextId)
                .collect(Collectors.toSet());
        Set<Integer> orders = response.questions().stream()
                .map(ReflectionQuestionLlmResponse.GeneratedQuestion::order)
                .collect(Collectors.toSet());
        if (!orders.equals(Set.of(1, 2, 3))) {
            throw new BaseException(ErrorCode.INVALID_LLM_RESPONSE);
        }

        for (ReflectionQuestionLlmResponse.GeneratedQuestion question
                : response.questions()) {
            if (!allowedContextIds.contains(question.contextId())) {
                throw new BaseException(ErrorCode.INVALID_QUESTION_CONTEXT);
            }
            if (isBlank(question.questionText())
                    || isBlank(question.koreanTranslation())
                    || isBlank(question.keyword())
                    || question.keyword().length() > 50) {
                throw new BaseException(ErrorCode.INVALID_LLM_RESPONSE);
            }
        }

        return response.questions().stream()
                .sorted(Comparator.comparing(
                        ReflectionQuestionLlmResponse.GeneratedQuestion::order
                ))
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
