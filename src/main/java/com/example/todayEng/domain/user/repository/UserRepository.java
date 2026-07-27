package com.example.todayEng.domain.user.repository;

import com.example.todayEng.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByNicknameAndIdNot(String nickname, Long id);
}
