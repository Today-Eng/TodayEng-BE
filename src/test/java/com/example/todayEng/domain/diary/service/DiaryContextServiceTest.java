package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.todayEng.domain.diary.client.DiaryContextDataClient;
import com.example.todayEng.domain.diary.client.DiaryImageAnalysisClient;
import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.home.entity.DailyContextSnapshot;
import com.example.todayEng.domain.home.entity.enums.DailyContextCollectionStatus;
import com.example.todayEng.domain.home.repository.DailyContextSnapshotRepository;
import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiaryContextServiceTest {

    @Mock DiaryRepository diaryRepository;
    @Mock DiaryContextPersistenceService persistenceService;
    @Mock ExternalAccountRepository accountRepository;
    @Mock DiaryContextDataClient dataClient;
    @Mock DiaryImageAnalysisClient imageAnalysisClient;
    @Mock DiaryMemoryService diaryMemoryService;
    @Mock DailyContextSnapshotRepository snapshotRepository;
    DiaryContextService service;
    Diary diary;

    @BeforeEach
    void setUp() {
        service = new DiaryContextService(diaryRepository, persistenceService,
                accountRepository, dataClient, imageAnalysisClient,
                new ImageUploadValidator(), diaryMemoryService, snapshotRepository,
                new ObjectMapper());
        User user = User.create();
        ReflectionTestUtils.setField(user, "id", 1L);
        diary = Diary.create(user, LocalDate.of(2026, 7, 30));
        ReflectionTestUtils.setField(diary, "id", 10L);
        given(diaryRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(diary));
        given(diaryRepository.claimContextCollection(10L, 1L)).willReturn(1);
        given(diaryMemoryService.create(1L, 10L))
                .willReturn(Optional.empty());
        given(accountRepository.findAllByUser_Id(1L)).willReturn(List.of());
        given(snapshotRepository.findAllByUserIdAndContextDateAndCollectionStatus(
                any(), any(), any())).willReturn(List.of());
        given(persistenceService.saveSuccess(any(), any(), any(), any()))
                .willAnswer(call -> DiaryContext.success(
                        diary, call.getArgument(2), call.getArgument(3)));
        given(persistenceService.saveFailure(any(), any(), any()))
                .willAnswer(call -> DiaryContext.failure(
                        diary, call.getArgument(2)));
    }

    @Test
    void analyzesImagesAndStoresOnlyAnalysis() throws Exception {
        var image = new MockMultipartFile(
                "images", "day.jpg", "image/jpeg",
                imageBytes(true));
        var analysis = new ObjectMapper().readTree("{\"summary\":\"공원\"}");
        given(imageAnalysisClient.analyze(List.of(image))).willReturn(analysis);

        var response = service.createContexts(1L, 10L,
                new DiaryContextCreateRequest(null, null), List.of(image));

        verify(imageAnalysisClient).analyze(List.of(image));
        assertThat(response.contexts()).singleElement().satisfies(result -> {
            assertThat(result.type()).isEqualTo(DiaryContextType.PHOTO);
            assertThat(result.success()).isTrue();
        });
        verify(persistenceService).completeContextCollection(1L, 10L);
        verify(persistenceService, never()).failContextCollection(any(), any());
        verify(snapshotRepository).deleteAllByUserIdAndContextDateAndContextType(
                1L, diary.getDiaryDate(), DiaryContextType.PHOTO);
    }

    @Test
    void imageAnalysisFailureIsStoredAsPartialFailure() {
        var image = new MockMultipartFile(
                "images", "day.png", "image/png",
                imageBytes(false));
        given(imageAnalysisClient.analyze(List.of(image)))
                .willThrow(new RuntimeException("timeout"));

        var response = service.createContexts(1L, 10L,
                new DiaryContextCreateRequest(null, null), List.of(image));

        assertThat(response.contexts()).singleElement()
                .matches(result -> !result.success());
        verify(persistenceService).completeContextCollection(1L, 10L);
        verify(snapshotRepository, never()).deleteAllByUserIdAndContextDateAndContextType(
                any(), any(), eq(DiaryContextType.PHOTO));
    }

    @Test
    void rejectsUnsupportedImageTypeBeforeCallingGemini() {
        var image = new MockMultipartFile(
                "images", "note.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> service.createContexts(1L, 10L,
                new DiaryContextCreateRequest(null, null), List.of(image)))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_EXTENSION);

        verify(persistenceService).failContextCollection(1L, 10L);
        verify(persistenceService, never()).completeContextCollection(any(), any());
    }

    @Test
    void rejectsDuplicateContextGenerationBeforeCollecting() {
        given(diaryRepository.claimContextCollection(10L, 1L)).willReturn(0);

        assertThatThrownBy(() -> service.createContexts(1L, 10L,
                new DiaryContextCreateRequest(null, null), List.of()))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DIARY_CONTEXT_ALREADY_GENERATED);

        verify(persistenceService, never()).failContextCollection(any(), any());
        verify(persistenceService, never()).completeContextCollection(any(), any());
        verify(diaryMemoryService, never()).create(any(), any());
    }

    @Test
    void reportsDiaryAlreadyCompletedWhenClaimLosesToAConcurrentCompletion() {
        Diary completedDiary = Diary.create(diary.getUser(), diary.getDiaryDate());
        ReflectionTestUtils.setField(completedDiary, "id", 10L);
        completedDiary.complete();
        given(diaryRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(diary))
                .willReturn(Optional.of(completedDiary));
        given(diaryRepository.claimContextCollection(10L, 1L)).willReturn(0);

        assertThatThrownBy(() -> service.createContexts(1L, 10L,
                new DiaryContextCreateRequest(null, null), List.of()))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DIARY_ALREADY_COMPLETED);
    }

    @Test
    void successfulCollectionSurvivesSnapshotCleanupFailure() throws Exception {
        var image = new MockMultipartFile(
                "images", "day.jpg", "image/jpeg",
                imageBytes(true));
        var analysis = new ObjectMapper().readTree("{\"summary\":\"공원\"}");
        given(imageAnalysisClient.analyze(List.of(image))).willReturn(analysis);
        given(snapshotRepository.deleteAllByUserIdAndContextDateAndContextType(
                any(), any(), any())).willThrow(new RuntimeException("db blip"));

        var response = service.createContexts(1L, 10L,
                new DiaryContextCreateRequest(null, null), List.of(image));

        assertThat(response.contexts()).singleElement()
                .matches(result -> result.success());
        verify(persistenceService, never()).saveFailure(any(), any(), any());
        verify(persistenceService).completeContextCollection(1L, 10L);
    }

    @Test
    void collectsSpotifyDirectlyWhenNoPreloadExists() {
        ExternalAccount spotify = mock(ExternalAccount.class);
        given(spotify.getProvider()).willReturn(ExternalServiceProvider.SPOTIFY);
        given(spotify.isUseEnabled()).willReturn(true);
        given(spotify.getAccessToken()).willReturn("token");
        given(accountRepository.findAllByUser_Id(1L)).willReturn(List.of(spotify));
        JsonNode freshData = new ObjectMapper().createObjectNode();
        given(dataClient.fetchSpotify("token", diary.getDiaryDate())).willReturn(freshData);

        service.createContexts(1L, 10L,
                new DiaryContextCreateRequest(null, null), List.of());

        verify(persistenceService).saveSuccess(1L, 10L, DiaryContextType.SPOTIFY, freshData);
        verify(snapshotRepository).deleteAllByUserIdAndContextDateAndContextType(
                1L, diary.getDiaryDate(), DiaryContextType.SPOTIFY);
    }

    @Test
    void mergesSpotifyPreloadWithFreshFetchWithoutDuplicates() throws Exception {
        ExternalAccount spotify = mock(ExternalAccount.class);
        given(spotify.getProvider()).willReturn(ExternalServiceProvider.SPOTIFY);
        given(spotify.isUseEnabled()).willReturn(true);
        given(spotify.getAccessToken()).willReturn("token");
        given(accountRepository.findAllByUser_Id(1L)).willReturn(List.of(spotify));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode preloadData = objectMapper.readTree("""
                {"items":[
                  {"played_at":"2026-07-30T09:00:00.000Z","track":{"name":"Morning Song"}}
                ]}
                """);
        DailyContextSnapshot snapshot = DailyContextSnapshot.start(
                diary.getUser(), diary.getDiaryDate(), DiaryContextType.SPOTIFY);
        snapshot.succeed(preloadData);
        given(snapshotRepository.findAllByUserIdAndContextDateAndCollectionStatus(
                1L, diary.getDiaryDate(), DailyContextCollectionStatus.SUCCEEDED))
                .willReturn(List.of(snapshot));

        JsonNode freshData = objectMapper.readTree("""
                {"items":[
                  {"played_at":"2026-07-30T09:00:00.000Z","track":{"name":"Morning Song"}},
                  {"played_at":"2026-07-30T20:00:00.000Z","track":{"name":"Evening Song"}}
                ]}
                """);
        given(dataClient.fetchSpotify("token", diary.getDiaryDate())).willReturn(freshData);

        service.createContexts(1L, 10L,
                new DiaryContextCreateRequest(null, null), List.of());

        ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
        verify(persistenceService).saveSuccess(
                eq(1L), eq(10L), eq(DiaryContextType.SPOTIFY), captor.capture());
        JsonNode saved = captor.getValue();
        assertThat(saved.path("items").size()).isEqualTo(2);
        assertThat(saved.path("items").get(0).path("track").path("name").asText())
                .isEqualTo("Evening Song");
        assertThat(saved.path("items").get(1).path("track").path("name").asText())
                .isEqualTo("Morning Song");
        verify(snapshotRepository).deleteAllByUserIdAndContextDateAndContextType(
                1L, diary.getDiaryDate(), DiaryContextType.SPOTIFY);
    }

    @Test
    void fallsBackToPreloadedSpotifyDataWhenFreshFetchFails() throws Exception {
        ExternalAccount spotify = mock(ExternalAccount.class);
        given(spotify.getProvider()).willReturn(ExternalServiceProvider.SPOTIFY);
        given(spotify.isUseEnabled()).willReturn(true);
        given(spotify.getAccessToken()).willReturn("token");
        given(accountRepository.findAllByUser_Id(1L)).willReturn(List.of(spotify));

        JsonNode preloadData = new ObjectMapper().readTree("""
                {"items":[
                  {"played_at":"2026-07-30T09:00:00.000Z","track":{"name":"Morning Song"}}
                ]}
                """);
        DailyContextSnapshot snapshot = DailyContextSnapshot.start(
                diary.getUser(), diary.getDiaryDate(), DiaryContextType.SPOTIFY);
        snapshot.succeed(preloadData);
        given(snapshotRepository.findAllByUserIdAndContextDateAndCollectionStatus(
                1L, diary.getDiaryDate(), DailyContextCollectionStatus.SUCCEEDED))
                .willReturn(List.of(snapshot));
        given(dataClient.fetchSpotify("token", diary.getDiaryDate()))
                .willThrow(new RuntimeException("token expired"));

        service.createContexts(1L, 10L,
                new DiaryContextCreateRequest(null, null), List.of());

        verify(persistenceService).saveSuccess(1L, 10L, DiaryContextType.SPOTIFY, preloadData);
        verify(persistenceService, never())
                .saveFailure(any(), any(), eq(DiaryContextType.SPOTIFY));
    }

    private byte[] imageBytes(boolean jpeg) {
        try {
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String format = jpeg
                    ? new String(new char[]{'j', 'p', 'g'})
                    : new String(new char[]{'p', 'n', 'g'});
            ImageIO.write(image, format, outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
