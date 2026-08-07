package com.example.todayEng.global.config;

import com.example.todayEng.domain.user.entity.InterestTag;
import com.example.todayEng.domain.user.entity.enums.InterestTagName;
import com.example.todayEng.domain.user.repository.InterestTagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterestTagSeederTest {

    @Mock
    InterestTagRepository interestTagRepository;

    @Test
    void createsOnlyMissingInterestTags() {
        given(interestTagRepository.findAll()).willReturn(List.of(
                InterestTag.create(InterestTagName.CULTURE_BOOK)
        ));

        new InterestTagSeeder(interestTagRepository).seed();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InterestTag>> captor = ArgumentCaptor.forClass(List.class);
        verify(interestTagRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .hasSize(InterestTagName.values().length - 1)
                .extracting(InterestTag::getTagName)
                .doesNotContain(InterestTagName.CULTURE_BOOK);
    }

    @Test
    void doesNotInsertWhenEveryInterestTagAlreadyExists() {
        List<InterestTag> existingTags = Arrays.stream(InterestTagName.values())
                .map(InterestTag::create)
                .toList();
        given(interestTagRepository.findAll()).willReturn(existingTags);

        new InterestTagSeeder(interestTagRepository).seed();

        verify(interestTagRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }
}
