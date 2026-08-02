package com.example.todayEng.domain.notification.service;

import com.example.todayEng.domain.notification.dto.WebPushPayload;
import com.example.todayEng.domain.notification.entity.NotificationSetting;
import com.example.todayEng.domain.notification.exception.PushSubscriptionExpiredException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebPushService {

    private static final int NOT_FOUND_STATUS = 404;
    private static final int GONE_STATUS = 410;

    private final PushService pushService;
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
            String payload = objectMapper.writeValueAsString(
                    new WebPushPayload(title, body, url)
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

            if (statusCode == NOT_FOUND_STATUS
                    || statusCode == GONE_STATUS) {
                throw new PushSubscriptionExpiredException();
            }

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
                | ExecutionException exception
        ) {
            throw new IllegalStateException(
                    "Web Push 전송 중 오류가 발생했습니다.",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Web Push 전송이 중단되었습니다.",
                    exception
            );
        }
    }
}