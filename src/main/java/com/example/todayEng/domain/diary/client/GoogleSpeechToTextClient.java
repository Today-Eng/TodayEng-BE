package com.example.todayEng.domain.diary.client;

public interface GoogleSpeechToTextClient {

    String transcribe(byte[] audio);
}
