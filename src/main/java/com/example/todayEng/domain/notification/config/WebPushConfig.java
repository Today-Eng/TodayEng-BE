package com.example.todayEng.domain.notification.config;

import nl.martijndwars.webpush.PushService;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;
import java.security.Security;

@Configuration
@EnableConfigurationProperties(WebPushProperties.class)
public class WebPushConfig {

    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final int RESPONSE_TIMEOUT_MILLIS = 5_000;
    private static final int CONNECTION_REQUEST_TIMEOUT_MILLIS = 1_000;

    @Bean
    public PushService pushService(
            WebPushProperties properties
    ) throws GeneralSecurityException {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(
                    new BouncyCastleProvider()
            );
        }

        return new PushService()
                .setPublicKey(properties.publicKey())
                .setPrivateKey(properties.privateKey())
                .setSubject(properties.subject());
    }

    @Bean(destroyMethod = "close")
    public CloseableHttpClient webPushHttpClient() {
        RequestConfig requestConfig =
                RequestConfig.custom()
                        .setConnectTimeout(
                                CONNECT_TIMEOUT_MILLIS
                        )
                        .setSocketTimeout(
                                RESPONSE_TIMEOUT_MILLIS
                        )
                        .setConnectionRequestTimeout(
                                CONNECTION_REQUEST_TIMEOUT_MILLIS
                        )
                        .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .disableAutomaticRetries()
                .build();
    }
}
