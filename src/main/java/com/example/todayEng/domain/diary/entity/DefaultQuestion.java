package com.example.todayEng.domain.diary.entity;

import com.example.todayEng.domain.user.entity.InterestTag;
import com.example.todayEng.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(name = "default_question")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DefaultQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "default_question_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_tag_id", nullable = false)
    private InterestTag interestTag;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "korean_translation", nullable = false, columnDefinition = "TEXT")
    private String koreanTranslation;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Builder(access = AccessLevel.PRIVATE)
    private DefaultQuestion(
            InterestTag interestTag,
            String questionText,
            String koreanTranslation
    ) {
        this.interestTag = interestTag;
        this.questionText = questionText;
        this.koreanTranslation = koreanTranslation;
        this.active = true;
    }

    public static DefaultQuestion create(
            InterestTag interestTag,
            String questionText,
            String koreanTranslation
    ) {
        return DefaultQuestion.builder()
                .interestTag(interestTag)
                .questionText(questionText)
                .koreanTranslation(koreanTranslation)
                .build();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
