package com.example.todayEng.domain.home.repository;

import com.example.todayEng.domain.home.entity.DailyContextSnapshot;
import com.example.todayEng.domain.home.entity.enums.DailyContextCollectionStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyContextSnapshotRepository
        extends JpaRepository<DailyContextSnapshot, Long> {

    List<DailyContextSnapshot> findAllByUserIdAndContextDateAndCollectionStatus(
            Long userId,
            LocalDate contextDate,
            DailyContextCollectionStatus collectionStatus
    );
}
