package com.example.todayEng.domain.diary.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GeminiClientConfig {

    @Bean
    @Qualifier("geminiRestClient")
    public RestClient geminiRestClient(GeminiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(
                properties.connectTimeoutSeconds()
        ));
        requestFactory.setReadTimeout(Duration.ofSeconds(
                properties.readTimeoutSeconds()
        ));

        return RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
    }
}
