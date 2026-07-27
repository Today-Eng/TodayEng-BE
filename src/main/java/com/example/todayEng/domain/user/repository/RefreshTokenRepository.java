package com.example.todayEng.domain.user.repository;

import com.example.todayEng.domain.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    void deleteByJti(String jti);
    void deleteAllByUserId(Long userId);
}
