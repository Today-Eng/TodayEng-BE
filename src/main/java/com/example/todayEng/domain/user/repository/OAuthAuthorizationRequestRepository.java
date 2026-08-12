package com.example.todayEng.domain.user.repository;

import com.example.todayEng.domain.user.entity.OAuthAuthorizationRequest;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import com.example.todayEng.domain.user.entity.enums.OAuthAuthorizationRequestStatus;
import com.example.todayEng.domain.user.entity.enums.OAuthCallbackFailureStage;
import com.example.todayEng.domain.user.entity.enums.OAuthCallbackFailureType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OAuthAuthorizationRequestRepository extends JpaRepository<OAuthAuthorizationRequest, Long> {

    long deleteAllByUserId(Long userId);

    Optional<OAuthAuthorizationRequest> findByStateHashAndProvider(
            String stateHash,
            ExternalServiceProvider provider
    );

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            UPDATE OAuthAuthorizationRequest request
            SET request.usedAt = :usedAt, request.processingStartedAt = :usedAt,
                request.status = :processing
            WHERE request.id = :requestId
              AND request.status = :issued
              AND request.expiresAt > :usedAt
            """)
    int startProcessingIfIssued(
            @Param("requestId") Long requestId,
            @Param("usedAt") LocalDateTime usedAt,
            @Param("issued") OAuthAuthorizationRequestStatus issued,
            @Param("processing") OAuthAuthorizationRequestStatus processing
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE OAuthAuthorizationRequest request
            SET request.status = :succeeded, request.completedAt = :completedAt
            WHERE request.id = :requestId AND request.status = :processing
            """)
    int markSucceeded(@Param("requestId") Long requestId,
                      @Param("processing") OAuthAuthorizationRequestStatus processing,
                      @Param("succeeded") OAuthAuthorizationRequestStatus succeeded,
                      @Param("completedAt") LocalDateTime completedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE OAuthAuthorizationRequest request
            SET request.status = :failed, request.failureStage = :stage,
                request.failureType = :failureType, request.completedAt = :completedAt
            WHERE request.id = :requestId AND request.status = :processing
            """)
    int markFailed(@Param("requestId") Long requestId,
                   @Param("processing") OAuthAuthorizationRequestStatus processing,
                   @Param("failed") OAuthAuthorizationRequestStatus failed,
                   @Param("stage") OAuthCallbackFailureStage stage,
                   @Param("failureType") OAuthCallbackFailureType failureType,
                   @Param("completedAt") LocalDateTime completedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE OAuthAuthorizationRequest request
            SET request.status = :failed, request.failureStage = :stage,
                request.failureType = :failureType, request.completedAt = :now
            WHERE request.status = :processing
              AND request.processingStartedAt < :staleBefore
            """)
    int failStaleProcessing(
            @Param("processing") OAuthAuthorizationRequestStatus processing,
            @Param("failed") OAuthAuthorizationRequestStatus failed,
            @Param("stage") OAuthCallbackFailureStage stage,
            @Param("failureType") OAuthCallbackFailureType failureType,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("now") LocalDateTime now);

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            DELETE FROM OAuthAuthorizationRequest request
            WHERE request.expiresAt < :threshold
            """)
    int deleteExpiredRequests(
            @Param("threshold") LocalDateTime threshold
    );
}
