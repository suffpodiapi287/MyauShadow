package myau.util.font.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class FontRenderer extends CharRenderer {
    private static final char FORMATTER = '\247';
    private static final Minecraft MC = Minecraft.getMinecraft();

    public FontRenderer(Font font) {
        super(font, true, true);
    }

    public void drawString(String text, float x, float y, int color) {
        this.drawString(text, x, y, color, false);
    }

    public void drawStringWithShadow(String text, float x, float y, int color) {
        this.drawString(text, x, y, color, true);
    }

    public void drawCenteredString(String text, float x, float y, int color) {
        this.drawString(text, x - (float) this.getStringWidth(text) / 2.0F, y, color, false);
    }

    public void drawString(String text, double x, double y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return;
        }

        ScaledResolution resolution = new ScaledResolution(MC);
        int scaleFactor = Math.max(1, resolution.getScaleFactor());

        if (shadow) {
            int shadowColor = (color & 0xFCFCFC) >> 2 | color & 0xFF000000;
            this.drawString(text, x + 1.0D, y + 1.0D, shadowColor, false);
        }

        double alpha = (double) (color >> 24 & 255) / 255.0D;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;

        double renderX = (x - 1.0D) * scaleFactor;
        double renderY = (y - 3.0D) * scaleFactor - 0.2D;

        GL11.glPushMatrix();
        GL11.glScaled(1.0D / scaleFactor, 1.0D / scaleFactor, 1.0D / scaleFactor);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(red, green, blue, (float) alpha);
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(this.texture.getGlTextureId());

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);

            if (character == FORMATTER && index + 1 < text.length()) {
                index++;
                continue;
            }

            if (character < this.charData.length) {
                GL11.glBegin(GL11.GL_TRIANGLES);
                this.drawChar(this.charData, character, renderX, renderY);
                GL11.glEnd();
                renderX += this.charData[character].width - 8.3D + this.charOffset;
                continue;
            }

            GL11.glPopMatrix();

            float logicalX = (float) (renderX / scaleFactor);
            float logicalY = (float) (renderY / scaleFactor) + 3.0F;
            MC.fontRendererObj.drawString(String.valueOf(character), logicalX, logicalY, color, false);

            GL11.glPushMatrix();
            GL11.glScaled(1.0D / scaleFactor, 1.0D / scaleFactor, 1.0D / scaleFactor);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(red, green, blue, (float) alpha);
            GlStateManager.bindTexture(this.texture.getGlTextureId());

            renderX += MC.fontRendererObj.getCharWidth(character) * scaleFactor;
        }

        GlStateManager.disableBlend();
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_DONT_CARE);
        GL11.glPopMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public double getStringWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0D;
        }

        ScaledResolution resolution = new ScaledResolution(MC);
        int scaleFactor = Math.max(1, resolution.getScaleFactor());
        double width = 0.0D;

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);

            if (character == FORMATTER && index + 1 < text.length()) {
                index++;
                continue;
            }

            if (character < this.charData.length) {
                width += this.charData[character].width - 8.3D + this.charOffset;
            } else {
                width += MC.fontRendererObj.getCharWidth(character) * scaleFactor;
            }
        }

        return width / scaleFactor;
    }

    public double getHeight() {
        ScaledResolution resolution = new ScaledResolution(MC);
        int scaleFactor = Math.max(1, resolution.getScaleFactor());
        return (this.fontHeight - 8.0D) / scaleFactor;
    }
}
