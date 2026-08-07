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
    void createsNewImmutableVersionsAndDeactivatesPreviousVersions() {
        Terms previous = Terms.create(TermsType.SERVICE_USE, "previous content", 1);
        given(termsRepository.findAll()).willReturn(List.of(previous));

        new TermsSeeder(termsRepository).seed();

        assertThat(previous.isActive()).isFalse();
        assertThat(previous.getContent()).isEqualTo("previous content");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Terms>> captor = ArgumentCaptor.forClass(List.class);
        verify(termsRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .hasSize(TermsType.values().length)
                .allMatch(Terms::isActive)
                .allMatch(terms -> terms.getVersion() == 2);
    }

    @Test
    void doesNotInsertWhenEveryCurrentVersionAlreadyExists() {
        List<Terms> currentTerms = TermsSeedCatalog.values().stream()
                .map(seed -> Terms.create(seed.type(), seed.content(), seed.version()))
                .toList();
        given(termsRepository.findAll()).willReturn(currentTerms);

        new TermsSeeder(termsRepository).seed();

        verify(termsRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        assertThat(currentTerms).allMatch(Terms::isActive);
    }
}
