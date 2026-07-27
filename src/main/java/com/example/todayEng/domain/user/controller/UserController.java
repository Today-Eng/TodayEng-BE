package com.example.todayEng.domain.user.controller;

import com.example.todayEng.domain.user.dto.UserDtos.*;
import com.example.todayEng.domain.user.service.UserService;
import com.example.todayEng.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/agreements")
    public ApiResponse<Void> agreements(@AuthenticationPrincipal Long userId,
                                        @Valid @RequestBody AgreementsRequest request) {
        userService.agree(userId, request);
        return ApiResponse.success();
    }

    @PostMapping("/onboarding")
    public ApiResponse<OnboardingResponse> onboarding(@AuthenticationPrincipal Long userId,
                                                       @Valid @RequestBody OnboardingRequest request) {
        return ApiResponse.success(userService.onboard(userId, request));
    }

    @GetMapping
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getMe(userId));
    }

    @DeleteMapping
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
        return ApiResponse.success();
    }

    @PatchMapping("/profile")
    public ApiResponse<ProfileResponse> profile(@AuthenticationPrincipal Long userId,
                                                @Valid @RequestBody ProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(userId, request));
    }

    @PatchMapping("/eng-level")
    public ApiResponse<EnglishLevelResponse> englishLevel(@AuthenticationPrincipal Long userId,
                                                          @Valid @RequestBody EnglishLevelRequest request) {
        return ApiResponse.success(userService.updateEnglishLevel(userId, request));
    }

    @PutMapping("/interests")
    public ApiResponse<InterestsResponse> interests(@AuthenticationPrincipal Long userId,
                                                    @Valid @RequestBody InterestsRequest request) {
        return ApiResponse.success(userService.updateInterests(userId, request));
    }
}
