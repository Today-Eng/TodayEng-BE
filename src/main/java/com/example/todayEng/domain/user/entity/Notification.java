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
    }