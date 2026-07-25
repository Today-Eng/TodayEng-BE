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
        name = "notification_setting",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_setting_user",
                        columnNames = "user_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_setting_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_notification_setting_user"
            )
    )
    private User user;

    @Column(name = "use_enabled", nullable = false)
    private boolean useEnabled;

    @Column(name = "push_endpoint", length = 500)
    private String pushEndpoint;

    @Column(name = "p256dh_key", length = 255)
    private String p256dhKey;

    @Column(name = "auth_key", length = 255)
    private String authKey;

    private Notification(
            User user,
            boolean useEnabled
    ) {
        this.user = user;
        this.useEnabled = useEnabled;
    }

    public static Notification createDefault(User user) {
        return new Notification(user, true);
    }

    public static Notification create(
            User user,
            boolean useEnabled
    ) {
        return new Notification(user, useEnabled);
    }

    public void updateEnabled(boolean useEnabled) {
        this.useEnabled = useEnabled;
    }

    public void enable() {
        this.useEnabled = true;
    }

    public void disable() {
        this.useEnabled = false;
    }

    public void updatePushSubscription(
            String pushEndpoint,
            String p256dhKey,
            String authKey
    ) {
        this.pushEndpoint = pushEndpoint;
        this.p256dhKey = p256dhKey;
        this.authKey = authKey;
    }

    public void clearPushSubscription() {
        this.pushEndpoint = null;
        this.p256dhKey = null;
        this.authKey = null;
    }
}