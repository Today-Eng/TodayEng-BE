package com.example.todayEng.domain.user.entity.enums;

import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.util.Arrays;
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
                .replace('_', '-')
                .toLowerCase();

        return Arrays.stream(values())
                .filter(provider -> provider.slug.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new BaseException(
                        ErrorCode.UNSUPPORTED_OAUTH_PROVIDER
                ));
    }
}
