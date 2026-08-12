package com.example.todayEng.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todayEng.domain.user.entity.OAuthAuthorizationRequest;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.entity.enums.OAuthAuthorizationRequestStatus;
import com.example.todayEng.domain.user.entity.enums.OAuthCallbackFailureStage;
import com.example.todayEng.domain.user.entity.enums.OAuthCallbackFailureType;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
class OAuthAuthorizationRequestRepositoryTest {

    @Autowired OAuthAuthorizationRequestRepository repository;
    @Autowired UserRepository userRepository;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void onlyOneCallbackCanAtomicallyStartProcessing() {
        OAuthAuthorizationRequest request = issueRequest();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

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
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        repository.startProcessingIfIssued(
                request.getId(), now.minusMinutes(15),
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

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentCallbacksAllowExactlyOneProcessingClaim() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Long requestId = transaction.execute(status -> issueRequest().getId());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = List.of(
                    executor.submit(() -> claimConcurrently(requestId, ready, start)),
                    executor.submit(() -> claimConcurrently(requestId, ready, start)));
            ready.await();
            start.countDown();

            int successfulClaims = 0;
            for (Future<Integer> result : results) {
                successfulClaims += result.get();
            }

            assertThat(successfulClaims).isEqualTo(1);
            OAuthAuthorizationRequestStatus finalStatus = transaction.execute(status ->
                    repository.findById(requestId).orElseThrow().getStatus());
            assertThat(finalStatus).isEqualTo(OAuthAuthorizationRequestStatus.PROCESSING);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void completionLockPreventsSchedulerFromChangingSucceededRequestToFailed() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Long requestId = transaction.execute(status -> {
            OAuthAuthorizationRequest request = issueRequest();
            repository.startProcessingIfIssued(
                    request.getId(), LocalDateTime.now().minusMinutes(15),
                    OAuthAuthorizationRequestStatus.ISSUED,
                    OAuthAuthorizationRequestStatus.PROCESSING);
            return request.getId();
        });
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Void> completion = executor.submit(() -> {
                transaction.executeWithoutResult(status -> {
                    OAuthAuthorizationRequest request =
                            repository.findByIdForUpdate(requestId).orElseThrow();
                    request.succeed(LocalDateTime.now());
                    locked.countDown();
                    try {
                        allowCommit.await();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }
                });
                return null;
            });
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();

            Future<Integer> recovery = executor.submit(() ->
                    transaction.execute(status -> repository.failStaleProcessing(
                            OAuthAuthorizationRequestStatus.PROCESSING,
                            OAuthAuthorizationRequestStatus.FAILED,
                            OAuthCallbackFailureStage.STALE_PROCESSING,
                            OAuthCallbackFailureType.INTERNAL,
                            LocalDateTime.now().minusMinutes(15),
                            LocalDateTime.now())));

            allowCommit.countDown();
            completion.get();
            assertThat(recovery.get()).isZero();

            OAuthAuthorizationRequestStatus finalStatus = transaction.execute(status ->
                    repository.findById(requestId).orElseThrow().getStatus());
            assertThat(finalStatus).isEqualTo(OAuthAuthorizationRequestStatus.SUCCEEDED);
        } finally {
            executor.shutdownNow();
        }
    }

    private int claimConcurrently(Long requestId, CountDownLatch ready, CountDownLatch start)
            throws Exception {
        ready.countDown();
        start.await();
        return new TransactionTemplate(transactionManager).execute(status ->
                repository.startProcessingIfIssued(
                        requestId, LocalDateTime.now(),
                        OAuthAuthorizationRequestStatus.ISSUED,
                        OAuthAuthorizationRequestStatus.PROCESSING));
    }

    private OAuthAuthorizationRequest issueRequest() {
        User user = userRepository.save(User.create());
        return repository.saveAndFlush(OAuthAuthorizationRequest.create(
                user,
                ExternalServiceProvider.SPOTIFY,
                UUID.randomUUID().toString().replace("-", "").repeat(2),
                LocalDateTime.now().plusMinutes(10)));
    }
}
