package com.example.todayEng.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.diary.config.S3AudioStorageProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3AudioStorageConfigTest {

    @Test
    void createsS3ClientAndPresignerWithoutApacheHttpClient() {
        S3AudioStorageProperties properties = new S3AudioStorageProperties(
                "todayeng-test", "ap-northeast-2", "tts", "stt", Duration.ofMinutes(30));
        S3AudioStorageConfig config = new S3AudioStorageConfig();

        try (S3Client client = config.audioS3Client(properties);
                S3Presigner presigner = config.audioS3Presigner(properties)) {
            assertThat(client).isNotNull();
            assertThat(presigner).isNotNull();
        }
    }
}
