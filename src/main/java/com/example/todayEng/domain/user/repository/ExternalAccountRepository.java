package com.example.todayEng.domain.user.repository;

import com.example.todayEng.domain.user.entity.ExternalAccount;
import com.example.todayEng.domain.user.entity.User;
import java.util.List;
import java.util.Optional;

import com.example.todayEng.domain.user.entity.enums.ExternalServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalAccountRepository extends JpaRepository<ExternalAccount, Long> {

    List<ExternalAccount> findAllByUser_Id(Long userId);

    void deleteAllByUserId(Long userId);

    Optional<ExternalAccount> findByUser_IdAndProvider(
            Long userId,
            ExternalServiceProvider provider
    );

    Optional<ExternalAccount> findByUserAndProvider(
            User user,
            ExternalServiceProvider provider
    );

    Optional<ExternalAccount> findByProviderAndProviderAccountId(
            ExternalServiceProvider provider,
            String providerAccountId
    );
}
