package myau.notification;

import myau.Myau;
import myau.module.modules.HUD;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class NotificationManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final List<ClientNotification> NOTIFICATIONS = new ArrayList<>();
    private static final float MIN_WIDTH = 96.0F;
    private static final float HEIGHT = 28.0F;
    private static final float HORIZONTAL_PADDING = 1.0F;
    private static final float VERTICAL_PADDING = 6.0F;
    private static final float GAP = 4.0F;
    private static final float ACCENT_BAR_HEIGHT = 1.5F;
    private static final int MAX_NOTIFICATIONS = 6;
    private static final Color BACKGROUND_COLOR = new Color(10, 12, 18, 145);
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 45);

    private NotificationManager() {
    }

    public static void addModuleToggle(String moduleName, boolean enabled) {
        addNotification(
                "Toggled Module",
                moduleName,
                enabled ? "Enabled" : "Disabled",
                enabled ? NotificationType.INFO : NotificationType.WARNING
        );
    }

    public static void addNotification(String title, String message, String detail, NotificationType type) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        synchronized (NOTIFICATIONS) {
            if (NOTIFICATIONS.size() >= MAX_NOTIFICATIONS) {
                NOTIFICATIONS.remove(0);
            }
            NOTIFICATIONS.add(new ClientNotification(title, message, detail, type));
        }
    }

    public static void render() {
        if (mc.thePlayer == null || mc.theWorld == null || mc.gameSettings.showDebugInfo) {
            return;
        }

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        long now = System.currentTimeMillis();
        float offset = 0.0F;

        synchronized (NOTIFICATIONS) {
            for (int i = NOTIFICATIONS.size() - 1; i >= 0; i--) {
                ClientNotification notification = NOTIFICATIONS.get(i);
                if (notification.isExpired(now)) {
                    NOTIFICATIONS.remove(i);
                    continue;
                }

                float animation = notification.getAnimation(now);
                if (animation <= 0.0F) {
                    continue;
                }

                float width = getWidth(notification);
                float right = scaledResolution.getScaledWidth() - HORIZONTAL_PADDING + (1.0F - animation) * (width + 12.0F);
                float left = right - width;
                float bottom = scaledResolution.getScaledHeight() - VERTICAL_PADDING + offset - (1.0F - animation) * 6.0F;
                float top = bottom - HEIGHT;

                drawNotification(left, top, right, bottom, notification);
                offset -= (HEIGHT + GAP) * animation;
            }
        }
    }

    private static float getWidth(ClientNotification notification) {
        FontRenderer fallback = mc.fontRendererObj;
        if (fallback == null) {
            return MIN_WIDTH;
        }

        String title = "\u00A7l" + safe(notification.getTitle());
        String message = safe(notification.getMessage());
        String detail = safe(notification.getDetail());
        float titleWidth = fallback.getStringWidth(title);
        float bodyWidth = fallback.getStringWidth(message);
        if (!detail.isEmpty()) {
            bodyWidth += 3.0F + fallback.getStringWidth(detail);
        }
        return Math.max(MIN_WIDTH, Math.max(titleWidth, bodyWidth) + 18.0F);
    }

    private static void drawNotification(float left, float top, float right, float bottom, ClientNotification notification) {
        Color accent = getHudAccentColor();
        int accentBarColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 210).getRGB();
        FontRenderer fontRenderer = mc.fontRendererObj;
        if (fontRenderer == null) {
            return;
        }

        RenderUtil.enableRenderState();
        RenderUtil.drawRect(left + 0.8F, top + 0.8F, right + 0.8F, bottom + 0.8F, SHADOW_COLOR.getRGB());
        RenderUtil.drawRect(left, top, right, bottom, BACKGROUND_COLOR.getRGB());
        RenderUtil.drawRect(left, top, right, top + ACCENT_BAR_HEIGHT, accentBarColor);
        RenderUtil.disableRenderState();

        String title = "\u00A7l" + safe(notification.getTitle());
        String message = safe(notification.getMessage());
        String detail = safe(notification.getDetail());
        float textX = left + 6.0F;

        GlStateManager.disableDepth();
        fontRenderer.drawStringWithShadow(title, textX, top + 4.0F, Color.WHITE.getRGB());
        fontRenderer.drawStringWithShadow(message, textX, top + 15.0F, Color.WHITE.getRGB());
        if (!detail.isEmpty()) {
            float detailX = textX + fontRenderer.getStringWidth(message) + 3.0F;
            fontRenderer.drawStringWithShadow(detail, detailX, top + 15.0F, notification.getType().getColor().getRGB());
        }
        GlStateManager.enableDepth();
    }

    private static Color getHudAccentColor() {
        if (Myau.moduleManager != null) {
            HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);
            if (hud != null) {
                return hud.getColor(System.currentTimeMillis());
            }
        }
        return Color.WHITE;
    }

    private static String safe(String text) {
        return text == null ? "" : stripFormatting(text);
    }

    private static String stripFormatting(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(text.length());
        boolean formattingCode = false;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (formattingCode) {
                formattingCode = false;
                continue;
            }
            if (character == '\u00A7') {
                formattingCode = true;
                continue;
            }
            builder.append(character);
        }
        return builder.toString();
    }
}
