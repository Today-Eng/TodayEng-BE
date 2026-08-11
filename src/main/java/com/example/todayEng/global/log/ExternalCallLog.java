package com.example.todayEng.global.log;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * 외부 API 실패를 로그로 남길 때 응답 본문이 보존되지 않도록 제한된 메타데이터로만 요약한다.
 */
public final class ExternalCallLog {

    private ExternalCallLog() {
    }

    public static String describe(Throwable throwable) {
        if (throwable == null) {
            return "none";
        }
        if (throwable instanceof RestClientResponseException responseException) {
            return throwable.getClass().getSimpleName()
                    + "(status=" + responseException.getStatusCode().value()
                    + ")";
        }
        if (throwable instanceof ResourceAccessException && throwable.getCause() != null) {
            return throwable.getClass().getSimpleName()
                    + "(" + throwable.getCause().getClass().getSimpleName() + ")";
        }
        return throwable.getClass().getSimpleName();
    }
}
