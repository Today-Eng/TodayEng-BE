package com.example.todayEng.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.todayEng.domain.notification.dto.request.NotificationEnabledRequest;
import com.example.todayEng.domain.notification.dto.request.PushSubscriptionRequest;
import com.example.todayEng.domain.notification.dto.response.NotificationSettingResponse;
import com.example.todayEng.domain.notification.entity.NotificationSetting;
import com.example.todayEng.domain.notification.repository.NotificationSettingRepository;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.UserRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceTest {

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationSettingService notificationSettingService;

    private final Long userId = 1L;
    private final User user = User.create();

    @Test
    @DisplayName("알림 설정이 없으면 비활성화 및 미구독 상태를 반환한다")
    void getNotificationSetting_withoutSetting_returnsDisabled() {
        given(userRepository.existsById(userId))
                .willReturn(true);

        given(notificationSettingRepository.findByUserId(userId))
                .willReturn(Optional.empty());

        NotificationSettingResponse response =
                notificationSettingService.getNotificationSetting(userId);

        assertThat(response.isEnabled()).isFalse();
        assertThat(response.hasPushSubscription()).isFalse();
    }

    @Test
    @DisplayName("푸시 구독 정보를 처음 저장하면 알림 설정이 생성되고 활성화된다")
    void savePushSubscription_newSetting_success() {
        PushSubscriptionRequest request =
                new PushSubscriptionRequest(
                        "https://example.com/push",
                        new PushSubscriptionRequest.PushSubscriptionKeys(
                                "p256dh-key",
                                "auth-key"
                        )
                );

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(
                notificationSettingRepository
                        .findOtherEndpointOwnerForUpdate(
                                "https://example.com/push",
                                userId
                        )
        ).willReturn(Optional.empty());

        given(notificationSettingRepository.findByUserId(userId))
                .willReturn(Optional.empty());

        given(notificationSettingRepository.save(
                org.mockito.ArgumentMatchers.any(NotificationSetting.class)
        )).willAnswer(invocation -> invocation.getArgument(0));

        NotificationSettingResponse response =
                notificationSettingService.savePushSubscription(
                        userId,
                        request
                );

        assertThat(response.isEnabled()).isTrue();
        assertThat(response.hasPushSubscription()).isTrue();

        verify(notificationSettingRepository)
                .save(org.mockito.ArgumentMatchers.any(NotificationSetting.class));
    }

    @Test
    @DisplayName("알림이 비활성화된 기존 설정의 구독 정보를 갱신해도 비활성화 상태를 유지한다")
    void savePushSubscription_existingDisabledSetting_keepsDisabled() {
        NotificationSetting setting =
                NotificationSetting.create(user);

        setting.updatePushSubscription(
                "https://example.com/old-push",
                "old-p256dh-key",
                "old-auth-key"
        );
        setting.disable();

        PushSubscriptionRequest request =
                new PushSubscriptionRequest(
                        "https://example.com/new-push",
                        new PushSubscriptionRequest.PushSubscriptionKeys(
                                "new-p256dh-key",
                                "new-auth-key"
                        )
                );

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(
                notificationSettingRepository
                        .findOtherEndpointOwnerForUpdate(
                                "https://example.com/new-push",
                                userId
                        )
        ).willReturn(Optional.empty());

        given(notificationSettingRepository.findByUserId(userId))
                .willReturn(Optional.of(setting));

        given(notificationSettingRepository.save(setting))
                .willReturn(setting);

        NotificationSettingResponse response =
                notificationSettingService.savePushSubscription(
                        userId,
                        request
                );

        assertThat(response.isEnabled()).isFalse();
        assertThat(response.hasPushSubscription()).isTrue();

        assertThat(setting.getPushEndpoint())
                .isEqualTo("https://example.com/new-push");
        assertThat(setting.getP256dhKey())
                .isEqualTo("new-p256dh-key");
        assertThat(setting.getAuthKey())
                .isEqualTo("new-auth-key");
    }

    @Test
    @DisplayName("알림이 활성화된 기존 설정의 구독 정보를 갱신하면 활성화 상태를 유지한다")
    void savePushSubscription_existingEnabledSetting_keepsEnabled() {
        NotificationSetting setting =
                NotificationSetting.create(user);

        setting.updatePushSubscription(
                "https://example.com/old-push",
                "old-p256dh-key",
                "old-auth-key"
        );
        setting.enable();

        PushSubscriptionRequest request =
                new PushSubscriptionRequest(
                        "https://example.com/new-push",
                        new PushSubscriptionRequest.PushSubscriptionKeys(
                                "new-p256dh-key",
                                "new-auth-key"
                        )
                );

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(notificationSettingRepository.findByUserId(userId))
                .willReturn(Optional.of(setting));

        given(notificationSettingRepository.save(setting))
                .willReturn(setting);

        NotificationSettingResponse response =
                notificationSettingService.savePushSubscription(
                        userId,
                        request
                );

        assertThat(response.isEnabled()).isTrue();
        assertThat(response.hasPushSubscription()).isTrue();

        assertThat(setting.getPushEndpoint())
                .isEqualTo("https://example.com/new-push");
    }

    @Test
    @DisplayName("동일 endpoint가 다른 사용자에게 연결되어 있으면 이전 연결을 제거하고 현재 사용자에게 저장한다")
    void savePushSubscription_endpointOwnedByAnotherUser_reassignsEndpoint() {
        NotificationSetting previousSetting =
                NotificationSetting.create(
                        User.create()
                );

        previousSetting.updatePushSubscription(
                "https://example.com/shared-push",
                "previous-p256dh",
                "previous-auth"
        );
        previousSetting.enable();

        NotificationSetting currentSetting =
                NotificationSetting.create(user);

        currentSetting.updatePushSubscription(
                "https://example.com/current-push",
                "current-p256dh",
                "current-auth"
        );
        currentSetting.enable();

        PushSubscriptionRequest request =
                new PushSubscriptionRequest(
                        "https://example.com/shared-push",
                        new PushSubscriptionRequest.PushSubscriptionKeys(
                                "new-p256dh",
                                "new-auth"
                        )
                );

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(
                notificationSettingRepository
                        .findOtherEndpointOwnerForUpdate(
                                "https://example.com/shared-push",
                                userId
                        )
        ).willReturn(Optional.of(previousSetting));

        given(notificationSettingRepository.findByUserId(userId))
                .willReturn(Optional.of(currentSetting));

        given(notificationSettingRepository.save(currentSetting))
                .willReturn(currentSetting);

        NotificationSettingResponse response =
                notificationSettingService.savePushSubscription(
                        userId,
                        request
                );

        assertThat(previousSetting.isUseEnabled()).isTrue();
        assertThat(previousSetting.hasPushSubscription()).isFalse();
        assertThat(previousSetting.getPushEndpoint()).isNull();
        assertThat(previousSetting.getP256dhKey()).isNull();
        assertThat(previousSetting.getAuthKey()).isNull();

        assertThat(currentSetting.isUseEnabled()).isTrue();
        assertThat(currentSetting.getPushEndpoint())
                .isEqualTo("https://example.com/shared-push");
        assertThat(currentSetting.getP256dhKey())
                .isEqualTo("new-p256dh");
        assertThat(currentSetting.getAuthKey())
                .isEqualTo("new-auth");

        assertThat(response.isEnabled()).isTrue();
        assertThat(response.hasPushSubscription()).isTrue();

        verify(notificationSettingRepository)
                .flush();

        verify(notificationSettingRepository)
                .save(currentSetting);
    }

    @Test
    @DisplayName("구독 정보가 없는 상태에서 알림 활성화를 요청하면 예외가 발생한다")
    void updateNotificationEnabled_withoutSubscription_throws() {
        given(userRepository.existsById(userId))
                .willReturn(true);

        given(notificationSettingRepository.findByUserId(userId))
                .willReturn(Optional.empty());

        NotificationEnabledRequest request =
                new NotificationEnabledRequest(true);

        assertThatThrownBy(() ->
                notificationSettingService.updateNotificationEnabled(
                        userId,
                        request
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.PUSH_SUBSCRIPTION_REQUIRED);
    }

    @Test
    @DisplayName("알림을 비활성화하면 구독 정보는 유지되고 사용 상태만 비활성화된다")
    void updateNotificationEnabled_disable_success() {
        NotificationSetting setting =
                NotificationSetting.create(user);

        setting.updatePushSubscription(
                "https://example.com/push",
                "p256dh-key",
                "auth-key"
        );

        given(userRepository.existsById(userId))
                .willReturn(true);

        given(notificationSettingRepository.findByUserId(userId))
                .willReturn(Optional.of(setting));

        NotificationSettingResponse response =
                notificationSettingService.updateNotificationEnabled(
                        userId,
                        new NotificationEnabledRequest(false)
                );

        assertThat(response.isEnabled()).isFalse();
        assertThat(response.hasPushSubscription()).isTrue();
    }

    @Test
    @DisplayName("푸시 구독을 삭제하면 구독 정보만 제거되고 알림 활성화 설정은 유지된다")
    void deletePushSubscription_success() {
        NotificationSetting setting =
                NotificationSetting.create(user);

        setting.updatePushSubscription(
                "https://example.com/push",
                "p256dh-key",
                "auth-key"
        );

        // 삭제 전 알림 ON 상태를 명시적으로 준비
        setting.enable();

        given(userRepository.existsById(userId))
                .willReturn(true);

        given(notificationSettingRepository.findByUserId(userId))
                .willReturn(Optional.of(setting));

        NotificationSettingResponse response =
                notificationSettingService.deletePushSubscription(userId);

        assertThat(response.isEnabled()).isTrue();
        assertThat(response.hasPushSubscription()).isFalse();

        assertThat(setting.isUseEnabled()).isTrue();
        assertThat(setting.getPushEndpoint()).isNull();
        assertThat(setting.getP256dhKey()).isNull();
        assertThat(setting.getAuthKey()).isNull();
    }

    @Test
    @DisplayName("알림이 비활성화된 상태에서 구독을 삭제하면 비활성화 설정은 그대로 유지된다")
    void deletePushSubscription_whenDisabled_keepsDisabled() {
        NotificationSetting setting =
                NotificationSetting.create(user);

        setting.updatePushSubscription(
                "https://example.com/push",
                "p256dh-key",
                "auth-key"
        );

        setting.disable();

        given(userRepository.existsById(userId))
                .willReturn(true);

        given(notificationSettingRepository.findByUserId(userId))
                .willReturn(Optional.of(setting));

        NotificationSettingResponse response =
                notificationSettingService.deletePushSubscription(userId);

        assertThat(response.isEnabled()).isFalse();
        assertThat(response.hasPushSubscription()).isFalse();

        assertThat(setting.isUseEnabled()).isFalse();
        assertThat(setting.getPushEndpoint()).isNull();
        assertThat(setting.getP256dhKey()).isNull();
        assertThat(setting.getAuthKey()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 알림 설정을 조회하면 예외가 발생한다")
    void getNotificationSetting_userNotFound_throws() {
        given(userRepository.existsById(userId))
                .willReturn(false);

        assertThatThrownBy(() ->
                notificationSettingService.getNotificationSetting(userId)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
