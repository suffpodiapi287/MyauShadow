package myau.util.font.impl;

import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class CharRenderer {
    protected static final int ATLAS_SIZE = 1024;

    protected final CharData[] charData = new CharData[256];
    protected final int charOffset = 0;

    protected Font font;
    protected boolean antiAlias;
    protected boolean fractionalMetrics;
    protected int fontHeight = -1;
    protected DynamicTexture texture;

    public CharRenderer(Font font, boolean antiAlias, boolean fractionalMetrics) {
        this.font = font;
        this.antiAlias = antiAlias;
        this.fractionalMetrics = fractionalMetrics;
        this.texture = this.setupTexture(font, antiAlias, fractionalMetrics, this.charData);
    }

    public void destroy() {
        if (this.texture != null) {
            this.texture.deleteGlTexture();
            this.texture = null;
        }
    }

    protected DynamicTexture setupTexture(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
        BufferedImage image = this.generateFontImage(font, antiAlias, fractionalMetrics, chars);
        return new DynamicTexture(image);
    }

    protected BufferedImage generateFontImage(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
        BufferedImage bufferedImage = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
        graphics.setFont(font);
        graphics.setColor(new Color(255, 255, 255, 0));
        graphics.fillRect(0, 0, ATLAS_SIZE, ATLAS_SIZE);
        graphics.setColor(Color.WHITE);
        graphics.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF
        );
        graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
        );
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF
        );

        FontMetrics fontMetrics = graphics.getFontMetrics();
        int currentHeight = 0;
        int posX = 0;
        int posY = 1;

        for (int index = 0; index < chars.length; index++) {
            char character = (char) index;
            CharData data = new CharData();
            Rectangle2D bounds = fontMetrics.getStringBounds(String.valueOf(character), graphics);
            data.width = bounds.getBounds().width + 8;
            data.height = bounds.getBounds().height;

            if (posX + data.width >= ATLAS_SIZE) {
                posX = 0;
                posY += currentHeight;
                currentHeight = 0;
            }

            if (data.height > currentHeight) {
                currentHeight = data.height;
            }

            data.storedX = posX;
            data.storedY = posY;
            this.fontHeight = Math.max(this.fontHeight, data.height);
            chars[index] = data;

            graphics.drawString(String.valueOf(character), posX + 2, posY + fontMetrics.getAscent());
            posX += data.width;
        }

        graphics.dispose();
        return bufferedImage;
    }

    protected void drawChar(CharData[] chars, char character, double x, double y) {
        this.drawQuad(
                x,
                y,
                chars[character].width,
                chars[character].height,
                chars[character].storedX,
                chars[character].storedY,
                chars[character].width,
                chars[character].height
        );
    }

    protected void drawQuad(double x, double y, double width, double height, double srcX, double srcY, double srcWidth, double srcHeight) {
        float renderSrcX = (float) (srcX / (double) ATLAS_SIZE);
        float renderSrcY = (float) (srcY / (double) ATLAS_SIZE);
        float renderSrcWidth = (float) (srcWidth / (double) ATLAS_SIZE);
        float renderSrcHeight = (float) (srcHeight / (double) ATLAS_SIZE);

        GL11.glTexCoord2f(renderSrcX + renderSrcWidth, renderSrcY);
        GL11.glVertex2d(x + width, y);
        GL11.glTexCoord2f(renderSrcX, renderSrcY);
        GL11.glVertex2d(x, y);
        GL11.glTexCoord2f(renderSrcX, renderSrcY + renderSrcHeight);
        GL11.glVertex2d(x, y + height);
        GL11.glTexCoord2f(renderSrcX, renderSrcY + renderSrcHeight);
        GL11.glVertex2d(x, y + height);
        GL11.glTexCoord2f(renderSrcX + renderSrcWidth, renderSrcY + renderSrcHeight);
        GL11.glVertex2d(x + width, y + height);
        GL11.glTexCoord2f(renderSrcX + renderSrcWidth, renderSrcY);
        GL11.glVertex2d(x + width, y);
    }

    public void setAntiAlias(boolean antiAlias) {
        if (this.antiAlias != antiAlias) {
            this.antiAlias = antiAlias;
            this.destroy();
            this.texture = this.setupTexture(this.font, antiAlias, this.fractionalMetrics, this.charData);
        }
    }

    public void setFractionalMetrics(boolean fractionalMetrics) {
        if (this.fractionalMetrics != fractionalMetrics) {
            this.fractionalMetrics = fractionalMetrics;
            this.destroy();
            this.texture = this.setupTexture(this.font, this.antiAlias, fractionalMetrics, this.charData);
        }
    }

    public void setFont(Font font) {
        this.font = font;
        this.destroy();
        this.texture = this.setupTexture(font, this.antiAlias, this.fractionalMetrics, this.charData);
    }

    protected static final class CharData {
        protected int width;
        protected int height;
        protected int storedX;
        protected int storedY;
    }
}
