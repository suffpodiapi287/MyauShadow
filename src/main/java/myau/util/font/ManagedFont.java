package myau.util.font;

public interface ManagedFont {
    void drawString(String text, float x, float y, int color);

    void drawStringWithShadow(String text, float x, float y, int color);

    float getStringWidth(String text);

    float getHeight();

    default void drawCenteredString(String text, float x, float y, int color) {
        this.drawString(text, x - this.getStringWidth(text) / 2.0F, y, color);
    }

    default void drawCenteredStringWithShadow(String text, float x, float y, int color) {
        this.drawStringWithShadow(text, x - this.getStringWidth(text) / 2.0F, y, color);
    }
}
