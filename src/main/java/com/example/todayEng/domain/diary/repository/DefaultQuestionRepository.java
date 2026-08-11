package com.example.todayEng.domain.diary.repository;

import com.example.todayEng.domain.diary.entity.DefaultQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DefaultQuestionRepository extends JpaRepository<DefaultQuestion, Long> {

    @Query("""
            select dq from DefaultQuestion dq
            join fetch dq.interestTag tag
            where dq.active = true
              and dq.questionType = com.example.todayEng.domain.diary.entity.enums.QuestionType.MAIN
              and tag.tagName in :interestNames
            order by dq.id
            """)
    List<DefaultQuestion> findActiveMainByInterestNames(
            @Param("interestNames") List<com.example.todayEng.domain.user.entity.enums.InterestTagName> interestNames
    );

    @Query("""
            select dq from DefaultQuestion dq
            join fetch dq.interestTag
            where dq.active = true
              and dq.questionType = com.example.todayEng.domain.diary.entity.enums.QuestionType.MAIN
            order by dq.id
            """)
    List<DefaultQuestion> findAllActiveMain();

    @Query("""
            select dq from DefaultQuestion dq
            left join fetch dq.interestTag
            where dq.active = true
              and dq.questionType = com.example.todayEng.domain.diary.entity.enums.QuestionType.FOLLOW_UP
            order by dq.id
            """)
    List<DefaultQuestion> findAllActiveFollowUps();
}
