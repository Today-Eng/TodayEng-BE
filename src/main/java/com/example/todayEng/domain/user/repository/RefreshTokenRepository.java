package com.example.todayEng.domain.user.repository;

import com.example.todayEng.domain.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rt from RefreshToken rt join fetch rt.user where rt.jti = :jti")
    Optional<RefreshToken> findByJtiForUpdate(@Param("jti") String jti);

    void deleteByJti(String jti);
    void deleteAllByUserId(Long userId);
}
