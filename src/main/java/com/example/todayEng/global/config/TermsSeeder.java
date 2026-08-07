package com.example.todayEng.global.config;

import com.example.todayEng.domain.user.entity.Terms;
import com.example.todayEng.domain.user.entity.enums.TermsType;
import com.example.todayEng.domain.user.repository.TermsRepository;
import com.example.todayEng.global.config.seed.TermsSeedCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TermsSeeder {

    private final TermsRepository termsRepository;

    @Transactional
    public void seed() {
        List<Terms> savedTerms = termsRepository.findAll();
        Map<TermsKey, Terms> byKey = new HashMap<>();
        savedTerms.forEach(terms -> byKey.put(new TermsKey(terms.getTermsType(), terms.getVersion()), terms));

        TermsSeedCatalog.values().forEach(seed -> savedTerms.stream()
                .filter(terms -> terms.getTermsType() == seed.type())
                .filter(terms -> terms.getVersion() != seed.version())
                .forEach(Terms::deactivate));

        List<Terms> missingTerms = TermsSeedCatalog.values().stream()
                .filter(seed -> !byKey.containsKey(new TermsKey(seed.type(), seed.version())))
                .map(seed -> Terms.create(seed.type(), seed.content(), seed.version()))
                .toList();
        if (!missingTerms.isEmpty()) {
            termsRepository.saveAll(missingTerms);
        }

        TermsSeedCatalog.values().stream()
                .map(seed -> byKey.get(new TermsKey(seed.type(), seed.version())))
                .filter(java.util.Objects::nonNull)
                .forEach(Terms::activate);
    }

    private record TermsKey(TermsType type, int version) {
    }
}
