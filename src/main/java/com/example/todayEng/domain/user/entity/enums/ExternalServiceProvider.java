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

    public static ExternalServiceProvider fromSlug(String slug) {
        return Arrays.stream(values())
                .filter(provider -> provider.slug.equals(slug))
                .findFirst()
                .orElseThrow(() -> new BaseException(
                        ErrorCode.UNSUPPORTED_OAUTH_PROVIDER
                ));
    }
}
