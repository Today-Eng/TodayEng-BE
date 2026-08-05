package com.example.todayEng.domain.diary.service;

import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageUploadValidator {

    private static final int MAX_IMAGE_COUNT = 2;
    private static final long MAX_IMAGE_SIZE = 7L * 1024 * 1024;
    private static final long MAX_TOTAL_IMAGE_SIZE = 14L * 1024 * 1024;
    private static final int SIGNATURE_LENGTH = 12;

    public List<MultipartFile> validate(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        if (images.size() > MAX_IMAGE_COUNT) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
        }

        long totalSize = 0;
        for (MultipartFile image : images) {
            validateImage(image);
            totalSize += image.getSize();
        }
        if (totalSize > MAX_TOTAL_IMAGE_SIZE) {
            throw new BaseException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        return List.copyOf(images);
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new BaseException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        String mediaType = baseMediaType(image.getContentType());
        try (InputStream inputStream = image.getInputStream()) {
            byte[] signature = inputStream.readNBytes(SIGNATURE_LENGTH);
            if (!hasMatchingSignature(mediaType, signature)) {
                throw new BaseException(ErrorCode.INVALID_FILE_EXTENSION);
            }
        } catch (IOException exception) {
            throw new BaseException(ErrorCode.MULTIPART_FILE_ERROR);
        }
    }

    private String baseMediaType(String contentType) {
        if (contentType == null) {
            throw new BaseException(ErrorCode.INVALID_FILE_EXTENSION);
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return mediaType.getType().toLowerCase(Locale.ROOT) + "/"
                    + mediaType.getSubtype().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            throw new BaseException(ErrorCode.INVALID_FILE_EXTENSION);
        }
    }

    private boolean hasMatchingSignature(String mediaType, byte[] bytes) {
        return switch (mediaType) {
            case "image/jpeg" -> isJpeg(bytes);
            case "image/png" -> isPng(bytes);
            case "image/webp" -> isWebp(bytes);
            default -> false;
        };
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && unsigned(bytes[0]) == 0xff
                && unsigned(bytes[1]) == 0xd8
                && unsigned(bytes[2]) == 0xff;
    }

    private boolean isPng(byte[] bytes) {
        int[] signature = {0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (unsigned(bytes[index]) != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E'
                && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private int unsigned(byte value) {
        return value & 0xff;
    }
}
