package com.example.todayEng.domain.user.entity.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import org.junit.jupiter.api.Test;

class ExternalServiceProviderTest {

    @Test
    void from_slug_returnsMatchingProvider() {
        assertThat(ExternalServiceProvider.from("google-calendar"))
                .isEqualTo(ExternalServiceProvider.GOOGLE_CALENDAR);
        assertThat(ExternalServiceProvider.from("spotify"))
                .isEqualTo(ExternalServiceProvider.SPOTIFY);
    }

    @Test
    void from_enumName_returnsMatchingProvider() {
        assertThat(ExternalServiceProvider.from("GOOGLE_CALENDAR"))
                .isEqualTo(ExternalServiceProvider.GOOGLE_CALENDAR);
        assertThat(ExternalServiceProvider.from("SPOTIFY"))
                .isEqualTo(ExternalServiceProvider.SPOTIFY);
    }

    @Test
    void from_unsupportedProvider_throws() {
        assertThatThrownBy(() ->
                ExternalServiceProvider.from("unknown")
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    }

    @Test
    void from_null_throws() {
        assertThatThrownBy(() ->
                ExternalServiceProvider.from(null)
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    }
}
