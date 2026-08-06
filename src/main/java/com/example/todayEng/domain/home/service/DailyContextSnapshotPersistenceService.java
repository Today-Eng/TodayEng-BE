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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyContextSnapshotPersistenceService {

    private static final Duration STALE_AFTER = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final DailyContextSnapshotRepository snapshotRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SnapshotClaim start(Long userId, LocalDate date, DiaryContextType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
        DailyContextSnapshot snapshot = snapshotRepository.saveAndFlush(
                DailyContextSnapshot.start(user, date, type)
        );
        return new SnapshotClaim(snapshot.getId(), snapshot.getLeaseVersion());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<SnapshotClaim> reclaimStale(Long userId, LocalDate date, DiaryContextType type) {
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
                .map(snapshot -> new SnapshotClaim(snapshot.getId(), snapshot.getLeaseVersion()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void succeed(Long snapshotId, long leaseVersion, JsonNode data) {
        if (!finishIfOwned(snapshotId, leaseVersion, DailyContextCollectionStatus.SUCCEEDED)) {
            return;
        }
        getSnapshot(snapshotId).attachData(data);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long snapshotId, long leaseVersion) {
        finishIfOwned(snapshotId, leaseVersion, DailyContextCollectionStatus.FAILED);
    }

    private boolean finishIfOwned(
            Long snapshotId,
            long leaseVersion,
            DailyContextCollectionStatus targetStatus
    ) {
        int updated = snapshotRepository.finishIfOwned(
                snapshotId,
                targetStatus,
                DailyContextCollectionStatus.IN_PROGRESS,
                leaseVersion
        );
        if (updated == 0) {
            log.warn("Daily context snapshot lease lost: id={}, expectedLeaseVersion={}",
                    snapshotId, leaseVersion);
            return false;
        }
        return true;
    }

    private DailyContextSnapshot getSnapshot(Long snapshotId) {
        return snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENTITY_NOT_FOUND));
    }

    public record SnapshotClaim(Long id, long leaseVersion) {
    }
}
