package com.example.todayEng.global.config;

import com.example.todayEng.domain.user.entity.Terms;
import com.example.todayEng.domain.user.entity.enums.TermsType;
import com.example.todayEng.domain.user.repository.TermsRepository;
import com.example.todayEng.global.config.seed.TermsSeedCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TermsSeeder {

    private final TermsRepository termsRepository;

    @Transactional
    public void seed() {
        Map<TermsType, Terms> savedTerms = new EnumMap<>(TermsType.class);
        termsRepository.findAll().forEach(terms -> savedTerms.put(terms.getTermsType(), terms));

        List<Terms> missingTerms = TermsSeedCatalog.values().stream()
                .filter(seed -> !savedTerms.containsKey(seed.type()))
                .map(seed -> Terms.create(seed.type(), seed.content()))
                .toList();
        if (!missingTerms.isEmpty()) {
            termsRepository.saveAll(missingTerms);
        }

        TermsSeedCatalog.values().stream()
                .filter(seed -> savedTerms.containsKey(seed.type()))
                .forEach(seed -> savedTerms.get(seed.type()).synchronize(seed.content()));
    }
}
