package com.example.todayEng.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.todayEng.domain.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class TestAuthControllerProfileTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(AuthService.class, () -> mock(AuthService.class))
            .withUserConfiguration(Config.class);

    @Test
    void prodProfileDoesNotExposeTestLoginController() {
        contextRunner.withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).doesNotHaveBean(TestAuthController.class));
    }

    @Test
    void localProfileExposesTestLoginController() {
        contextRunner.withPropertyValues("spring.profiles.active=local")
                .run(context -> assertThat(context).hasSingleBean(TestAuthController.class));
    }

    @Configuration
    @Import(TestAuthController.class)
    static class Config {
    }
}
