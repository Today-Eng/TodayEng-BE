package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisResponse;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisResponse.MemoryItem;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.DiaryContextSource;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.DiaryContextRepository;
import com.example.todayEng.domain.diary.repository.DiaryContextSourceRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiaryMemoryPersistenceServiceTest {

    @Mock DiaryRepository diaryRepository;
    @Mock DiaryContextRepository contextRepository;
    @Mock DiaryContextSourceRepository sourceRepository;
    DiaryMemoryPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new DiaryMemoryPersistenceService(
                diaryRepository,
                contextRepository,
                sourceRepository,
                new ObjectMapper()
        );
    }

    @Test
    void replacesSourceLinksWithValidatedDiaries() {
        User owner = user(1L);
        Diary current = diary(10L, owner, DiaryStatus.IN_PROGRESS);
        Diary first = diary(1L, owner, DiaryStatus.COMPLETED);
        Diary second = diary(2L, owner, DiaryStatus.COMPLETED);
        DiaryContext context = mock(DiaryContext.class);
        given(context.getId()).willReturn(100L);
        given(diaryRepository.findByIdForUpdate(10L))
                .willReturn(Optional.of(current));
        given(diaryRepository.findAllById(Set.of(1L, 2L)))
                .willReturn(List.of(first, second));
        given(contextRepository.findByDiaryAndContextType(
                current, DiaryContextType.DIARY_MEMORY
        )).willReturn(Optional.of(context));
        given(contextRepository.saveAndFlush(context)).willReturn(context);

        service.saveSuccess(
                1L,
                10L,
                response(),
                Set.of(1L, 2L)
        );

        verify(sourceRepository).deleteAllByContextId(100L);
        verify(sourceRepository).flush();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DiaryContextSource>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(sourceRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(source -> source.getSourceDiary().getId())
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void rejectsSourceOwnedByAnotherUser() {
        User owner = user(1L);
        Diary current = diary(10L, owner, DiaryStatus.IN_PROGRESS);
        Diary foreign = diary(1L, user(2L), DiaryStatus.COMPLETED);
        given(diaryRepository.findByIdForUpdate(10L))
                .willReturn(Optional.of(current));
        given(diaryRepository.findAllById(Set.of(1L)))
                .willReturn(List.of(foreign));

        assertThatThrownBy(() -> service.saveSuccess(
                1L,
                10L,
                response(),
                Set.of(1L)
        )).isInstanceOfSatisfying(BaseException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_LLM_RESPONSE));
    }

    private DiaryMemoryAnalysisResponse response() {
        return new DiaryMemoryAnalysisResponse(
                List.of(),
                List.of(new MemoryItem("Seongsu", List.of(1L, 2L))),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private User user(Long id) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        return user;
    }

    private Diary diary(Long id, User user, DiaryStatus status) {
        Diary diary = mock(Diary.class);
        lenient().when(diary.getId()).thenReturn(id);
        lenient().when(diary.getUser()).thenReturn(user);
        lenient().when(diary.getStatus()).thenReturn(status);
        return diary;
    }
}
