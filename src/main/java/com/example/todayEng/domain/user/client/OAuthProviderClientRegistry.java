package com.example.todayEng.domain.user.client;

import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class OAuthProviderClientRegistry {

    private final Map<ExternalServiceProvider, OAuthProviderClient> clients;

    public OAuthProviderClientRegistry(
            List<OAuthProviderClient> providerClients
    ) {
        Map<ExternalServiceProvider, OAuthProviderClient> clientMap =
                new EnumMap<>(ExternalServiceProvider.class);

        for (OAuthProviderClient client : providerClients) {
            OAuthProviderClient previousClient =
                    clientMap.put(client.supports(), client);

            if (previousClient != null) {
                throw new IllegalStateException(
                        "동일한 OAuth Provider Client가 중복 등록되었습니다: "
                                + client.supports()
                );
            }
        }

        this.clients = Map.copyOf(clientMap);
    }

    public OAuthProviderClient getClient(
            ExternalServiceProvider provider
    ) {
        OAuthProviderClient client = clients.get(provider);

        if (client == null) {
            throw new BaseException(
                    ErrorCode.UNSUPPORTED_OAUTH_PROVIDER
            );
        }

        return client;
    }
}