package com.example.todayEng.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.user.entity.OAuthAuthorizationRequest;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.entity.enums.OAuthAuthorizationRequestStatus;
import com.example.todayEng.domain.user.entity.enums.OAuthCallbackFailureStage;
import com.example.todayEng.domain.user.entity.enums.OAuthCallbackFailureType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class OAuthAuthorizationRequestRepositoryTest {

    @Autowired OAuthAuthorizationRequestRepository repository;
    @Autowired UserRepository userRepository;

    @Test
    void onlyOneCallbackCanAtomicallyStartProcessing() {
        OAuthAuthorizationRequest request = issueRequest();
        LocalDateTime now = LocalDateTime.now();

        int first = repository.startProcessingIfIssued(
                request.getId(), now,
                OAuthAuthorizationRequestStatus.ISSUED,
                OAuthAuthorizationRequestStatus.PROCESSING);
        int duplicate = repository.startProcessingIfIssued(
                request.getId(), now,
                OAuthAuthorizationRequestStatus.ISSUED,
                OAuthAuthorizationRequestStatus.PROCESSING);

        assertThat(first).isEqualTo(1);
        assertThat(duplicate).isZero();
        assertThat(repository.findById(request.getId()).orElseThrow().getStatus())
                .isEqualTo(OAuthAuthorizationRequestStatus.PROCESSING);
    }

    @Test
    void recordsFailureClassificationWithoutCallbackSecrets() {
        OAuthAuthorizationRequest request = issueRequest();
        LocalDateTime now = LocalDateTime.now();
        repository.startProcessingIfIssued(
                request.getId(), now,
                OAuthAuthorizationRequestStatus.ISSUED,
                OAuthAuthorizationRequestStatus.PROCESSING);

        int updated = repository.markFailed(
                request.getId(),
                OAuthAuthorizationRequestStatus.PROCESSING,
                OAuthAuthorizationRequestStatus.FAILED,
                OAuthCallbackFailureStage.TOKEN_EXCHANGE,
                OAuthCallbackFailureType.EXTERNAL_API,
                now.plusSeconds(1));

        OAuthAuthorizationRequest failed = repository.findById(request.getId()).orElseThrow();
        assertThat(updated).isEqualTo(1);
        assertThat(failed.getStatus()).isEqualTo(OAuthAuthorizationRequestStatus.FAILED);
        assertThat(failed.getFailureStage()).isEqualTo(OAuthCallbackFailureStage.TOKEN_EXCHANGE);
        assertThat(failed.getFailureType()).isEqualTo(OAuthCallbackFailureType.EXTERNAL_API);
    }

    @Test
    void staleProcessingRequestIsFailedInsteadOfRetried() {
        OAuthAuthorizationRequest request = issueRequest();
        LocalDateTime now = LocalDateTime.now();
        repository.startProcessingIfIssued(
                request.getId(), now.minusMinutes(20),
                OAuthAuthorizationRequestStatus.ISSUED,
                OAuthAuthorizationRequestStatus.PROCESSING);

        int updated = repository.failStaleProcessing(
                OAuthAuthorizationRequestStatus.PROCESSING,
                OAuthAuthorizationRequestStatus.FAILED,
                OAuthCallbackFailureStage.STALE_PROCESSING,
                OAuthCallbackFailureType.INTERNAL,
                now.minusMinutes(15),
                now);

        OAuthAuthorizationRequest failed = repository.findById(request.getId()).orElseThrow();
        assertThat(updated).isEqualTo(1);
        assertThat(failed.getStatus()).isEqualTo(OAuthAuthorizationRequestStatus.FAILED);
        assertThat(failed.getFailureStage())
                .isEqualTo(OAuthCallbackFailureStage.STALE_PROCESSING);
    }

    private OAuthAuthorizationRequest issueRequest() {
        User user = userRepository.save(User.create());
        return repository.saveAndFlush(OAuthAuthorizationRequest.create(
                user,
                ExternalServiceProvider.SPOTIFY,
                "a".repeat(64),
                LocalDateTime.now().plusMinutes(10)));
    }
}
