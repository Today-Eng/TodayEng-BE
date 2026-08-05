package com.example.todayEng.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ImageUploadValidatorTest {

    private static final int MAX_IMAGE_SIZE = 7 * 1024 * 1024;

    private final ImageUploadValidator validator = new ImageUploadValidator();

    @Test
    void acceptsTwoImagesAtCountLimit() {
        var jpeg = image("first.jpg", "image/jpeg", jpegBytes(1024));
        var png = image("second.png", "image/png", pngBytes(1024));

        assertThat(validator.validate(List.of(jpeg, png)))
                .containsExactly(jpeg, png);
    }

    @Test
    void rejectsThreeImagesAboveCountLimit() {
        var image = image("day.jpg", "image/jpeg", jpegBytes(1024));

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
    void acceptsTwoImagesAtTotalSizeLimit() {
        var first = image("first.jpg", "image/jpeg", jpegBytes(MAX_IMAGE_SIZE));
        var second = image("second.png", "image/png", pngBytes(MAX_IMAGE_SIZE));

        assertThat(validator.validate(List.of(first, second)))
                .containsExactly(first, second);
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
    void rejectsCorruptedJpegWithMatchingSignature() {
        byte[] corrupted = {
                (byte) 0xff, (byte) 0xd8, (byte) 0xff,
                0x00, 0x01, 0x02, 0x03
        };
        var image = image("corrupted.jpg", "image/jpeg", corrupted);

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
        return validImageBytes("jpg", size);
    }

    private byte[] pngBytes(int size) {
        return validImageBytes("png", size);
    }

    private byte[] validImageBytes(String format, int size) {
        try {
            BufferedImage image = new BufferedImage(
                    2,
                    2,
                    BufferedImage.TYPE_INT_RGB
            );
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, format, outputStream);
            byte[] encoded = outputStream.toByteArray();
            if (encoded.length > size) {
                throw new IllegalArgumentException("Requested image size is too small");
            }
            return Arrays.copyOf(encoded, size);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private void assertError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(errorCode));
    }
}
