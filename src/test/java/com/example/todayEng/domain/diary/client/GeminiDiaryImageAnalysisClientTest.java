package com.example.todayEng.domain.diary.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GeminiDiaryImageAnalysisClientTest {

    private final GeminiDiaryImageAnalysisClient client =
            new GeminiDiaryImageAnalysisClient(null, null, new ObjectMapper());

    @Test
    void acceptsSeparateContextsCoveringEveryImageOnce() throws Exception {
        DiaryImageAnalysis result = client.parseAnalysis(response("""
                [{"sourceImageIndexes":[0],"summary":"공원"},
                 {"sourceImageIndexes":[1],"summary":"카페"}]
                """), 2);

        assertThat(result.photoContexts()).hasSize(2);
        assertThat(result.photoContexts().get(0).sourceImageIndexes()).containsExactly(0);
        assertThat(result.photoContexts().get(1).sourceImageIndexes()).containsExactly(1);
    }

    @Test
    void acceptsMergedContextWhenItCoversBothSimilarImages() throws Exception {
        DiaryImageAnalysis result = client.parseAnalysis(response("""
                [{"sourceImageIndexes":[0,1],"summary":"같은 공원"}]
                """), 2);

        assertThat(result.photoContexts()).singleElement()
                .satisfies(context -> assertThat(context.sourceImageIndexes())
                        .containsExactly(0, 1));
    }

    @Test
    void rejectsDuplicateOrMissingImageIndexes() {
        assertThatThrownBy(() -> client.parseAnalysis(response("""
                [{"sourceImageIndexes":[0],"summary":"공원"},
                 {"sourceImageIndexes":[0],"summary":"카페"}]
                """), 2))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_LLM_RESPONSE);
    }

    private String response(String photoContexts) {
        return """
                {"photoContexts":%s}
                """.formatted(photoContexts);
    }
}
