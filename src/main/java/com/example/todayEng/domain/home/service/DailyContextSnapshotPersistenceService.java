package com.example.todayEng.domain.home.service;

import com.example.todayEng.domain.diary.entity.enums.DiaryContextType;
import com.example.todayEng.domain.home.entity.DailyContextSnapshot;
import com.example.todayEng.domain.home.entity.enums.DailyContextCollectionStatus;
import com.example.todayEng.domain.home.repository.DailyContextSnapshotRepository;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.repository.UserRepository;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DailyContextSnapshotPersistenceService {

    private static final Duration STALE_AFTER = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final DailyContextSnapshotRepository snapshotRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long start(Long userId, LocalDate date, DiaryContextType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
        return snapshotRepository.saveAndFlush(
                DailyContextSnapshot.start(user, date, type)
        ).getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Long> reclaimStale(Long userId, LocalDate date, DiaryContextType type) {
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = snapshotRepository.reclaimStale(
                userId,
                date,
                type,
                DailyContextCollectionStatus.IN_PROGRESS,
                now,
                now.minus(STALE_AFTER)
        );
        if (updated == 0) {
            return Optional.empty();
        }
        return snapshotRepository
                .findByUser_IdAndContextDateAndContextType(userId, date, type)
                .map(DailyContextSnapshot::getId);
    }

    @Transactional
    public void succeed(Long snapshotId, JsonNode data) {
        DailyContextSnapshot snapshot = getSnapshot(snapshotId);
        snapshot.succeed(data);
    }

    @Transactional
    public void fail(Long snapshotId) {
        DailyContextSnapshot snapshot = getSnapshot(snapshotId);
        snapshot.fail();
    }

    private DailyContextSnapshot getSnapshot(Long snapshotId) {
        return snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENTITY_NOT_FOUND));
    }
}
