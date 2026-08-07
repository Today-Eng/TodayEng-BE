package com.example.todayEng.global.config;

import com.example.todayEng.global.config.seed.TermsSeedCatalog;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class TermsSeedCatalogTest {

    private static final Pattern PLACEHOLDER = Pattern.compile(
            "\\[[^]]*(업체명|국가명|링크|실제|예:|보유 기간)[^]]*]"
    );

    @Test
    void containsNoDeploymentPlaceholders() {
        assertThat(TermsSeedCatalog.values())
                .allSatisfy(seed -> assertThat(seed.content())
                        .doesNotContainPattern(PLACEHOLDER));
    }
}
