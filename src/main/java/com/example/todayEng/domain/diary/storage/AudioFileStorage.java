package com.example.todayEng.domain.diary.storage;

public interface AudioFileStorage {

    String store(Long diaryId, Long questionId, byte[] audio);

    String storeAnswer(Long diaryId, Long questionId, byte[] audio);

    byte[] read(String audioKey);

    String publicUrl(String audioKey);

    void deleteQuietly(String audioKey);
}
