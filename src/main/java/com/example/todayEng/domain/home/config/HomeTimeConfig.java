package com.example.todayEng.domain.home.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HomeTimeConfig {

    @Bean
    public Clock serviceClock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
