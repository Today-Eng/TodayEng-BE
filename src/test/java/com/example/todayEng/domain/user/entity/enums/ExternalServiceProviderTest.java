package com.example.todayEng.domain.user.entity.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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

    /*
     * slug와 enum 이름의 표기가 우연히 겹쳐서 통과하는 일이 없도록
     * 모든 상수에 대해 두 표기를 함께 검증합니다.
     * NOTION("notion-db") 처럼 두 표기가 어긋나는 상수가 추가되면 여기서 걸립니다.
     */
    @ParameterizedTest
    @EnumSource(ExternalServiceProvider.class)
    void from_모든_상수는_slug와_enum_이름_양쪽으로_찾을_수_있다(
            ExternalServiceProvider provider
    ) {
        assertThat(ExternalServiceProvider.from(provider.getSlug()))
                .isEqualTo(provider);
        assertThat(ExternalServiceProvider.from(provider.name()))
                .isEqualTo(provider);
    }

    /*
     * 터키어 로케일에서 'I'는 점 없는 'ı'로 소문자 변환됩니다.
     * 로케일 의존 변환을 쓰면 SPOTIFY 가 spotıfy 로 정규화되어 매칭에 실패합니다.
     */
    @Test
    void from_기본_로케일이_터키어여도_정상_동작한다() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));

            assertThat(ExternalServiceProvider.from("SPOTIFY"))
                    .isEqualTo(ExternalServiceProvider.SPOTIFY);
            assertThat(ExternalServiceProvider.from("GOOGLE_CALENDAR"))
                    .isEqualTo(ExternalServiceProvider.GOOGLE_CALENDAR);
        } finally {
            Locale.setDefault(original);
        }
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
