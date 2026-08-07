package com.example.todayEng.domain.user.repository;

import com.example.todayEng.domain.user.entity.Terms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermsRepository extends JpaRepository<Terms, Long> {
    List<Terms> findAllByActiveTrue();
    Optional<Terms> findByIdAndActiveTrue(Long id);
}
