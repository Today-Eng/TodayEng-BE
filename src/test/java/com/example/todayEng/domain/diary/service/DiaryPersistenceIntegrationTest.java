package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisResponse;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisResponse.MemoryItem;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.repository.DiaryContextRepository;
import com.example.todayEng.domain.diary.repository.DiaryContextSourceRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;

@DataJpaTest
@Import({
        DiaryContextPersistenceService.class,
        DiaryMemoryPersistenceService.class,
        DiaryPersistenceIntegrationTest.Config.class
})
class DiaryPersistenceIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired DiaryRepository diaryRepository;
    @Autowired DiaryContextRepository contextRepository;
    @Autowired DiaryContextSourceRepository sourceRepository;
    @Autowired DiaryContextPersistenceService contextPersistenceService;
    @Autowired DiaryMemoryPersistenceService memoryPersistenceService;
    @Autowired ObjectMapper objectMapper;
    @Autowired EntityManager entityManager;

    @Test
    void savesFirstMemoPhotoAndWeatherContextsWithManagedDiary() {
        User user = userRepository.save(User.create());
        Diary diary = saveDiary(user, LocalDate.of(2026, 8, 6), false);
        Long userId = diary.getUser().getId();
        Long diaryId = diary.getId();
        entityManager.flush();
        entityManager.clear();

        contextPersistenceService.saveMemo(userId, diaryId,
                String.valueOf('m'), objectMapper.createObjectNode());
        contextPersistenceService.saveSuccess(userId, diaryId,
                DiaryContextType.PHOTO, objectMapper.createObjectNode());
        contextPersistenceService.saveSuccess(userId, diaryId,
                DiaryContextType.WEATHER, objectMapper.createObjectNode());
        entityManager.flush();
        entityManager.clear();

        assertThat(contextRepository.findAllByDiaryIdAndSuccessTrueOrderById(diaryId))
                .extracting(DiaryContext::getContextType)
                .containsExactlyInAnyOrder(
                        DiaryContextType.MEMO,
                        DiaryContextType.PHOTO,
                        DiaryContextType.WEATHER
                );
        assertThat(diaryRepository.findById(diaryId).orElseThrow().getMemo())
                .isEqualTo(String.valueOf('m'));
    }

    @Test
    void replacesSameMemorySourcesOnConsecutiveCalls() {
        User user = userRepository.save(User.create());
        Diary current = saveDiary(user, LocalDate.of(2026, 8, 6), false);
        Diary source = saveDiary(user, LocalDate.of(2026, 8, 5), true);
        Long userId = current.getUser().getId();
        DiaryMemoryAnalysisResponse response = new DiaryMemoryAnalysisResponse(
                List.of(),
                List.of(new MemoryItem(String.valueOf('p'), List.of(source.getId()))),
                List.of(), List.of(), List.of()
        );
        entityManager.flush();
        entityManager.clear();

        memoryPersistenceService.saveSuccess(
                userId, current.getId(), response, Set.of(source.getId()));
        memoryPersistenceService.saveSuccess(
                userId, current.getId(), response, Set.of(source.getId()));
        entityManager.flush();

        assertThat(sourceRepository.findAll()).hasSize(1);
    }

    private Diary saveDiary(User user, LocalDate date, boolean completed) {
        Diary diary = Diary.create(user, date);
        if (completed) {
            diary.complete();
        }
        return diaryRepository.save(diary);
    }

    @TestConfiguration
    static class Config {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
