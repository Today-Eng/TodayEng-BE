package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.todayEng.domain.diary.client.DiaryContextDataClient;
import com.example.todayEng.domain.diary.client.DiaryImageAnalysisClient;
import com.example.todayEng.domain.diary.dto.request.DiaryContextCreateRequest;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.DiaryContext;
import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.diary.repository.DiaryContextRepository;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
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
    @Mock DiaryContextRepository contextRepository;
    @Mock ExternalAccountRepository accountRepository;
    @Mock DiaryContextDataClient dataClient;
    @Mock DiaryImageAnalysisClient imageAnalysisClient;
    @Mock DiaryMemoryService diaryMemoryService;
    DiaryContextService service;
    Diary diary;

    @BeforeEach
    void setUp() {
        service = new DiaryContextService(diaryRepository, contextRepository,
                accountRepository, dataClient, imageAnalysisClient,
                new ImageUploadValidator(), diaryMemoryService, new ObjectMapper());
        User user = User.create();
        ReflectionTestUtils.setField(user, "id", 1L);
        diary = Diary.create(user, LocalDate.of(2026, 7, 30));
        ReflectionTestUtils.setField(diary, "id", 10L);
        given(diaryRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(diary));
        given(diaryMemoryService.create(1L, 10L))
                .willReturn(Optional.empty());
        given(accountRepository.findAllByUser_Id(1L)).willReturn(List.of());
        given(contextRepository.findByDiaryAndContextType(any(), any()))
                .willReturn(Optional.empty());
        given(contextRepository.save(any())).willAnswer(call -> call.getArgument(0));
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
