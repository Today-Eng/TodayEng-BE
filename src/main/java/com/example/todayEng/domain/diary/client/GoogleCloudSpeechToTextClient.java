package com.example.todayEng.domain.diary.client;

import com.example.todayEng.domain.diary.config.GoogleSpeechProperties;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.stereotype.Component;

@Component
public class GoogleCloudSpeechToTextClient implements GoogleSpeechToTextClient {

    private final GoogleSpeechProperties properties;

    public GoogleCloudSpeechToTextClient(GoogleSpeechProperties properties) {
        this.properties = properties;
    }

    @Override
    public String transcribe(byte[] audio) {
        try (InputStream credentials = properties.credentialsLocation().getInputStream()) {
            SpeechSettings settings = SpeechSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(
                            GoogleCredentials.fromStream(credentials)))
                    .build();
            try (SpeechClient client = SpeechClient.create(settings)) {
                RecognitionConfig.Builder config = RecognitionConfig.newBuilder()
                        .setLanguageCode(properties.languageCode())
                        .setEnableAutomaticPunctuation(true)
                        .setEncoding(RecognitionConfig.AudioEncoding.valueOf(properties.audioEncoding()));
                RecognizeResponse response = client.recognize(
                        config.build(),
                        RecognitionAudio.newBuilder().setContent(ByteString.copyFrom(audio)).build()
                );
                String transcript = response.getResultsList().stream()
                        .filter(result -> result.getAlternativesCount() > 0)
                        .map(SpeechRecognitionResult::getAlternativesList)
                        .map(alternatives -> alternatives.get(0).getTranscript())
                        .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right)
                        .trim();
                if (transcript.isBlank()) {
                    throw new BaseException(ErrorCode.INVALID_STT_RESPONSE);
                }
                return transcript;
            }
        } catch (BaseException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new BaseException(ErrorCode.STT_API_FAILED);
        }
    }

}
