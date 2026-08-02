package com.example.todayEng.domain.user.dto.oauth;

public record ExternalUserInfo(
        String providerAccountId,
        String accountIdentifier
) {
}