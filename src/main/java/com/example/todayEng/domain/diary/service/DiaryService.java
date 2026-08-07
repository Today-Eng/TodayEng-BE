package com.example.todayEng.domain.diary.service;

import com.example.todayEng.domain.diary.dto.request.DiaryStartRequest;
import com.example.todayEng.domain.diary.dto.response.DiaryStartResponse;
import com.example.todayEng.domain.diary.entity.Diary;
import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.repository.DiaryRepository;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.UserRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

// 유니크 제약 충돌 후 재조회가 새 스냅샷을 보려면 서비스 레벨 트랜잭션이 없어야 한다.
@Service
@RequiredArgsConstructor
public class DiaryService {

    private static final int DIARY_WRITABLE_PERIOD_DAYS = 7;
    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final String DIARY_USER_DATE_UNIQUE_CONSTRAINT = "uk_diary_user_date";

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;

    public DiaryStartResponse startDiary(
            Long userId,
            DiaryStartRequest request
    ) {
        LocalDate diaryDate = request.diaryDate();
        LocalDate today = LocalDate.now(SERVICE_ZONE_ID);

        validateDiaryDate(diaryDate, today);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.USER_NOT_FOUND
                ));

        Optional<Diary> existingDiary = diaryRepository.findByUserAndDiaryDate(user, diaryDate);
        if (existingDiary.isPresent()) {
            return resumeOrReject(existingDiary.get());
        }

        Diary diary = Diary.create(user, diaryDate);

        try {
            Diary savedDiary = diaryRepository.saveAndFlush(diary);
            return DiaryStartResponse.from(savedDiary);
        } catch (DataIntegrityViolationException exception) {
            if (!isDiaryUserDateUniqueViolation(exception)) {
                throw exception;
            }

            Diary concurrentlyCreated = diaryRepository.findByUserAndDiaryDate(user, diaryDate)
                    .orElseThrow(() -> new BaseException(
                            ErrorCode.DIARY_ALREADY_IN_PROGRESS
                    ));

            return resumeOrReject(concurrentlyCreated);
        }
    }

    private boolean isDiaryUserDateUniqueViolation(DataIntegrityViolationException exception) {
        if (!(exception.getCause() instanceof ConstraintViolationException constraintViolationException)) {
            return false;
        }

        String constraintName = constraintViolationException.getConstraintName();
        return constraintName != null
                && constraintName.toLowerCase().contains(DIARY_USER_DATE_UNIQUE_CONSTRAINT);
    }

    private void validateDiaryDate(
            LocalDate diaryDate,
            LocalDate today
    ) {
        if (diaryDate.isAfter(today)) {
            throw new BaseException(
                    ErrorCode.FUTURE_DIARY_DATE_NOT_ALLOWED
            );
        }

        LocalDate earliestWritableDate = today.minusDays(
                DIARY_WRITABLE_PERIOD_DAYS - 1
        );

        if (diaryDate.isBefore(earliestWritableDate)) {
            throw new BaseException(
                    ErrorCode.DIARY_DATE_EXPIRED
            );
        }
    }

    private DiaryStartResponse resumeOrReject(Diary diary) {
        if (diary.getStatus() == DiaryStatus.IN_PROGRESS) {
            return DiaryStartResponse.from(diary, true);
        }

        if (diary.getStatus() == DiaryStatus.COMPLETED) {
            throw new BaseException(
                    ErrorCode.DIARY_ALREADY_COMPLETED
            );
        }

        throw new BaseException(ErrorCode.DUPLICATE_RESOURCE);
    }
}
