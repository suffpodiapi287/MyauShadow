package myau.ui;

import myau.util.RenderUtil;
import net.minecraft.client.gui.Gui;

import java.awt.*;

public final class BlackStyle {
    public static final int BACKDROP = new Color(0, 0, 0, 135).getRGB();
    public static final int HEADER = new Color(20, 20, 20, 235).getRGB();
    public static final int BODY = new Color(40, 40, 40, 220).getRGB();
    public static final int MODULE = new Color(40, 40, 40, 235).getRGB();
    public static final int MODULE_HOVER = new Color(57, 57, 57, 235).getRGB();
    public static final int BORDER = new Color(12, 12, 12, 245).getRGB();
    public static final int TEXT = Color.WHITE.getRGB();
    public static final int TEXT_MUTED = new Color(210, 210, 210).getRGB();
    public static final int TEXT_DISABLED = new Color(150, 150, 150).getRGB();
    public static final int TRACK = new Color(225, 225, 225, 120).getRGB();
    public static final int TRACK_FILL = new Color(20, 20, 20, 255).getRGB();

    private BlackStyle() {
    }

    public static void drawBorderedRect(int left, int top, int right, int bottom, int borderColor, int fillColor) {
        Gui.drawRect(left, top, right, bottom, fillColor);
        Gui.drawRect(left, top, right, top + 1, borderColor);
        Gui.drawRect(left, bottom - 1, right, bottom, borderColor);
        Gui.drawRect(left, top, left + 1, bottom, borderColor);
        Gui.drawRect(right - 1, top, right, bottom, borderColor);
    }

    public static void drawPanelHeader(int x, int y, int width, int height, String title) {
        drawBorderedRect(x, y, x + width, y + height, BORDER, HEADER);
        ClickGuiFont.drawStringWithShadow(
                title,
                x + (width - ClickGuiFont.getWidth(title)) / 2.0F,
                y + (height - ClickGuiFont.getHeight()) / 2.0F,
                TEXT
        );
    }

    public static int blend(int first, int second, float progress) {
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        Color a = new Color(first, true);
        Color b = new Color(second, true);
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * clamped),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * clamped),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * clamped),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * clamped)
        ).getRGB();
    }

    public static String prettify(String text) {
        String normalized = text.replace('-', ' ').replace('_', ' ').trim();
        if (normalized.isEmpty()) {
            return text;
        }

        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.toString();
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void drawCircle(float x, float y, float radius, int color) {
        RenderUtil.fillCircle(x, y, radius, 24, color);
    }
}
