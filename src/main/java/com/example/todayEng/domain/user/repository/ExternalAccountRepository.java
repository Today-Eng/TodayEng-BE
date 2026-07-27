package com.example.todayEng.domain.user.repository;

import com.example.todayEng.domain.user.entity.ExternalAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalAccountRepository extends JpaRepository<ExternalAccount, Long> {
    void deleteAllByUserId(Long userId);
}
