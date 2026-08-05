package com.example.todayEng.domain.user.service;

import com.example.todayEng.domain.notification.repository.NotificationSettingRepository;
import com.example.todayEng.domain.user.dto.UserDtos.*;
import com.example.todayEng.domain.user.entity.*;
import com.example.todayEng.domain.user.entity.enums.TermsType;
import com.example.todayEng.domain.user.repository.*;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final TermsRepository termsRepository;
    private final UserTermsRepository userTermsRepository;
    private final InterestTagRepository interestTagRepository;
    private final UserInterestRepository userInterestRepository;
    private final AuthAccountRepository authAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ExternalAccountRepository externalAccountRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    @Transactional
    public void agree(Long userId, AgreementsRequest request) {
        User user = getUser(userId);
        Set<Long> ids = new HashSet<>();
        for (AgreementItem item : request.agreements()) {
            if (!ids.add(item.termId())) throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
            Terms terms = termsRepository.findById(item.termId())
                    .orElseThrow(() -> new BaseException(ErrorCode.TERMS_NOT_FOUND));
            if (terms.isRequired() && !item.agree()) throw new BaseException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
            UserTerms agreement = userTermsRepository.findByUserIdAndTermsId(userId, item.termId())
                    .orElseGet(() -> UserTerms.create(user, terms, item.agree()));
            agreement.updateAgreement(item.agree());
            userTermsRepository.save(agreement);
        }
    }

    @Transactional
    public OnboardingResponse onboard(Long userId, OnboardingRequest request) {
        User user = getUser(userId);
        validateRequiredTerms(userId);
        validateNickname(user, request.nickname());
        List<InterestTag> tags = getTags(request.interestTagIds());
        user.completeOnboarding(request.nickname(), request.profileUrl(), request.englishLevel());
        replaceInterests(user, tags);
        return onboardingResponse(user, tags);
    }

    public MeResponse getMe(Long userId) {
        User user = getUser(userId);
        return new MeResponse(user.getId(), user.getNickname(), user.getProfileUrl(),
                user.getEnglishLevel(), user.getTotalDiaryCount(), user.getCurrentStreak(),
                user.getEmail(),
                interestResponses(userInterestRepository.findAllByUserIdOrderByInterestTagId(userId)
                        .stream().map(UserInterest::getInterestTag).toList()));
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = getUser(userId);

        refreshTokenRepository.deleteAllByUserId(userId);

        userTermsRepository.deleteAllByUserId(userId);
        userInterestRepository.deleteAllByUserId(userId);

        externalAccountRepository.deleteAllByUserId(userId);
        authAccountRepository.deleteAllByUserId(userId);

        notificationSettingRepository.deleteAllByUserId(userId);

        userRepository.delete(user);
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileRequest request) {
        User user = getUser(userId);
        validateNickname(user, request.nickname());
        user.updateProfile(request.nickname(), null);
        return new ProfileResponse(user.getNickname());
    }

    @Transactional
    public EnglishLevelResponse updateEnglishLevel(Long userId, EnglishLevelRequest request) {
        User user = getUser(userId);
        user.updateEnglishLevel(request.englishLevel());
        return new EnglishLevelResponse(user.getEnglishLevel());
    }

    @Transactional
    public InterestsResponse updateInterests(Long userId, InterestsRequest request) {
        User user = getUser(userId);
        List<InterestTag> tags = getTags(request.interestTagIds());
        replaceInterests(user, tags);
        return new InterestsResponse(interestResponses(tags));
    }

    private User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateRequiredTerms(Long userId) {
        List<TermsType> requiredTypes = Arrays.stream(TermsType.values())
                .filter(TermsType::isRequired)
                .toList();
        if (userTermsRepository.countAgreedRequiredTerms(userId, requiredTypes) != requiredTypes.size())
            throw new BaseException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
    }

    private void validateNickname(User user, String nickname) {
        if (!Objects.equals(user.getNickname(), nickname) && userRepository.existsByNickname(nickname))
            throw new BaseException(ErrorCode.DUPLICATE_NICKNAME);
    }

    private List<InterestTag> getTags(List<Long> requestedIds) {
        if (requestedIds.size() != new HashSet<>(requestedIds).size())
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
        List<InterestTag> tags = interestTagRepository.findAllById(requestedIds);
        if (tags.size() != requestedIds.size()) throw new BaseException(ErrorCode.INTEREST_TAG_NOT_FOUND);
        Map<Long, InterestTag> byId = new HashMap<>();
        tags.forEach(tag -> byId.put(tag.getId(), tag));
        return requestedIds.stream().map(byId::get).toList();
    }

    private void replaceInterests(User user, List<InterestTag> tags) {
        userInterestRepository.deleteAllByUserId(user.getId());
        userInterestRepository.flush();
        userInterestRepository.saveAll(tags.stream().map(tag -> UserInterest.create(user, tag)).toList());
    }

    private OnboardingResponse onboardingResponse(User user, List<InterestTag> tags) {
        return new OnboardingResponse(user.getId(), user.getNickname(), user.getProfileUrl(),
                user.getEnglishLevel(), interestResponses(tags));
    }

    private List<InterestResponse> interestResponses(List<InterestTag> tags) {
        return tags.stream().map(tag -> new InterestResponse(tag.getId(), tag.getTagName().name())).toList();
    }
}
