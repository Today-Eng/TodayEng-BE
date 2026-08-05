package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ImageUploadValidatorTest {

    private static final int MAX_IMAGE_SIZE = 7 * 1024 * 1024;

    private final ImageUploadValidator validator = new ImageUploadValidator();

    @Test
    void acceptsTwoImagesAtCountLimit() {
        var jpeg = image("first.jpg", "image/jpeg", jpegBytes(3));
        var png = image("second.png", "image/png", pngBytes(8));

        assertThat(validator.validate(List.of(jpeg, png)))
                .containsExactly(jpeg, png);
    }

    @Test
    void rejectsThreeImagesAboveCountLimit() {
        var image = image("day.jpg", "image/jpeg", jpegBytes(3));

        assertError(
                () -> validator.validate(List.of(image, image, image)),
                ErrorCode.INVALID_INPUT_VALUE
        );
    }

    @Test
    void acceptsImageAtPerFileSizeLimit() {
        var image = image("day.jpg", "image/jpeg", jpegBytes(MAX_IMAGE_SIZE));

        assertThat(validator.validate(List.of(image))).containsExactly(image);
    }

    @Test
    void rejectsImageAbovePerFileSizeLimit() {
        var image = image("day.jpg", "image/jpeg", jpegBytes(MAX_IMAGE_SIZE + 1));

        assertError(
                () -> validator.validate(List.of(image)),
                ErrorCode.FILE_SIZE_EXCEEDED
        );
    }

    @Test
    void acceptsWebpWithMatchingContentTypeAndSignature() {
        byte[] webp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        var image = image("day.webp", "image/webp", webp);

        assertThat(validator.validate(List.of(image))).containsExactly(image);
    }

    @Test
    void rejectsSpoofedImageContentType() {
        var image = image("fake.jpg", "image/jpeg", "not-an-image".getBytes());

        assertError(
                () -> validator.validate(List.of(image)),
                ErrorCode.INVALID_FILE_EXTENSION
        );
    }

    @Test
    void convertsImageReadFailureToMultipartError() throws IOException {
        MultipartFile image = mock(MultipartFile.class);
        given(image.isEmpty()).willReturn(false);
        given(image.getSize()).willReturn(1L);
        given(image.getContentType()).willReturn("image/jpeg");
        given(image.getInputStream()).willThrow(new IOException("read failed"));

        assertError(
                () -> validator.validate(List.of(image)),
                ErrorCode.MULTIPART_FILE_ERROR
        );
    }

    private MockMultipartFile image(String name, String contentType, byte[] content) {
        return new MockMultipartFile("images", name, contentType, content);
    }

    private byte[] jpegBytes(int size) {
        byte[] bytes = new byte[size];
        bytes[0] = (byte) 0xff;
        bytes[1] = (byte) 0xd8;
        bytes[2] = (byte) 0xff;
        return bytes;
    }

    private byte[] pngBytes(int size) {
        byte[] bytes = new byte[size];
        byte[] signature = {
                (byte) 0x89, 0x50, 0x4e, 0x47,
                0x0d, 0x0a, 0x1a, 0x0a
        };
        System.arraycopy(signature, 0, bytes, 0, signature.length);
        return bytes;
    }

    private void assertError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(errorCode));
    }
}
