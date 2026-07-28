package com.example.todayEng.domain.user.entity;



import com.example.todayEng.domain.user.entity.enums.InterestCategory;
import com.example.todayEng.domain.user.entity.enums.InterestTagName;
import com.example.todayEng.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "interest_tag",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_interest_tag_name",
                        columnNames = "tag_name"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interest_tag_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_name", nullable = false, length = 50)
    private InterestTagName tagName;

    private InterestTag(InterestTagName tagName) {
        this.tagName = tagName;
    }

    public static InterestTag create(InterestTagName tagName) {
        return new InterestTag(tagName);
    }

    public String getDisplayName() {
        return tagName.getDisplayName();
    }

    public InterestCategory getCategory() {
        return tagName.getCategory();
    }

    public int getDisplayOrder() {
        return tagName.getDisplayOrder();
    }
}