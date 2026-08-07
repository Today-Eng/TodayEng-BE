package com.example.todayEng.global.config;

import com.example.todayEng.domain.user.entity.Terms;
import com.example.todayEng.domain.user.entity.enums.TermsType;
import com.example.todayEng.domain.user.repository.TermsRepository;
import com.example.todayEng.global.config.seed.TermsSeedCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TermsSeederTest {

    @Mock
    TermsRepository termsRepository;

    @Test
    void synchronizesExistingTermsAndCreatesOnlyMissingTerms() {
        Terms existing = Terms.create(TermsType.SERVICE_USE, "old content");
        given(termsRepository.findAll()).willReturn(List.of(existing));

        new TermsSeeder(termsRepository).seed();

        String expectedContent = TermsSeedCatalog.values().stream()
                .filter(seed -> seed.type() == TermsType.SERVICE_USE)
                .findFirst()
                .orElseThrow()
                .content();
        assertThat(existing.getContent()).isEqualTo(expectedContent);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Terms>> captor = ArgumentCaptor.forClass(List.class);
        verify(termsRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .hasSize(TermsType.values().length - 1)
                .extracting(Terms::getTermsType)
                .doesNotContain(TermsType.SERVICE_USE);
    }

    @Test
    void doesNotInsertWhenEveryTermsTypeAlreadyExists() {
        List<Terms> existingTerms = TermsSeedCatalog.values().stream()
                .map(seed -> Terms.create(seed.type(), seed.content()))
                .toList();
        given(termsRepository.findAll()).willReturn(existingTerms);

        new TermsSeeder(termsRepository).seed();

        verify(termsRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }
}
