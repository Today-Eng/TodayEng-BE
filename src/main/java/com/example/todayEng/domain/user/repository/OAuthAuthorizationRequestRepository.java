package com.example.todayEng.domain.user.repository;

import com.example.todayEng.domain.user.entity.OAuthAuthorizationRequest;
import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OAuthAuthorizationRequestRepository extends JpaRepository<OAuthAuthorizationRequest, Long> {

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
            SET request.usedAt = :usedAt
            WHERE request.id = :requestId
              AND request.usedAt IS NULL
              AND request.expiresAt > :usedAt
            """)
    int consumeIfAvailable(
            @Param("requestId") Long requestId,
            @Param("usedAt") LocalDateTime usedAt
    );

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
