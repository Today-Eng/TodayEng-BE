package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.config.GoogleSpeechProperties;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AudioUploadValidator {

    private final GoogleSpeechProperties properties;

    public AudioUploadValidator(GoogleSpeechProperties properties) {
        this.properties = properties;
    }

    public ValidatedAudio validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BaseException(ErrorCode.INVALID_AUDIO_FILE);
        if (file.getSize() > properties.maxFileSizeBytes()) throw new BaseException(ErrorCode.FILE_SIZE_EXCEEDED);
        if (!sameBaseMediaType(properties.contentType(), file.getContentType())) {
            throw new BaseException(ErrorCode.INVALID_AUDIO_FILE);
        }
        try {
            byte[] bytes = file.getBytes();
            if (!isWebm(bytes)) throw new BaseException(ErrorCode.INVALID_AUDIO_FILE);
            return new ValidatedAudio(bytes);
        } catch (IOException exception) {
            throw new BaseException(ErrorCode.MULTIPART_FILE_ERROR);
        }
    }

    private boolean sameBaseMediaType(String expected, String actual) {
        if (actual == null) return false;
        try {
            MediaType expectedType = MediaType.parseMediaType(expected);
            MediaType actualType = MediaType.parseMediaType(actual);
            return expectedType.getType().equalsIgnoreCase(actualType.getType())
                    && expectedType.getSubtype().equalsIgnoreCase(actualType.getSubtype());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isWebm(byte[] b) {
        return b.length >= 4 && (b[0] & 0xff) == 0x1a && (b[1] & 0xff) == 0x45
                && (b[2] & 0xff) == 0xdf && (b[3] & 0xff) == 0xa3;
    }

    public record ValidatedAudio(byte[] bytes) { }
}
