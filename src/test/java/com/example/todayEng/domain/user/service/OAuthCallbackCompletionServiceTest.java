package com.example.todayEng.domain.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.todayEng.domain.user.dto.oauth.ExternalUserInfo;
import com.example.todayEng.domain.user.dto.oauth.OAuthTokenResponse;
import com.example.todayEng.domain.user.entity.OAuthAuthorizationRequest;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.OAuthAuthorizationRequestRepository;
import com.example.todayEng.global.error.exception.BaseException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuthCallbackCompletionServiceTest {

    @Mock OAuthAuthorizationRequestRepository repository;
    @Mock ExternalAccountConnectionService connectionService;

    @Test
    void doesNotSaveAccountWhenRequestIsNoLongerProcessing() {
        OAuthAuthorizationRequest request = OAuthAuthorizationRequest.create(
                User.create(), ExternalServiceProvider.SPOTIFY, "a".repeat(64),
                LocalDateTime.now().plusMinutes(10));
        given(repository.findByIdForUpdate(10L)).willReturn(Optional.of(request));
        OAuthCallbackCompletionService service =
                new OAuthCallbackCompletionService(repository, connectionService);
        OAuthTokenResponse token = new OAuthTokenResponse("access", "refresh", 3600L);
        ExternalUserInfo userInfo = new ExternalUserInfo("id", "email");

        assertThatThrownBy(() -> service.saveAccountAndSucceed(
                10L, 1L, ExternalServiceProvider.SPOTIFY, token, userInfo))
                .isInstanceOf(BaseException.class);

        verify(connectionService, never()).saveOrUpdate(
                1L, ExternalServiceProvider.SPOTIFY, token, userInfo);
    }
}
