package com.example.todayEng.domain.user.repository;

import com.example.todayEng.domain.user.entity.Terms;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsRepository extends JpaRepository<Terms, Long> {
}
