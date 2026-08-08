package com.example.todayEng.global.config;

import com.example.todayEng.domain.user.entity.Terms;
import com.example.todayEng.domain.user.entity.enums.TermsType;
import com.example.todayEng.domain.user.repository.TermsRepository;
import com.example.todayEng.global.config.seed.TermsSeedCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TermsSeeder {

    private final TermsRepository termsRepository;

    @Transactional
    public void seed() {
        List<Terms> savedTerms = termsRepository.findAll();

        List<Terms> missingTerms = TermsSeedCatalog.values().stream()
                .filter(seed -> savedTerms.stream()
                        .noneMatch(terms -> terms.getTermsType() == seed.type()))
                .map(seed -> Terms.create(seed.type(), seed.content(), seed.version()))
                .toList();
        if (!missingTerms.isEmpty()) {
            termsRepository.saveAll(missingTerms);
        }

        // 같은 타입이 중복 생성된 경우 최초 버전을 기준 데이터로 복구한다.
        TermsSeedCatalog.values().forEach(seed -> {
            List<Terms> termsOfType = savedTerms.stream()
                    .filter(terms -> terms.getTermsType() == seed.type())
                    .sorted(Comparator.comparingInt(Terms::getVersion))
                    .toList();
            if (!termsOfType.isEmpty()) {
                termsOfType.get(0).activate();
                termsOfType.stream().skip(1).forEach(Terms::deactivate);
            }
        });
    }
}
