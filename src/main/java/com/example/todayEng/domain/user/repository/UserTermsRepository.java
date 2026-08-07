package com.example.todayEng.domain.user.repository;

import com.example.todayEng.domain.user.entity.UserTerms;
import com.example.todayEng.domain.user.entity.enums.TermsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserTermsRepository extends JpaRepository<UserTerms, Long> {
    Optional<UserTerms> findByUserIdAndTermsId(Long userId, Long termsId);

    @EntityGraph(attributePaths = "terms")
    List<UserTerms> findAllByUserId(Long userId);

    @Query("""
            select count(ut)
            from UserTerms ut
            where ut.user.id = :userId
              and ut.agree = true
              and ut.terms.active = true
              and ut.terms.termsType in :requiredTypes
            """)
    long countAgreedRequiredTerms(
            @Param("userId") Long userId,
            @Param("requiredTypes") Collection<TermsType> requiredTypes);
    void deleteAllByUserId(Long userId);
}
