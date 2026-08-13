package com.example.todayEng.domain.notification.service;

import com.example.todayEng.domain.notification.dto.WebPushPayload;
import com.example.todayEng.domain.notification.dto.WebPushTarget;
import com.example.todayEng.domain.notification.entity.NotificationSetting;
import com.example.todayEng.domain.notification.exception.PushSubscriptionExpiredException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.GeneralSecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Service;
import nl.martijndwars.webpush.Encoding;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushService {

    private static final int NOT_FOUND_STATUS = 404;
    private static final int GONE_STATUS = 410;

    private final PushService pushService;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient webPushHttpClient;

    // 테스트 알림용
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

        send(
                notificationSetting.getPushEndpoint(),
                notificationSetting.getP256dhKey(),
                notificationSetting.getAuthKey(),
                title,
                body,
                url
        );
    }

    // 회고 알림 스케줄러용
    public void send(
            WebPushTarget target,
            String title,
            String body,
            String url
    ) {
        send(
                target.pushEndpoint(),
                target.p256dhKey(),
                target.authKey(),
                title,
                body,
                url
        );
    }

    // 실제 Web Push HTTP 전송
    private void send(
            String pushEndpoint,
            String p256dhKey,
            String authKey,
            String title,
            String body,
            String url
    ) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new WebPushPayload(
                            title,
                            body,
                            url
                    )
            );

            Notification notification = new Notification(
                    pushEndpoint,
                    p256dhKey,
                    authKey,
                    payload
            );

            HttpPost request =
                    pushService.preparePost(
                            notification,
                            Encoding.AES128GCM
                    );

            try (CloseableHttpResponse response =
                         webPushHttpClient.execute(request)) {
                int statusCode =
                        response.getStatusLine().getStatusCode();
                EntityUtils.consume(response.getEntity());

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

                log.info(
                        "Web Push Provider 응답 성공. status={}",
                        statusCode
                );
            }
        } catch (
                GeneralSecurityException
                | IOException
                | JoseException exception
        ) {
            throw new IllegalStateException(
                    "Web Push 전송 중 오류가 발생했습니다.",
                    exception
            );
        }
    }
}
