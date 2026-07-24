package com.example.todayEng.domain.user.entity;

import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "user_interest",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_interest_user_tag",
                        columnNames = {"user_id", "interest_tag_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInterest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_interest_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_interest_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "interest_tag_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_interest_tag")
    )
    private InterestTag interestTag;

    private UserInterest(User user, InterestTag interestTag) {
        this.user = user;
        this.interestTag = interestTag;
    }

    public static UserInterest create(
            User user,
            InterestTag interestTag
    ) {
        return new UserInterest(user, interestTag);
    }
}