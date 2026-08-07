package com.example.todayEng.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todayEng.domain.user.dto.response.ExternalAccountListResponse;
import com.example.todayEng.domain.user.dto.response.ExternalAccountResponse;
import com.example.todayEng.domain.user.dto.response.ExternalAccountSettingsResponse;
import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.repository.ExternalAccountRepository;
import com.example.todayEng.domain.user.repository.UserRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(ExternalAccountService.class)
class ExternalAccountServiceTest {

    @Autowired UserRepository userRepository;
    @Autowired ExternalAccountRepository externalAccountRepository;
    @Autowired ExternalAccountService externalAccountService;
    @Autowired EntityManager entityManager;

    @Test
    void getExternalAccounts_연동되지_않은_provider도_함께_내려준다() {
        User user = userRepository.save(User.create());
        saveExternalAccount(user, ExternalServiceProvider.GOOGLE_CALENDAR);

        ExternalAccountListResponse response =
                externalAccountService.getExternalAccounts(user.getId());

        assertThat(response.externalAccounts())
                .extracting(ExternalAccountResponse::provider)
                .containsExactly(ExternalServiceProvider.values());

        ExternalAccountResponse spotify = findByProvider(
                response,
                ExternalServiceProvider.SPOTIFY
        );
        assertThat(spotify.connected()).isFalse();
        assertThat(spotify.useEnabled()).isFalse();
        assertThat(spotify.externalAccountId()).isNull();
        assertThat(spotify.accountIdentifier()).isNull();
        assertThat(spotify.connectedAt()).isNull();
    }

    @Test
    void getExternalAccounts_하나도_연동하지_않은_사용자도_전체_provider를_받는다() {
        User user = userRepository.save(User.create());

        ExternalAccountListResponse response =
                externalAccountService.getExternalAccounts(user.getId());

        assertThat(response.externalAccounts())
                .hasSize(ExternalServiceProvider.values().length)
                .allMatch(account -> !account.connected());
    }

    @Test
    void getExternalAccounts_연동된_provider는_계정_정보를_채워서_내려준다() {
        User user = userRepository.save(User.create());
        ExternalAccount account = saveExternalAccount(
                user,
                ExternalServiceProvider.GOOGLE_CALENDAR
        );

        ExternalAccountListResponse response =
                externalAccountService.getExternalAccounts(user.getId());

        ExternalAccountResponse googleCalendar = findByProvider(
                response,
                ExternalServiceProvider.GOOGLE_CALENDAR
        );
        assertThat(googleCalendar.connected()).isTrue();
        assertThat(googleCalendar.useEnabled()).isTrue();
        assertThat(googleCalendar.externalAccountId())
                .isEqualTo(account.getId());
        assertThat(googleCalendar.accountIdentifier())
                .isEqualTo("account@example.com");
        assertThat(googleCalendar.connectedAt()).isNotNull();
    }

    @Test
    void getExternalAccounts_다른_사용자의_연동은_보이지_않는다() {
        User owner = userRepository.save(User.create());
        User other = userRepository.save(User.create());
        saveExternalAccount(owner, ExternalServiceProvider.SPOTIFY);

        ExternalAccountListResponse response =
                externalAccountService.getExternalAccounts(other.getId());

        assertThat(response.externalAccounts())
                .allMatch(account -> !account.connected());
    }

    @Test
    void updateUseEnabled_사용_설정을_끄고_연동은_유지한다() {
        User user = userRepository.save(User.create());
        ExternalAccount account = saveExternalAccount(
                user,
                ExternalServiceProvider.SPOTIFY
        );

        ExternalAccountSettingsResponse response =
                externalAccountService.updateUseEnabled(
                        user.getId(),
                        ExternalServiceProvider.SPOTIFY,
                        false
                );

        assertThat(response.provider())
                .isEqualTo(ExternalServiceProvider.SPOTIFY);
        assertThat(response.connected()).isTrue();
        assertThat(response.useEnabled()).isFalse();

        flushAndClear();
        assertThat(externalAccountRepository.findById(account.getId()))
                .get()
                .satisfies(saved -> assertThat(saved.isUseEnabled()).isFalse());
    }

