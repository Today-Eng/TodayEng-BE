package com.example.todayEng.domain.user.service;

import com.example.todayEng.domain.user.dto.response.ExternalAccountResponse;
import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExternalAccountService {

    private final ExternalAccountRepository externalAccountRepository;

    public List<ExternalAccountResponse> getExternalAccounts(Long userId) {
        return externalAccountRepository.findAllByUser_Id(userId).stream()
                .map(ExternalAccountResponse::from)
                .toList();
    }

    @Transactional
    public void updateUseEnabled(
            Long userId,
            Long externalAccountId,
            boolean useEnabled
    ) {
        ExternalAccount externalAccount = getOwnedExternalAccount(userId, externalAccountId);
        externalAccount.updateUseEnabled(useEnabled);
    }

    @Transactional
    public void disconnect(
            Long userId,
            Long externalAccountId
    ) {
        ExternalAccount externalAccount = getOwnedExternalAccount(userId, externalAccountId);
        externalAccountRepository.delete(externalAccount);
    }

    private ExternalAccount getOwnedExternalAccount(
            Long userId,
            Long externalAccountId
    ) {
        return externalAccountRepository.findByIdAndUser_Id(externalAccountId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.EXTERNAL_ACCOUNT_NOT_FOUND));
    }
}
