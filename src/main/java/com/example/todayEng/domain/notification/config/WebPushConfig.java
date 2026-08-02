package com.example.todayEng.domain.notification.config;

import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;
import java.security.Security;

@Configuration
@EnableConfigurationProperties(WebPushProperties.class)
public class WebPushConfig {

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
}