    @Test
    void updateUseEnabled_연동되지_않은_provider면_예외가_발생한다() {
        User user = userRepository.save(User.create());

        assertThatThrownBy(() ->
                externalAccountService.updateUseEnabled(
                        user.getId(),
                        ExternalServiceProvider.SPOTIFY,
                        false
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.EXTERNAL_ACCOUNT_NOT_CONNECTED);
    }

    @Test
    void updateUseEnabled_다른_사용자의_연동은_변경하지_못한다() {
        User owner = userRepository.save(User.create());
        User attacker = userRepository.save(User.create());
        ExternalAccount account = saveExternalAccount(
                owner,
                ExternalServiceProvider.SPOTIFY
        );

        assertThatThrownBy(() ->
                externalAccountService.updateUseEnabled(
                        attacker.getId(),
                        ExternalServiceProvider.SPOTIFY,
                        false
                )
        )
                .isInstanceOf(BaseException.class)
                .extracting(exception ->
                        ((BaseException) exception).getErrorCode()
                )
                .isEqualTo(ErrorCode.EXTERNAL_ACCOUNT_NOT_CONNECTED);

        flushAndClear();
        assertThat(externalAccountRepository.findById(account.getId()))
                .get()
                .satisfies(saved -> assertThat(saved.isUseEnabled()).isTrue());
    }

    @Test
    void disconnect_연동을_삭제한다() {
        User user = userRepository.save(User.create());
        ExternalAccount account = saveExternalAccount(
                user,
                ExternalServiceProvider.SPOTIFY
        );

        externalAccountService.disconnect(
                user.getId(),
                ExternalServiceProvider.SPOTIFY
        );

        flushAndClear();
        assertThat(externalAccountRepository.findById(account.getId()))
                .isEmpty();
    }

    @Test
    void disconnect_이미_해제된_상태에서_다시_호출해도_예외가_없다() {
        User user = userRepository.save(User.create());
        saveExternalAccount(user, ExternalServiceProvider.SPOTIFY);

        externalAccountService.disconnect(
                user.getId(),
                ExternalServiceProvider.SPOTIFY
        );
        flushAndClear();

        externalAccountService.disconnect(
                user.getId(),
                ExternalServiceProvider.SPOTIFY
        );

        flushAndClear();
        assertThat(externalAccountRepository.findAllByUser_Id(user.getId()))
                .isEmpty();
    }

    @Test
    void disconnect_한_번도_연동하지_않은_provider여도_예외가_없다() {
        User user = userRepository.save(User.create());

        externalAccountService.disconnect(
                user.getId(),
                ExternalServiceProvider.GOOGLE_CALENDAR
        );

        flushAndClear();
        assertThat(externalAccountRepository.findAllByUser_Id(user.getId()))
                .isEmpty();
    }

    @Test
    void disconnect_다른_사용자의_연동은_삭제하지_못한다() {
        User owner = userRepository.save(User.create());
        User attacker = userRepository.save(User.create());
        ExternalAccount account = saveExternalAccount(
                owner,
                ExternalServiceProvider.SPOTIFY
        );

        externalAccountService.disconnect(
                attacker.getId(),
                ExternalServiceProvider.SPOTIFY
        );

        flushAndClear();
        assertThat(externalAccountRepository.findById(account.getId()))
                .isPresent();
    }

    private ExternalAccount saveExternalAccount(
            User user,
            ExternalServiceProvider provider
    ) {
        return externalAccountRepository.save(
                ExternalAccount.create(
                        user,
                        provider,
                        provider.name() + "-" + user.getId(),
                        "account@example.com",
                        "access-token",
                        "refresh-token",
                        LocalDateTime.now().plusHours(1)
                )
        );
    }

    private ExternalAccountResponse findByProvider(
            ExternalAccountListResponse response,
            ExternalServiceProvider provider
    ) {
        return response.externalAccounts().stream()
                .filter(account -> account.provider() == provider)
                .findFirst()
                .orElseThrow();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
