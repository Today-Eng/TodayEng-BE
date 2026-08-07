package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
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
import com.example.todayEng.domain.home.service.DailyContextSnapshotPersistenceService;
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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    @Mock DailyContextSnapshotPersistenceService snapshotPersistenceService;
    DiaryContextService service;
    Diary diary;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-30T10:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        service = new DiaryContextService(diaryRepository, persistenceService,
                accountRepository, dataClient, imageAnalysisClient,
                new ImageUploadValidator(), diaryMemoryService, snapshotPersistenceService,
                new ObjectMapper(), clock);
        User user = User.create();
        ReflectionTestUtils.setField(user, "id", 1L);
        diary = Diary.create(user, LocalDate.of(2026, 7, 30));
        ReflectionTestUtils.setField(diary, "id", 10L);
        given(diaryRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(diary));
        given(persistenceService.claimContextCollection(eq(10L), eq(1L), any()))
                .willReturn(true);
        given(diaryMemoryService.create(1L, 10L))
                .willReturn(Optional.empty());
        given(accountRepository.findAllByUser_Id(1L)).willReturn(List.of());
        given(snapshotPersistenceService.findSuccessfulContextData(any(), any(), any()))
                .willReturn(Optional.empty());
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
        verify(snapshotPersistenceService).cleanupCollected(
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
        verify(snapshotPersistenceService, never()).cleanupCollected(
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
        given(persistenceService.claimContextCollection(eq(10L), eq(1L), any()))
                .willReturn(false);
        given(persistenceService.reclaimStaleContextCollection(eq(10L), eq(1L), any(), any()))
                .willReturn(false);

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
        given(persistenceService.claimContextCollection(eq(10L), eq(1L), any()))
                .willReturn(false);
        given(persistenceService.reclaimStaleContextCollection(eq(10L), eq(1L), any(), any()))
                .willReturn(false);

        assertThatThrownBy(() -> service.createContexts(1L, 10L,
                new DiaryContextCreateRequest(null, null), List.of()))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DIARY_ALREADY_COMPLETED);
    }

    @Test
    void reclaimsStaleCollectingClaimAfterCrashAndProceeds() throws Exception {
        given(persistenceService.claimContextCollection(eq(10L), eq(1L), any()))
                .willReturn(false);
        given(persistenceService.reclaimStaleContextCollection(eq(10L), eq(1L), any(), any()))
                .willReturn(true);
        var image = new MockMultipartFile(
                "images", "day.jpg", "image/jpeg",
                imageBytes(true));
        var analysis = new ObjectMapper().readTree("{\"summary\":\"공원\"}");
        given(imageAnalysisClient.analyze(List.of(image))).willReturn(analysis);

        var response = service.createContexts(1L, 10L,
                new DiaryContextCreateRequest(null, null), List.of(image));

        assertThat(response.contexts()).singleElement()
                .matches(result -> result.success());
        verify(persistenceService).completeContextCollection(1L, 10L);
    }

    @Test
    void failsClaimedCollectionWhenAnUnrecoverableErrorEscapes() {
        given(diaryMemoryService.create(1L, 10L))
                .willThrow(new OutOfMemoryError("simulated"));

        assertThatThrownBy(() -> service.createContexts(1L, 10L,
                new DiaryContextCreateRequest(null, null), List.of()))
                .isInstanceOf(OutOfMemoryError.class);

        verify(persistenceService).failContextCollection(1L, 10L);
        verify(persistenceService, never()).completeContextCollection(any(), any());
    }

    @Test
    void successfulCollectionSurvivesSnapshotCleanupFailure() throws Exception {
        var image = new MockMultipartFile(
                "images", "day.jpg", "image/jpeg",
                imageBytes(true));
        var analysis = new ObjectMapper().readTree("{\"summary\":\"공원\"}");
        given(imageAnalysisClient.analyze(List.of(image))).willReturn(analysis);
        willThrow(new RuntimeException("db blip"))
                .given(snapshotPersistenceService)
                .cleanupCollected(any(), any(), any());

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
        verify(snapshotPersistenceService).cleanupCollected(
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
        given(snapshotPersistenceService.findSuccessfulContextData(
                1L, diary.getDiaryDate(), DiaryContextType.SPOTIFY))
                .willReturn(Optional.of(preloadData));

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
        verify(snapshotPersistenceService).cleanupCollected(
                1L, diary.getDiaryDate(), DiaryContextType.SPOTIFY);
    }

    @Test
    void mergeSkipsItemsMissingPlayedAtAndPreservesOtherFields() throws Exception {
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
        given(snapshotPersistenceService.findSuccessfulContextData(
                1L, diary.getDiaryDate(), DiaryContextType.SPOTIFY))
                .willReturn(Optional.of(preloadData));

        JsonNode freshData = objectMapper.readTree("""
                {"href":"https://api.spotify.com/v1/me/player/recently-played",
                 "next":null,
                 "cursors":{"after":"123456","before":"654321"},
                 "limit":50,
                 "total":2,
                 "items":[
                  {"played_at":"2026-07-30T09:00:00.000Z","track":{"name":"Morning Song"}},
                  {"track":{"name":"Untimed Song"}}
                ]}
                """);
        given(dataClient.fetchSpotify("token", diary.getDiaryDate())).willReturn(freshData);

        service.createContexts(1L, 10L,
                new DiaryContextCreateRequest(null, null), List.of());

        ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
        verify(persistenceService).saveSuccess(
                eq(1L), eq(10L), eq(DiaryContextType.SPOTIFY), captor.capture());
        JsonNode saved = captor.getValue();
        assertThat(saved.path("items").size()).isEqualTo(1);
        assertThat(saved.path("items").get(0).path("track").path("name").asText())
                .isEqualTo("Morning Song");
        assertThat(saved.path("href").asText())
                .isEqualTo("https://api.spotify.com/v1/me/player/recently-played");
        assertThat(saved.path("cursors").path("after").asText()).isEqualTo("123456");
        assertThat(saved.path("cursors").path("before").asText()).isEqualTo("654321");
        assertThat(saved.path("limit").asInt()).isEqualTo(50);
        assertThat(saved.path("total").asInt()).isEqualTo(2);
    }

    @Test
    void savesSpotifyFailureWhenFreshFetchFailsWithoutPreload() {
        ExternalAccount spotify = mock(ExternalAccount.class);
        given(spotify.getProvider()).willReturn(ExternalServiceProvider.SPOTIFY);
        given(spotify.isUseEnabled()).willReturn(true);
        given(spotify.getAccessToken()).willReturn("token");
        given(accountRepository.findAllByUser_Id(1L)).willReturn(List.of(spotify));
        given(dataClient.fetchSpotify("token", diary.getDiaryDate()))
                .willThrow(new RuntimeException("token expired"));

        var response = service.createContexts(1L, 10L,
                new DiaryContextCreateRequest(null, null), List.of());

        assertThat(response.contexts()).singleElement()
                .matches(result -> !result.success());
        verify(persistenceService).saveFailure(1L, 10L, DiaryContextType.SPOTIFY);
        verify(snapshotPersistenceService, never()).cleanupCollected(
                any(), any(), eq(DiaryContextType.SPOTIFY));
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
        given(snapshotPersistenceService.findSuccessfulContextData(
                1L, diary.getDiaryDate(), DiaryContextType.SPOTIFY))
                .willReturn(Optional.of(preloadData));
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
