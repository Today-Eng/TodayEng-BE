package com.example.todayEng.domain.user.dto;

import com.example.todayEng.domain.user.entity.enums.EnglishLevel;
import com.example.todayEng.domain.user.entity.enums.TermsType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class UserDtos {
    private UserDtos() {}

    public record AgreementItem(
            @Schema(description = "약관 ID", example = "1") @NotNull Long termId,
            @Schema(description = "동의 여부", example = "true") @NotNull Boolean agree) {}
    public record AgreementsRequest(@NotEmpty List<@Valid AgreementItem> agreements) {}

    public enum AgreementStatus {
        AGREED,
        DISAGREED,
        NOT_ANSWERED
    }

    public record AgreementResponse(
            Long termId,
            TermsType termsType,
            String title,
            String content,
            boolean required,
            int displayOrder,
            AgreementStatus agreementStatus,
            LocalDateTime agreedAt) {}

    public record AgreementsResponse(
            boolean allRequiredAgreed,
            List<AgreementResponse> agreements) {}

    public record OnboardingRequest(
            @Schema(description = "닉네임", example = "현경")
            @NotBlank @Size(max = 30) String nickname,
            @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
            @Size(max = 500) String profileUrl,
            @Schema(description = "영어 학습 난이도", example = "BEGINNER")
            @NotNull EnglishLevel englishLevel,
            @Schema(description = "관심사 태그 ID 목록", example = "[1, 2, 4]")
            @NotEmpty List<@NotNull Long> interestTagIds) {}

    public record ProfileRequest(
            @Schema(description = "변경할 닉네임", example = "새로운닉네임")
            @NotBlank @Size(max = 30) String nickname) {}

    public record EnglishLevelRequest(
            @Schema(description = "변경할 영어 학습 난이도", example = "INTERMEDIATE")
            @NotNull EnglishLevel englishLevel) {}
    public record InterestsRequest(
            @Schema(description = "새로 설정할 관심사 태그 ID 목록", example = "[2, 3, 5]")
            @NotEmpty List<@NotNull Long> interestTagIds) {}

    public record InterestResponse(Long interestTagId, String tagName) {}
    public record OnboardingResponse(Long userId, String nickname, String profileUrl,
                                     EnglishLevel englishLevel, List<InterestResponse> interests) {}
    public record MeResponse(Long userId, String nickname, String profileUrl,
                             EnglishLevel englishLevel, Long totalDiaryCount, Integer currentStreak,
                             String email, List<InterestResponse> interests) {}
    public record ProfileResponse(String nickname) {}
    public record EnglishLevelResponse(EnglishLevel englishLevel) {}
    public record InterestsResponse(List<InterestResponse> interests) {}
}
