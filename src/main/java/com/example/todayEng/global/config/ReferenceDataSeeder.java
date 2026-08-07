package com.example.todayEng.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReferenceDataSeeder implements ApplicationRunner {

    private final TermsSeeder termsSeeder;
    private final InterestTagSeeder interestTagSeeder;

    @Override
    public void run(ApplicationArguments args) {
        termsSeeder.seed();
        interestTagSeeder.seed();
    }
}
