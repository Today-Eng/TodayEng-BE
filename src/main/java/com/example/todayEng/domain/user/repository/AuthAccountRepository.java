package com.example.todayEng.domain.user.repository;

import com.example.todayEng.domain.user.entity.AuthAccount;
import com.example.todayEng.domain.user.entity.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {
    Optional<AuthAccount> findByProviderAndProviderSubject(AuthProvider provider, String providerSubject);
    void deleteAllByUserId(Long userId);
}
