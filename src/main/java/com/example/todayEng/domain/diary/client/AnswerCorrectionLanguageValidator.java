package com.example.todayEng.domain.diary.client;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AnswerCorrectionLanguageValidator {

    private static final Pattern HANGUL = Pattern.compile("[가-힣ㄱ-ㅎㅏ-ㅣ]");
    private static final Pattern LATIN = Pattern.compile("[A-Za-z]");

    public boolean isKoreanExplanation(String value) {
        return value != null && HANGUL.matcher(value).find();
    }

    public boolean isEnglishText(String value) {
        return value != null
                && LATIN.matcher(value).find()
                && !HANGUL.matcher(value).find();
    }

    public boolean areEnglishExpressions(List<String> values) {
        return values != null && values.stream().allMatch(this::isEnglishText);
    }
}
