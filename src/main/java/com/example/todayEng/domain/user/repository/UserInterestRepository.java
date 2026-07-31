package com.example.todayEng.domain.user.repository;

import com.example.todayEng.domain.user.entity.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {
    List<UserInterest> findAllByUserIdOrderByInterestTagId(Long userId);
    void deleteAllByUserId(Long userId);
}
