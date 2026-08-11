package com.example.todayEng.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ErrorCodeTest {

    @Test
    void codesAreUnique() {
        long distinctCodes = Arrays.stream(ErrorCode.values())
                .map(ErrorCode::getCode)
                .distinct()
                .count();

        assertThat(distinctCodes).isEqualTo(ErrorCode.values().length);
    }
}
