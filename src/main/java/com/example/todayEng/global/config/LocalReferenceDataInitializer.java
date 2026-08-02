package com.example.todayEng.global.config;

import com.example.todayEng.domain.user.entity.InterestTag;
import com.example.todayEng.domain.user.entity.Terms;
import com.example.todayEng.domain.user.entity.enums.InterestTagName;
import com.example.todayEng.domain.user.entity.enums.TermsType;
import com.example.todayEng.domain.user.repository.InterestTagRepository;
import com.example.todayEng.domain.user.repository.TermsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Profile("local")
@Component
@RequiredArgsConstructor
public class LocalReferenceDataInitializer implements ApplicationRunner {
    private final TermsRepository termsRepository;
    private final InterestTagRepository interestTagRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (termsRepository.count() == 0) {
            termsRepository.saveAll(Arrays.stream(TermsType.values())
                    .map(type -> Terms.create(type, type.getDisplayName()))
                    .toList());
        }
        if (interestTagRepository.count() == 0) {
            interestTagRepository.saveAll(Arrays.stream(InterestTagName.values())
                    .map(InterestTag::create)
                    .toList());
        }
    }
}
