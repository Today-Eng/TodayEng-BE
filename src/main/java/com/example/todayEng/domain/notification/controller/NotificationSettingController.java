package com.example.todayEng.domain.notification.controller;

import com.example.todayEng.domain.notification.dto.request.NotificationEnabledRequest;
import com.example.todayEng.domain.notification.dto.request.PushSubscriptionRequest;
import com.example.todayEng.domain.notification.dto.response.NotificationSettingResponse;
import com.example.todayEng.domain.notification.service.NotificationService;
import com.example.todayEng.domain.notification.service.NotificationSettingService;
import com.example.todayEng.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(
        name = "알림",
        description = "푸시 알림 설정 및 구독 관리 API"
)
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;
    private final NotificationService notificationService;

    @Operation(
            summary = "알림 설정 조회",
            description = "로그인한 회원의 알림 활성화 여부와 푸시 구독 정보 존재 여부를 조회합니다."
    )
    @GetMapping("/notification")
    public ApiResponse<NotificationSettingResponse> getNotificationSetting(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                notificationSettingService.getNotificationSetting(userId)
        );
    }

    @Operation(
            summary = "푸시 구독 정보 등록 및 갱신",
            description = "브라우저에서 생성한 Web Push 구독 정보를 저장하고 알림을 활성화합니다."
    )
    @PutMapping("/push-subscription")
    public ApiResponse<NotificationSettingResponse> savePushSubscription(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PushSubscriptionRequest request
    ) {
        return ApiResponse.success(
                notificationSettingService.savePushSubscription(
                        userId,
                        request
                )
        );
    }

    @Operation(
            summary = "알림 활성화 상태 변경",
            description = "로그인한 회원의 알림 수신 여부를 활성화하거나 비활성화합니다."
    )
    @PatchMapping("/notification")
    public ApiResponse<NotificationSettingResponse> updateNotificationEnabled(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody NotificationEnabledRequest request
    ) {
        return ApiResponse.success(
                notificationSettingService.updateNotificationEnabled(
                        userId,
                        request
                )
        );
    }

    @Operation(
            summary = "푸시 구독 해제",
            description = "현재 기기의 푸시 구독 정보만 삭제하며, 사용자의 알림 활성화 설정은 유지합니다."
    )
    @DeleteMapping("/push-subscription")
    public ApiResponse<NotificationSettingResponse> deletePushSubscription(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                notificationSettingService.deletePushSubscription(userId)
        );
    }

    @Operation(
            summary = "테스트 푸시 알림 발송",
            description = "로그인한 회원의 저장된 푸시 구독 정보로 테스트 알림을 발송합니다."
    )
    @PostMapping("/notification/test")
    public ApiResponse<Void> sendTestNotification(
            @AuthenticationPrincipal Long userId
    ) {
        notificationService.sendTestNotification(userId);

        return ApiResponse.success();
    }
}