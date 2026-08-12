package com.example.todayEng.domain.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.todayEng.domain.user.entity.enums.OAuthAuthorizationRequestStatus;
import com.example.todayEng.domain.user.repository.OAuthAuthorizationRequestRepository;
import com.example.todayEng.domain.user.repository.UserRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuthAuthorizationRequestServiceTest {

    @Mock OAuthAuthorizationRequestRepository repository;
    @Mock UserRepository userRepository;

    @Test
    void succeedRejectsRequestAlreadyFailedByTimeout() {
        OAuthAuthorizationRequestService service =
                new OAuthAuthorizationRequestService(repository, userRepository);
        given(repository.markSucceeded(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(OAuthAuthorizationRequestStatus.PROCESSING),
                org.mockito.ArgumentMatchers.eq(OAuthAuthorizationRequestStatus.SUCCEEDED),
                org.mockito.ArgumentMatchers.any()))
                .willReturn(0);

        assertThatThrownBy(() -> service.succeed(10L))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.OAUTH_STATE_CONSUME_FAILED);
    }
}
