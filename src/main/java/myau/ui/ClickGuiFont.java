package myau.ui;

import myau.Myau;
import myau.module.modules.GuiModule;
import myau.util.font.FontManager;
import myau.util.font.ManagedFont;

public final class ClickGuiFont {
    private static final int FONT_SIZE = 12;

    private ClickGuiFont() {
    }

    public static ManagedFont get() {
        GuiModule guiModule = Myau.moduleManager == null ? null : (GuiModule) Myau.moduleManager.getModule(GuiModule.class);
        String mode = guiModule == null ? "Minecraft" : guiModule.fontMode.getModeString();
        int size = guiModule == null ? FONT_SIZE : Math.max(8, Math.round(guiModule.fontSize.getValue()));
        return FontManager.getManagedFont(mode, size);
    }

    public static void drawString(String text, float x, float y, int color) {
        get().drawString(text, x, y, color);
    }

    public static void drawStringWithShadow(String text, float x, float y, int color) {
        get().drawStringWithShadow(text, x, y, color);
    }

    public static float getWidth(String text) {
        return get().getStringWidth(text);
    }

    public static float getHeight() {
        return get().getHeight();
    }
}
