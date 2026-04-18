package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.ColorUtil;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import myau.util.font.ManagedFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class WaterMark extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String BRAND_TEXT = "MyauShadow";
    private static final int MODE_MODERN = 0;
    private static final int MODE_SIMPLE = 1;
    private static final int LOGO_HUD = 0;
    private static final int LOGO_MIX = 1;

    public final ModeProperty mode = new ModeProperty("mode", MODE_MODERN, new String[]{"Modern", "Simple"});
    public final IntProperty positionX = new IntProperty("position-x", 30, 0, 1000);
    public final IntProperty positionY = new IntProperty("position-y", 50, 0, 1000);
    public final ModeProperty logoMode = new ModeProperty("logo-mode", LOGO_HUD, new String[]{"HUD", "MIX"});
    public final ColorProperty logoColor1 = new ColorProperty("logo-color-1", 0x44BCFC, () -> this.logoMode.getValue() == LOGO_MIX);
    public final ColorProperty logoColor2 = new ColorProperty("logo-color-2", 0x3275F0, () -> this.logoMode.getValue() == LOGO_MIX);
    public final ColorProperty backgroundColor = new ColorProperty("background-color", 0x000000, () -> this.mode.getValue() == MODE_MODERN);
    public final PercentProperty backgroundAlpha = new PercentProperty("background-alpha", 40, () -> this.mode.getValue() == MODE_MODERN);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty head = new BooleanProperty("head", true, () -> this.mode.getValue() == MODE_MODERN);
    public final BooleanProperty username = new BooleanProperty("username", true, () -> this.mode.getValue() == MODE_MODERN);
    public final BooleanProperty fps = new BooleanProperty("fps", true, () -> this.mode.getValue() == MODE_MODERN);

    public WaterMark() {
        super("WaterMark", false, false);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.mode.getValue() == MODE_SIMPLE) {
            this.renderSimple();
        } else {
            this.renderModern();
        }
    }

    private void renderModern() {
        double x = this.positionX.getValue();
        double y = this.positionY.getValue();
        double height = 15.0;
        long time = System.nanoTime() / 1_000_000L;

        ManagedFont logoFont = FontManager.getManagedFont("Nunito Bold", 20);
        ManagedFont infoFont = FontManager.getManagedFont("SanFrancisco", 15);

        String versionText = this.getVersionText();
        String usernameText = this.getUsernameText();
        String fpsValue = String.valueOf(Minecraft.getDebugFPS());
        String fpsLabel = "FPS:";

        double width = 2.0;
        if (this.head.getValue()) {
            width += 11.0 + 4.0;
        }
        width += this.getStringWidth(logoFont, BRAND_TEXT);
        if (!versionText.isEmpty()) {
            width += 5.0 + this.getStringWidth(infoFont, versionText);
        }
        if (this.username.getValue() && !usernameText.isEmpty()) {
            width += 5.0 + this.getStringWidth(infoFont, usernameText);
        }
        if (this.fps.getValue()) {
            width += 5.0 + this.getStringWidth(infoFont, fpsLabel) + this.getStringWidth(infoFont, fpsValue);
        }
        width += 2.0;

        RenderUtil.enableRenderState();
        this.drawRoundedRect((float) x, (float) y, (float) width, (float) height, 1.5F, this.getBackgroundColor().getRGB());
        RenderUtil.disableRenderState();

        float drawX = (float) x + 2.0F;
        if (this.head.getValue()) {
            this.drawSelfHead(drawX, (float) y + 2.0F, 11.0F);
            drawX += 11.0F + 4.0F;
        }

        float logoY = (float) (y + height / 2.0 - this.getHeight(logoFont) / 2.0 - 0.25);
        float infoY = (float) (y + height / 2.0 - this.getHeight(infoFont) / 2.0 - 0.25);

        drawX = this.drawBrand(logoFont, drawX, logoY, time);

        if (!versionText.isEmpty()) {
            drawX += 5.0F;
            this.drawText(infoFont, versionText, drawX, infoY, -1);
            drawX += this.getStringWidth(infoFont, versionText);
        }

        if (this.username.getValue() && !usernameText.isEmpty()) {
            drawX += 5.0F;
            this.drawText(infoFont, usernameText, drawX, infoY, -1);
            drawX += this.getStringWidth(infoFont, usernameText);
        }

        if (this.fps.getValue()) {
            drawX += 5.0F;
            this.drawText(infoFont, fpsLabel, drawX, infoY, -1);
            drawX += this.getStringWidth(infoFont, fpsLabel);
            this.drawText(infoFont, fpsValue, drawX, infoY, -1);
        }
    }

    private void renderSimple() {
        double x = this.positionX.getValue();
        double y = this.positionY.getValue();
        long time = System.nanoTime() / 1_000_000L;

        ManagedFont logoFont = FontManager.getManagedFont("Nunito Bold", 20);
        ManagedFont versionFont = FontManager.getManagedFont("SanFrancisco", 15);
        String versionText = this.getVersionText();

        float logoY = (float) y;
        float drawX = (float) x;
        drawX = this.drawBrand(logoFont, drawX, logoY, time);

        if (!versionText.isEmpty()) {
            drawX += 4.0F;
            float versionY = (float) (y + this.getHeight(logoFont) / 2.0 - this.getHeight(versionFont) / 2.0);
            this.drawText(versionFont, versionText, drawX, versionY, -1);
        }
    }

    private float drawBrand(ManagedFont font, float x, float y, long time) {
        float drawX = x;
        for (int i = 0; i < BRAND_TEXT.length(); i++) {
            String letter = String.valueOf(BRAND_TEXT.charAt(i));
            int color = this.getBrandColor(time, i * 20.0D);
            this.drawText(font, letter, drawX, y, color);
            drawX += this.getStringWidth(font, letter);
        }
        return drawX;
    }

    private int getBrandColor(long time, double offset) {
        if (this.logoMode.getValue() == LOGO_HUD) {
            HUD hud = (HUD) Myau.moduleManager.getModule("HUD");
            if (hud != null) {
                return hud.getColor(time, (long) offset).getRGB();
            }
        }
        float cycle = (float) ((Math.sin((time + offset) / 280.0D) + 1.0D) * 0.5D);
        return ColorUtil.interpolate(cycle, new Color(this.logoColor1.getValue()), new Color(this.logoColor2.getValue())).getRGB();
    }

    private Color getBackgroundColor() {
        Color base = new Color(this.backgroundColor.getValue());
        int alpha = Math.round(this.backgroundAlpha.getValue() * 2.55F);
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private String getVersionText() {
        if (Myau.version == null) {
            return "";
        }
        String version = Myau.version.trim();
        return version.isEmpty() ? "" : version;
    }

    private String getUsernameText() {
        if (mc.getSession() == null) {
            return "";
        }
        String name = mc.getSession().getUsername();
        if (name == null) {
            return "";
        }
        return name.trim();
    }

    private void drawSelfHead(float x, float y, float size) {
        ResourceLocation skin = this.getSelfSkin();
        if (skin == null) {
            return;
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(skin);
        Gui.drawScaledCustomSizeModalRect(Math.round(x), Math.round(y), 8.0F, 8.0F, 8, 8, Math.round(size), Math.round(size), 64.0F, 64.0F);
    }

    private ResourceLocation getSelfSkin() {
        if (mc.thePlayer == null || mc.getNetHandler() == null || mc.thePlayer.getUniqueID() == null) {
            return null;
        }
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        return info != null ? info.getLocationSkin() : null;
    }

    private void drawText(ManagedFont font, String text, float x, float y, int color) {
        if (font == null) {
            if (this.shadow.getValue()) {
                mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
            } else {
                mc.fontRendererObj.drawString(text, x, y, color, false);
            }
            return;
        }
        if (this.shadow.getValue()) {
            font.drawStringWithShadow(text, x, y, color);
        } else {
            font.drawString(text, x, y, color);
        }
    }

    private float getStringWidth(ManagedFont font, String text) {
        if (font == null) {
            return mc.fontRendererObj.getStringWidth(text);
        }
        return font.getStringWidth(text);
    }

    private float getHeight(ManagedFont font) {
        if (font == null) {
            return mc.fontRendererObj.FONT_HEIGHT;
        }
        return font.getHeight();
    }

    private void drawCirclePart(float centerX, float centerY, float radius, int startAngle, int endAngle, int color) {
        RenderUtil.setColor(color);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(centerX, centerY);
        for (int angle = startAngle; angle <= endAngle; angle += 6) {
            double radians = Math.toRadians(angle);
            GL11.glVertex2d(centerX + Math.cos(radians) * radius, centerY + Math.sin(radians) * radius);
        }
        double endRadians = Math.toRadians(endAngle);
        GL11.glVertex2d(centerX + Math.cos(endRadians) * radius, centerY + Math.sin(endRadians) * radius);
        GL11.glEnd();
        GlStateManager.resetColor();
    }

    private void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        if (width <= 0.0F || height <= 0.0F || color == 0) {
            return;
        }
        float clampedRadius = Math.min(radius, Math.min(width, height) / 2.0F);
        if (clampedRadius <= 0.5F) {
            RenderUtil.drawRect(x, y, x + width, y + height, color);
            return;
        }
        RenderUtil.drawRect(x + clampedRadius, y, x + width - clampedRadius, y + height, color);
        RenderUtil.drawRect(x, y + clampedRadius, x + clampedRadius, y + height - clampedRadius, color);
        RenderUtil.drawRect(x + width - clampedRadius, y + clampedRadius, x + width, y + height - clampedRadius, color);
        this.drawCirclePart(x + clampedRadius, y + clampedRadius, clampedRadius, 180, 270, color);
        this.drawCirclePart(x + width - clampedRadius, y + clampedRadius, clampedRadius, 270, 360, color);
        this.drawCirclePart(x + width - clampedRadius, y + height - clampedRadius, clampedRadius, 0, 90, color);
        this.drawCirclePart(x + clampedRadius, y + height - clampedRadius, clampedRadius, 90, 180, color);
    }
}
