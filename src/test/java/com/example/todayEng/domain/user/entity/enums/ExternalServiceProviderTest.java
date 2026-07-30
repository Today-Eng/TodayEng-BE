package com.example.todayEng.domain.user.entity.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import org.junit.jupiter.api.Test;

class ExternalServiceProviderTest {

    @Test
    void fromSlug_returnsMatchingProvider() {
        assertThat(ExternalServiceProvider.fromSlug("google-calendar"))
                .isEqualTo(ExternalServiceProvider.GOOGLE_CALENDAR);
        assertThat(ExternalServiceProvider.fromSlug("spotify"))
                .isEqualTo(ExternalServiceProvider.SPOTIFY);
    }

    @Test
    void fromSlug_unsupportedProvider_throws() {
        assertThatThrownBy(() ->
                ExternalServiceProvider.fromSlug("unknown")
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    }

    @Test
    void fromSlug_null_throws() {
        assertThatThrownBy(() ->
                ExternalServiceProvider.fromSlug(null)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    }
}
