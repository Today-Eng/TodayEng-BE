package com.example.todayEng.domain.diary.event;

import com.example.todayEng.domain.diary.storage.AudioFileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DiaryAudioCleanupEventListener {

    private final AudioFileStorage audioFileStorage;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAudioCleanup(DiaryAudioCleanupEvent event) {
        event.audioKeys().forEach(audioFileStorage::deleteQuietly);
    }
}
