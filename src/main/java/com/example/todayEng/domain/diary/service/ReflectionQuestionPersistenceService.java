package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionGenerationCommand;
import com.example.todayEng.domain.diary.dto.llm.ReflectionQuestionLlmResponse;
import com.example.todayEng.domain.diary.dto.response.ReflectionSessionResponse;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.entity.enums.ReflectionQuestionGenerationStatus;
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
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReflectionQuestionPersistenceService {

    private static final Duration CLAIM_STALE_AFTER = Duration.ofMinutes(10);

    private final DiaryRepository diaryRepository;
    private final DiaryContextRepository contextRepository;
    private final DiaryQuestionRepository questionRepository;
    private final UserInterestRepository userInterestRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

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

        if (!claimQuestionGeneration(diaryId, userId)) {
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
        if (contexts.isEmpty() && interests.isEmpty()) {
            claimedDiary.failQuestionGeneration();
            throw new BaseException(ErrorCode.DIARY_CONTEXT_NOT_FOUND);
        }

        return new ReflectionQuestionGenerationCommand(
                userId,
                diaryId,
                claimedDiary.getQuestionGenerationLeaseVersion(),
                claimedDiary.getUser().getNickname(),
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

        // 회수된 이전 요청이 질문을 중복 저장하지 않도록 저장 전에 소유권을 확정한다
        if (!finishIfOwned(command, ReflectionQuestionGenerationStatus.COMPLETED)) {
            throw new BaseException(ErrorCode.REFLECTION_QUESTION_CLAIM_LOST);
        }

        Diary diary = diaryRepository
                .findByIdAndUserId(command.diaryId(), command.userId())
                .orElseThrow(() -> new BaseException(ErrorCode.ACCESS_DENIED));

        List<Long> contextIds = generated.stream()
                .map(ReflectionQuestionLlmResponse.GeneratedQuestion::contextId)
                .filter(Objects::nonNull)
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
                        item.order() * 2 - 1,
                        item.questionText().trim(),
                        item.koreanTranslation().trim(),
                        item.keyword().trim(),
                        command.englishLevel(),
                        interestSnapshot.deepCopy()
                ))
                .toList();

        List<DiaryQuestion> saved = questionRepository.saveAllAndFlush(questions);

        return new ReflectionSessionResponse(
                command.diaryId(),
                saved.stream()
                        .sorted(Comparator.comparing(DiaryQuestion::getQuestionOrder))
                        .map(ReflectionSessionResponse.Question::from)
                        .toList()
        );
    }

    @Transactional
    public void markFailed(ReflectionQuestionGenerationCommand command) {
        if (!finishIfOwned(command, ReflectionQuestionGenerationStatus.FAILED)) {
            log.warn("Question generation lease lost: diaryId={}, expectedLeaseVersion={}",
                    command.diaryId(), command.leaseVersion());
        }
    }

    private boolean finishIfOwned(
            ReflectionQuestionGenerationCommand command,
            ReflectionQuestionGenerationStatus targetStatus
    ) {
        return diaryRepository.finishQuestionGenerationIfOwned(
                command.diaryId(),
                command.userId(),
                targetStatus,
                command.leaseVersion()
        ) == 1;
    }

    private List<ReflectionQuestionLlmResponse.GeneratedQuestion> validateAndSort(
            ReflectionQuestionGenerationCommand command,
            ReflectionQuestionLlmResponse response
    ) {
        if (response == null || response.questions() == null
                || response.questions().size() != 3) {
            throw new BaseException(ErrorCode.INVALID_LLM_RESPONSE);
        }

        Set<Integer> orders = response.questions().stream()
                .map(ReflectionQuestionLlmResponse.GeneratedQuestion::order)
                .collect(Collectors.toSet());
        if (!orders.equals(Set.of(1, 2, 3))) {
            throw new BaseException(ErrorCode.INVALID_LLM_RESPONSE);
        }

        validateContextComposition(command, response);

        for (ReflectionQuestionLlmResponse.GeneratedQuestion question
                : response.questions()) {
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

    /**
     * 컨텍스트가 부족할 때 같은 소재가 반복되지 않도록, 컨텍스트 기반 질문과 관심사 기반
     * 질문의 구성이 프롬프트에 요구한 정책과 일치하는지 검증한다.
     */
    private void validateContextComposition(
            ReflectionQuestionGenerationCommand command,
            ReflectionQuestionLlmResponse response
    ) {
        ReflectionQuestionGenerationCommand.QuestionPlan plan = command.plan();
        Set<Long> allowedContextIds = command.contexts().stream()
                .map(ReflectionQuestionGenerationCommand.ContextInput::contextId)
                .collect(Collectors.toSet());
        List<Long> usedContextIds = response.questions().stream()
                .map(ReflectionQuestionLlmResponse.GeneratedQuestion::contextId)
                .filter(Objects::nonNull)
                .toList();

        if (usedContextIds.size() != plan.contextQuestionCount()) {
            throw new BaseException(ErrorCode.INVALID_QUESTION_CONTEXT);
        }
        if (!allowedContextIds.containsAll(usedContextIds)) {
            throw new BaseException(ErrorCode.INVALID_QUESTION_CONTEXT);
        }
        if (plan.requireDistinctContexts()
                && Set.copyOf(usedContextIds).size() != usedContextIds.size()) {
            throw new BaseException(ErrorCode.INVALID_QUESTION_CONTEXT);
        }
    }

    private boolean claimQuestionGeneration(Long diaryId, Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (diaryRepository.claimQuestionGeneration(diaryId, userId, now) == 1) {
            return true;
        }
        // 이전 시도가 GENERATING에서 죽어 고착된 경우, 일정 시간이 지났으면 재시도로 복구
        return diaryRepository.reclaimStaleQuestionGeneration(
                diaryId, userId, now, now.minus(CLAIM_STALE_AFTER)) == 1;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
