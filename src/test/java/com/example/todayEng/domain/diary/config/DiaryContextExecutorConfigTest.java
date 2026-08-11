package com.example.todayEng.domain.diary.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class DiaryContextExecutorConfigTest {

    @Test
    void rejectedTasksThrowSoServiceCanRunThemInRequestThread() {
        var configured = new DiaryContextExecutorConfig().diaryContextExecutor();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) configured;
        try {
            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                    .isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
        } finally {
            executor.shutdown();
        }
    }
}
