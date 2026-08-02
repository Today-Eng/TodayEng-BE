package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todayEng.domain.diary.config.GoogleSpeechProperties;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AudioUploadValidatorTest {
    private final AudioUploadValidator validator = new AudioUploadValidator(
            new GoogleSpeechProperties("en-US", "WEBM_OPUS", "audio/webm", "webm", null, 10));

    @Test
    void acceptsConfiguredWebmByContentTypeAndSignature() {
        var result = validator.validate(new MockMultipartFile("audio", "a.webm", "audio/webm",
                new byte[]{0x1a, 0x45, (byte) 0xdf, (byte) 0xa3}));
        assertThat(result.bytes()).hasSize(4);
    }

    @Test
    void rejectsSpoofedAudio() {
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile(
                "audio", "a.webm", "audio/webm", new byte[]{1, 2, 3, 4})))
                .isInstanceOfSatisfying(BaseException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_AUDIO_FILE));
    }

    @Test
    void rejectsOversizedAudio() {
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile(
                "audio", "a.webm", "audio/webm", new byte[11])))
                .isInstanceOfSatisfying(BaseException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED));
    }

    @Test
    void rejectsNonConfiguredContentType() {
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile(
                "audio", "a.mp3", "audio/mpeg", new byte[]{'I', 'D', '3'})))
                .isInstanceOfSatisfying(BaseException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_AUDIO_FILE));
    }
}
