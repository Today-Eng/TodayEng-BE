package com.example.todayEng.domain.user.entity.enums;

import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.util.Arrays;
import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExternalServiceProvider {

    GOOGLE_CALENDAR("google-calendar"),
    SPOTIFY("spotify");

    private final String slug;

    /*
     * OAuth 경로에서 쓰는 slug(google-calendar)와
     * 연동 관리 API에서 쓰는 enum 이름(GOOGLE_CALENDAR)을 모두 허용합니다.
     */
    public static ExternalServiceProvider from(String value) {
        if (value == null || value.isBlank()) {
            throw new BaseException(
                    ErrorCode.UNSUPPORTED_OAUTH_PROVIDER
            );
        }

        String normalized = value.trim()
                .toLowerCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(provider -> provider.matches(normalized))
                .findFirst()
                .orElseThrow(() -> new BaseException(
                        ErrorCode.UNSUPPORTED_OAUTH_PROVIDER
                ));
    }

    private boolean matches(String normalized) {
        return slug.equals(normalized)
                || name().toLowerCase(Locale.ROOT).equals(normalized);
    }
}
