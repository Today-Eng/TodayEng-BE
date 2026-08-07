package com.example.todayEng.global.security;

import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class GoogleTokenVerifier {
    private final RestClient restClient;
    private final String clientId;

    public GoogleTokenVerifier(RestClient restClient,
                               @Value("${google.client-id:}") String clientId) {
        this.restClient = restClient;
        this.clientId = clientId;
    }

    public GoogleUser verify(String idToken) {
        try {
            GoogleTokenInfo info = restClient.get()
                    .uri(uri -> uri.scheme("https").host("oauth2.googleapis.com")
                            .path("/tokeninfo").queryParam("id_token", idToken).build())
                    .retrieve().body(GoogleTokenInfo.class);
            if (info == null) {
                log.warn("Google ID token verification failed: empty tokeninfo response");
                throw new BaseException(ErrorCode.INVALID_GOOGLE_TOKEN);
            }
            if (info.sub() == null) {
                log.warn("Google ID token verification failed: subject is missing");
                throw new BaseException(ErrorCode.INVALID_GOOGLE_TOKEN);
            }
            if (!"true".equalsIgnoreCase(info.emailVerified())) {
                log.warn("Google ID token verification failed: email is not verified");
                throw new BaseException(ErrorCode.INVALID_GOOGLE_TOKEN);
            }
            if (!clientId.equals(info.aud())) {
                log.warn("Google ID token verification failed: audience mismatch");
                throw new BaseException(ErrorCode.INVALID_GOOGLE_TOKEN);
            }
            return new GoogleUser(info.sub(), info.email());
        } catch (RestClientException e) {
            log.warn("Google ID token verification request failed: {}", e.getMessage());
            throw new BaseException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }
    }

    private record GoogleTokenInfo(String sub, String email, String aud,
                                   @JsonProperty("email_verified") String emailVerified) {}
    public record GoogleUser(String subject, String email) {}
}
