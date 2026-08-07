package com.example.todayEng.domain.user.service;

import com.example.todayEng.domain.user.dto.response.ExternalAccountListResponse;
import com.example.todayEng.domain.user.dto.response.ExternalAccountResponse;
import com.example.todayEng.domain.user.dto.response.ExternalAccountSettingsResponse;
import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExternalAccountService {

    private final ExternalAccountRepository externalAccountRepository;

    /*
     * 연동되지 않은 provider도 connected=false 로 함께 내려줍니다.
     * 프론트가 연동 관리 화면을 provider 목록 기준으로 그리기 때문입니다.
     */
    public ExternalAccountListResponse getExternalAccounts(Long userId) {
        Map<ExternalServiceProvider, ExternalAccount> connectedAccounts =
                new EnumMap<>(ExternalServiceProvider.class);

        externalAccountRepository.findAllByUser_Id(userId)
                .forEach(account ->
                        connectedAccounts.put(account.getProvider(), account)
                );

        List<ExternalAccountResponse> externalAccounts =
                Arrays.stream(ExternalServiceProvider.values())
                        .map(provider -> toResponse(
                                provider,
                                connectedAccounts.get(provider)
                        ))
                        .toList();

        return new ExternalAccountListResponse(externalAccounts);
    }

    @Transactional
    public ExternalAccountSettingsResponse updateUseEnabled(
            Long userId,
            ExternalServiceProvider provider,
            boolean useEnabled
    ) {
        ExternalAccount externalAccount =
                externalAccountRepository
                        .findByUser_IdAndProvider(userId, provider)
                        .orElseThrow(() -> new BaseException(
                                ErrorCode.EXTERNAL_ACCOUNT_NOT_CONNECTED
                        ));

        externalAccount.updateUseEnabled(useEnabled);

        return ExternalAccountSettingsResponse.from(externalAccount);
    }

    /*
     * 이미 해제된 상태로 다시 호출해도 성공으로 응답하는 멱등 API입니다.
     */
    @Transactional
    public void disconnect(
            Long userId,
            ExternalServiceProvider provider
    ) {
        externalAccountRepository
                .findByUser_IdAndProvider(userId, provider)
                .ifPresent(externalAccountRepository::delete);
    }

    private ExternalAccountResponse toResponse(
            ExternalServiceProvider provider,
            ExternalAccount externalAccount
    ) {
        if (externalAccount == null) {
            return ExternalAccountResponse.disconnected(provider);
        }

        return ExternalAccountResponse.from(externalAccount);
    }
}
