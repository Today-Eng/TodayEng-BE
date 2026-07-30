package com.example.todayEng.domain.user.client;

import com.example.todayEng.domain.user.dto.oauth.ExternalUserInfo;
import com.example.todayEng.domain.user.dto.oauth.OAuthTokenResponse;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;

public interface OAuthProviderClient {

    /**
     * 해당 OAuth Client가 지원하는 외부 서비스 Provider를 반환합니다.
     */
    ExternalServiceProvider supports();

    /**
     * Provider의 OAuth 인가 페이지로 이동할 URL을 생성합니다.
     *
     * @param state 서버에서 발급한 일회성 OAuth state 원문
     * @return Provider 인가 URL
     */
    String buildAuthorizationUrl(String state);

    /**
     * Authorization Code를 Provider의 토큰으로 교환합니다.
     *
     * @param code Provider가 콜백으로 전달한 Authorization Code
     * @return Provider 응답을 공통 형식으로 변환한 토큰 정보
     */
    OAuthTokenResponse exchangeToken(String code);

    /**
     * Access Token으로 외부 계정의 식별 정보를 조회합니다.
     *
     * @param accessToken Provider에서 발급받은 Access Token
     * @return 외부 계정 식별자와 화면 표시용 계정 정보
     */
    ExternalUserInfo fetchUserInfo(String accessToken);
}