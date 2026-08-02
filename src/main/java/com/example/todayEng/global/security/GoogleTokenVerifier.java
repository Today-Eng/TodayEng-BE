package com.example.todayEng.global.security;

import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
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
            if (info == null || info.sub() == null || !"true".equalsIgnoreCase(info.emailVerified())
                    || !clientId.equals(info.aud())) {
                throw new BaseException(ErrorCode.INVALID_GOOGLE_TOKEN);
            }
            return new GoogleUser(info.sub(), info.email());
        } catch (RestClientException e) {
            throw new BaseException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }
    }

    private record GoogleTokenInfo(String sub, String email, String aud,
                                   @JsonProperty("email_verified") String emailVerified) {}
    public record GoogleUser(String subject, String email) {}
}
