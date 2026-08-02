package com.example.todayEng.domain.notification.service;

import com.example.todayEng.domain.notification.config.WebPushProperties;
import com.example.todayEng.domain.notification.dto.WebPushPayload;
import com.example.todayEng.domain.notification.entity.NotificationSetting;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Service;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

@Service
@RequiredArgsConstructor
public class WebPushService {

    private final WebPushProperties webPushProperties;
    private final ObjectMapper objectMapper;

    public void send(
            NotificationSetting notificationSetting,
            String title,
            String body,
            String url
    ) {
        if (!notificationSetting.isUseEnabled()
                || !notificationSetting.hasPushSubscription()) {
            return;
        }

        try {
            PushService pushService = createPushService();

            String payload = objectMapper.writeValueAsString(
                    new WebPushPayload(
                            title,
                            body,
                            url
                    )
            );

            Notification notification = new Notification(
                    notificationSetting.getPushEndpoint(),
                    notificationSetting.getP256dhKey(),
                    notificationSetting.getAuthKey(),
                    payload
            );

            HttpResponse response =
                    pushService.send(notification);

            int statusCode =
                    response.getStatusLine().getStatusCode();

            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException(
                        "Web Push 전송에 실패했습니다. status="
                                + statusCode
                );
            }
        } catch (
                GeneralSecurityException
                | IOException
                | JoseException
                | ExecutionException e
        ) {
            throw new IllegalStateException(
                    "Web Push 전송 중 오류가 발생했습니다.",
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Web Push 전송이 중단되었습니다.",
                    e
            );
        }
    }

    private PushService createPushService()
            throws GeneralSecurityException {

        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        return new PushService()
                .setPublicKey(
                        webPushProperties.publicKey()
                )
                .setPrivateKey(
                        webPushProperties.privateKey()
                )
                .setSubject(
                        webPushProperties.subject()
                );
    }
}