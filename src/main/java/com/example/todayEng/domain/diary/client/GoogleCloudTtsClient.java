package com.example.todayEng.domain.diary.client;

import com.example.todayEng.domain.diary.config.GoogleTtsProperties;
import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleCloudTtsClient implements GoogleTtsClient {

    private final GoogleTtsProperties properties;

    @Override
    public byte[] synthesize(String text) {
        try (InputStream credentialsStream = properties
                .credentialsLocation()
                .getInputStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    credentialsStream
            );
            TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                    .setCredentialsProvider(
                            FixedCredentialsProvider.create(credentials)
                    )
                    .build();

            try (TextToSpeechClient client = TextToSpeechClient.create(settings)) {
                SynthesisInput input = SynthesisInput.newBuilder()
                        .setText(text)
                        .build();
                VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                        .setLanguageCode(properties.languageCode())
                        .setName(properties.voiceName())
                        .build();
                AudioConfig audioConfig = AudioConfig.newBuilder()
                        .setAudioEncoding(resolveAudioEncoding())
                        .setSpeakingRate(properties.speakingRate())
                        .build();

                SynthesizeSpeechResponse response = client.synthesizeSpeech(
                        input,
                        voice,
                        audioConfig
                );
                return response.getAudioContent().toByteArray();
            }
        } catch (Exception exception) {
            throw new BaseException(ErrorCode.TTS_API_FAILED);
        }
    }

    private AudioEncoding resolveAudioEncoding() {
        try {
            AudioEncoding encoding = AudioEncoding.valueOf(
                    properties.audioEncoding().toUpperCase()
            );
            if (encoding != AudioEncoding.MP3) {
                throw new IllegalArgumentException("Only MP3 is supported");
            }
            return encoding;
        } catch (RuntimeException exception) {
            throw new BaseException(ErrorCode.TTS_API_FAILED);
        }
    }
}
