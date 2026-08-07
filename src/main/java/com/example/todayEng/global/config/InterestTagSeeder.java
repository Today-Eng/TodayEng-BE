package com.example.todayEng.global.config;

import com.example.todayEng.domain.user.entity.InterestTag;
import com.example.todayEng.domain.user.entity.enums.InterestTagName;
import com.example.todayEng.domain.user.repository.InterestTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class InterestTagSeeder {

    private final InterestTagRepository interestTagRepository;

    @Transactional
    public void seed() {
        Set<InterestTagName> savedTagNames = EnumSet.noneOf(InterestTagName.class);
        interestTagRepository.findAll().stream()
                .map(InterestTag::getTagName)
                .forEach(savedTagNames::add);

        List<InterestTag> missingTags = Arrays.stream(InterestTagName.values())
                .filter(tagName -> !savedTagNames.contains(tagName))
                .map(InterestTag::create)
                .toList();
        if (!missingTags.isEmpty()) {
            interestTagRepository.saveAll(missingTags);
        }
    }
}
