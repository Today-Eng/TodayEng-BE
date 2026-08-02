package com.example.todayEng.domain.diary.repository;

import com.example.todayEng.domain.diary.entity.DiaryAnswer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiaryAnswerRepository extends JpaRepository<DiaryAnswer, Long> {

    @Query("""
            select a from DiaryAnswer a
            join fetch a.question q
            join fetch q.diary d
            where a.id = :answerId
            """)
    Optional<DiaryAnswer> findDetailById(@Param("answerId") Long answerId);

    @Query("""
            select a from DiaryAnswer a
            join fetch a.question q
            join fetch q.diary d
            where d.id = :diaryId
            order by q.questionOrder asc,
                case when q.questionType = com.example.todayEng.domain.diary.entity.enums.QuestionType.MAIN
                     then 0 else 1 end asc
            """)
    java.util.List<DiaryAnswer> findAllByDiaryIdInReflectionOrder(@Param("diaryId") Long diaryId);

    boolean existsByQuestionId(Long questionId);

    Optional<DiaryAnswer> findByIdAndQuestionIdAndQuestionDiaryIdAndQuestionDiaryUserId(
            Long answerId, Long questionId, Long diaryId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DiaryAnswer a set a.transcriptionStatus =
                com.example.todayEng.domain.diary.entity.enums.TranscriptionStatus.PROCESSING,
                a.transcriptionError = null
            where a.id = :answerId and a.transcriptionStatus in (
                com.example.todayEng.domain.diary.entity.enums.TranscriptionStatus.UPLOADED,
                com.example.todayEng.domain.diary.entity.enums.TranscriptionStatus.FAILED)
            """)
    int claimTranscription(@Param("answerId") Long answerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DiaryAnswer a set a.correctionStatus =
                com.example.todayEng.domain.diary.entity.enums.CorrectionStatus.PROCESSING,
                a.correctionError = null
            where a.id = :answerId
              and a.transcriptionStatus = com.example.todayEng.domain.diary.entity.enums.TranscriptionStatus.SUCCEEDED
              and a.correctionStatus in (
                com.example.todayEng.domain.diary.entity.enums.CorrectionStatus.PENDING,
                com.example.todayEng.domain.diary.entity.enums.CorrectionStatus.FAILED)
            """)
    int claimCorrection(@Param("answerId") Long answerId);
}
