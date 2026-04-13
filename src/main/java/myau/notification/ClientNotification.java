package myau.notification;

public final class ClientNotification {
    private static final long ANIMATION_TIME = 250L;
    private static final long DISPLAY_TIME = 2000L;

    private final String title;
    private final String message;
    private final String detail;
    private final NotificationType type;
    private final long createdAt;

    public ClientNotification(String title, String message, String detail, NotificationType type) {
        this.title = title;
        this.message = message;
        this.detail = detail;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
    }

    public String getTitle() {
        return this.title;
    }

    public String getMessage() {
        return this.message;
    }

    public String getDetail() {
        return this.detail;
    }

    public NotificationType getType() {
        return this.type;
    }

    public boolean isExpired(long now) {
        return now - this.createdAt >= DISPLAY_TIME;
    }

    public float getAnimation(long now) {
        long age = Math.max(0L, now - this.createdAt);
        if (age >= DISPLAY_TIME) {
            return 0.0F;
        }
        if (age < ANIMATION_TIME) {
            return this.easeOut(age / (float) ANIMATION_TIME);
        }
        long hideStart = DISPLAY_TIME - ANIMATION_TIME;
        if (age > hideStart) {
            return this.easeOut((DISPLAY_TIME - age) / (float) ANIMATION_TIME);
        }
        return 1.0F;
    }

    private float easeOut(float progress) {
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        float inverse = 1.0F - clamped;
        return 1.0F - inverse * inverse * inverse;
    }
}
