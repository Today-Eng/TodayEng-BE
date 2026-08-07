package com.example.todayEng.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.todayEng.domain.notification.repository.NotificationSettingRepository;
import com.example.todayEng.domain.user.dto.UserDtos.AgreementStatus;
import com.example.todayEng.domain.user.entity.Terms;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.UserTerms;
import com.example.todayEng.domain.user.entity.enums.TermsType;
import com.example.todayEng.domain.user.repository.AuthAccountRepository;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.domain.user.repository.InterestTagRepository;
import com.example.todayEng.domain.user.repository.RefreshTokenRepository;
import com.example.todayEng.domain.user.repository.TermsRepository;
import com.example.todayEng.domain.user.repository.UserInterestRepository;
import com.example.todayEng.domain.user.repository.UserRepository;
import com.example.todayEng.domain.user.repository.UserTermsRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserAgreementQueryServiceTest {

    private static final Long USER_ID = 1L;

    @Mock UserRepository userRepository;
    @Mock TermsRepository termsRepository;
    @Mock UserTermsRepository userTermsRepository;
    @Mock InterestTagRepository interestTagRepository;
    @Mock UserInterestRepository userInterestRepository;
    @Mock AuthAccountRepository authAccountRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock ExternalAccountRepository externalAccountRepository;
    @Mock NotificationSettingRepository notificationSettingRepository;

    UserService userService;
    User user;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                termsRepository,
                userTermsRepository,
                interestTagRepository,
                userInterestRepository,
                authAccountRepository,
                refreshTokenRepository,
                externalAccountRepository,
                notificationSettingRepository
        );
        user = User.create();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    }

    @Test
    void returnsEveryTermInDisplayOrderWithUserAgreementStatus() {
        Terms serviceUse = term(10L, TermsType.SERVICE_USE, "서비스 약관 내용");
        Terms calendar = term(20L, TermsType.CALENDAR_INFORMATION_COLLECTION, "캘린더 약관 내용");
        Terms marketing = term(30L, TermsType.MARKETING_INFORMATION_RECEIVE, "마케팅 약관 내용");
        UserTerms agreed = UserTerms.create(user, serviceUse, true);
        UserTerms disagreed = UserTerms.create(user, marketing, false);

        given(termsRepository.findAllByActiveTrue()).willReturn(List.of(marketing, calendar, serviceUse));
        given(userTermsRepository.findAllByUserId(USER_ID)).willReturn(List.of(agreed, disagreed));

        var response = userService.getAgreements(USER_ID);

        assertThat(response.allRequiredAgreed()).isFalse();
        assertThat(response.agreements()).extracting("termId")
                .containsExactly(10L, 20L, 30L);
        assertThat(response.agreements()).extracting("agreementStatus")
                .containsExactly(
                        AgreementStatus.AGREED,
                        AgreementStatus.NOT_ANSWERED,
                        AgreementStatus.DISAGREED
                );
        assertThat(response.agreements().get(0).agreedAt()).isNotNull();
        assertThat(response.agreements().get(1).agreedAt()).isNull();
        assertThat(response.agreements().get(2).agreedAt()).isNull();
    }

    @Test
    void reportsRequiredAgreementsAsIncompleteWhenOneWasNotAnswered() {
        Terms serviceUse = term(10L, TermsType.SERVICE_USE, "서비스 약관 내용");
        Terms privacy = term(20L, TermsType.PRIVACY_COLLECTION, "개인정보 약관 내용");
        UserTerms agreed = UserTerms.create(user, serviceUse, true);

        given(termsRepository.findAllByActiveTrue()).willReturn(List.of(serviceUse, privacy));
        given(userTermsRepository.findAllByUserId(USER_ID)).willReturn(List.of(agreed));

        var response = userService.getAgreements(USER_ID);

        assertThat(response.allRequiredAgreed()).isFalse();
    }

    private Terms term(Long id, TermsType type, String content) {
        Terms terms = Terms.create(type, content);
        ReflectionTestUtils.setField(terms, "id", id);
        return terms;
    }
}
