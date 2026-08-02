package com.example.todayEng.domain.diary.repository;

import com.example.todayEng.domain.diary.entity.DiaryQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiaryQuestionRepository extends JpaRepository<DiaryQuestion, Long> {

    long countByDiaryId(Long diaryId);

    long countByDiaryIdAndQuestionType(Long diaryId,
            com.example.todayEng.domain.diary.entity.enums.QuestionType questionType);

    @Query("""
            select q from DiaryQuestion q
            where q.diary.id = :diaryId
            order by q.questionOrder asc,
                case when q.questionType = com.example.todayEng.domain.diary.entity.enums.QuestionType.MAIN
                     then 0 else 1 end asc
            """)
    java.util.List<DiaryQuestion> findAllByDiaryIdInReflectionOrder(@Param("diaryId") Long diaryId);

    List<DiaryQuestion> findAllByDiaryIdOrderByQuestionOrder(Long diaryId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DiaryQuestion q
               set q.ttsStatus = com.example.todayEng.domain.diary.entity.enums.TtsStatus.PROCESSING,
                   q.ttsErrorMessage = null
             where q.id = :questionId
               and q.diary.id = :diaryId
               and q.diary.user.id = :userId
               and q.ttsStatus in (
                   com.example.todayEng.domain.diary.entity.enums.TtsStatus.PENDING,
                   com.example.todayEng.domain.diary.entity.enums.TtsStatus.FAILED
               )
            """)
    int claimTtsGeneration(
            @Param("userId") Long userId,
            @Param("diaryId") Long diaryId,
            @Param("questionId") Long questionId
    );

    java.util.Optional<DiaryQuestion> findByIdAndDiaryIdAndDiaryUserId(
            Long questionId,
            Long diaryId,
            Long userId
    );

    boolean existsByParentQuestionId(Long parentQuestionId);

    java.util.Optional<DiaryQuestion> findFirstByDiaryIdAndQuestionTypeAndQuestionOrderGreaterThanOrderByQuestionOrder(
            Long diaryId,
            com.example.todayEng.domain.diary.entity.enums.QuestionType questionType,
            Integer questionOrder
    );
}
