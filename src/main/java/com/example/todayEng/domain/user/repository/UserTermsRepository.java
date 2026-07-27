package com.example.todayEng.domain.user.repository;

import com.example.todayEng.domain.user.entity.UserTerms;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserTermsRepository extends JpaRepository<UserTerms, Long> {
    Optional<UserTerms> findByUserIdAndTermsId(Long userId, Long termsId);
    void deleteAllByUserId(Long userId);
}
