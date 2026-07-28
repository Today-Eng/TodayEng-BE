package com.example.todayEng.domain.user.repository;

import com.example.todayEng.domain.user.entity.ExternalAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalAccountRepository extends JpaRepository<ExternalAccount, Long> {

    List<ExternalAccount> findAllByUser_Id(Long userId);

    Optional<ExternalAccount> findByIdAndUser_Id(Long id, Long userId);
}
