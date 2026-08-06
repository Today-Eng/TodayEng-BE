package com.example.todayEng.domain.user.controller;

import com.example.todayEng.domain.user.dto.UserDtos.*;
import com.example.todayEng.domain.user.service.UserService;
import com.example.todayEng.global.common.ApiResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "회원", description = "온보딩 및 마이페이지 회원 관리 API")
public class UserController {
    private final UserService userService;

    @Operation(summary = "약관 동의", description = "로그인한 회원의 약관 동의 여부를 저장합니다.")
    @PostMapping("/agreements")
    public ApiResponse<Void> agreements(@AuthenticationPrincipal Long userId,
                                        @Valid @RequestBody AgreementsRequest request) {
        userService.agree(userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "온보딩 완료", description = "닉네임, 프로필 이미지, 학습 난이도와 관심사를 저장하여 온보딩을 완료합니다.")
    @PostMapping("/onboarding")
    public ApiResponse<OnboardingResponse> onboarding(@AuthenticationPrincipal Long userId,
                                                       @Valid @RequestBody OnboardingRequest request) {
        return ApiResponse.success(userService.onboard(userId, request));
    }

    @Operation(summary = "내 정보 조회", description = "로그인한 회원의 프로필, 학습 현황과 관심사를 조회합니다.")
    @GetMapping
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getMe(userId));
    }

    @Operation(summary = "회원 탈퇴", description = "로그인한 회원과 관련 인증·약관·관심사 정보를 삭제합니다.")
    @DeleteMapping
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
        return ApiResponse.success();
    }

    @Operation(summary = "프로필 수정", description = "로그인한 회원의 닉네임을 변경합니다.")
    @PatchMapping("/profile")
    public ApiResponse<ProfileResponse> profile(@AuthenticationPrincipal Long userId,
                                                @Valid @RequestBody ProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(userId, request));
    }

    @Operation(summary = "학습 난이도 수정", description = "영어 학습 난이도를 BEGINNER, INTERMEDIATE, ADVANCED 중 하나로 변경합니다.")
    @PatchMapping("/eng-level")
    public ApiResponse<EnglishLevelResponse> englishLevel(@AuthenticationPrincipal Long userId,
                                                          @Valid @RequestBody EnglishLevelRequest request) {
        return ApiResponse.success(userService.updateEnglishLevel(userId, request));
    }

    @Operation(summary = "관심사 태그 수정", description = "기존 관심사 태그를 요청한 태그 목록으로 교체합니다.")
    @PutMapping("/interests")
    public ApiResponse<InterestsResponse> interests(@AuthenticationPrincipal Long userId,
                                                    @Valid @RequestBody InterestsRequest request) {
        return ApiResponse.success(userService.updateInterests(userId, request));
    }
}
