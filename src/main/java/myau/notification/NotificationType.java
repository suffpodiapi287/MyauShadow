package myau.notification;

import java.awt.Color;

public enum NotificationType {
    INFO(new Color(104, 213, 134)),
    WARNING(new Color(232, 96, 96)),
    ERROR(new Color(255, 172, 76));

    private final Color color;

    NotificationType(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return this.color;
    }
}
