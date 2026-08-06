package com.example.todayEng.global.config;

import com.example.todayEng.domain.diary.config.S3AudioStorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(name = "storage.audio.type", havingValue = "s3")
public class S3AudioStorageConfig {

    @Bean
    public S3Client audioS3Client(S3AudioStorageProperties properties) {
        properties.validate();
        return S3Client.builder()
                .region(Region.of(properties.region()))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }

    @Bean
    public S3Presigner audioS3Presigner(S3AudioStorageProperties properties) {
        properties.validate();
        return S3Presigner.builder().region(Region.of(properties.region())).build();
    }
}
