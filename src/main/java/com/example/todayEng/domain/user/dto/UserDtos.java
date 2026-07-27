package com.example.todayEng.domain.user.dto;

import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class UserDtos {
    private UserDtos() {}

    public record AgreementItem(@NotNull Long termId, @NotNull Boolean agree) {}
    public record AgreementsRequest(@NotEmpty List<@Valid AgreementItem> agreements) {}

    public record OnboardingRequest(
            @NotBlank @Size(max = 30) String nickname,
            @Size(max = 500) String profileUrl,
            @NotNull EnglishLevel englishLevel,
            @NotEmpty List<@NotNull Long> interestTagIds) {}

    public record ProfileRequest(@NotBlank @Size(max = 30) String nickname) {}

    public record EnglishLevelRequest(@NotNull EnglishLevel englishLevel) {}
    public record InterestsRequest(@NotEmpty List<@NotNull Long> interestTagIds) {}

    public record InterestResponse(Long interestTagId, String tagName) {}
    public record OnboardingResponse(Long userId, String nickname, String profileUrl,
                                     EnglishLevel englishLevel, List<InterestResponse> interests) {}
    public record MeResponse(Long userId, String nickname, String profileUrl,
                             EnglishLevel englishLevel, Long totalDiaryCount, Integer currentStreak,
                             List<InterestResponse> interests) {}
    public record ProfileResponse(String nickname) {}
    public record EnglishLevelResponse(EnglishLevel englishLevel) {}
    public record InterestsResponse(List<InterestResponse> interests) {}
}
