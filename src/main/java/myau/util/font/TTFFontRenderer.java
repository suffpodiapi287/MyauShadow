package myau.util.font;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;

public class TTFFontRenderer {
    private static final Random RANDOM = new Random();
    private static final char FORMATTER = '\247';

    private final int[] colorCodes = new int[32];
    private final Font font;
    private final CharacterData[] charData = new CharacterData[256];
    private final int margin;
    private final boolean antiAlias;
    private final boolean fractionalMetrics;

    public TTFFontRenderer(Font font) {
        this(font, true, true);
    }

    public TTFFontRenderer(Font font, boolean antiAlias, boolean fractionalMetrics) {
        this.font = font;
        this.margin = 6;
        this.antiAlias = antiAlias;
        this.fractionalMetrics = fractionalMetrics;
        this.generateColors();
        this.generateTextures();
    }

    public void drawString(String text, float x, float y, int color) {
        this.renderString(text, x, y, color, false);
    }

    public void drawStringWithShadow(String text, float x, float y, int color) {
        glTranslated(0.5, 0.5, 0.0);
        this.renderString(text, x, y, color, true);
        glTranslated(-0.5, -0.5, 0.0);
        this.renderString(text, x, y, color, false);
    }

    public void drawCenteredString(String text, float x, float y, int color) {
        this.renderString(text, x - this.getWidth(text) / 2.0F, y, color, false);
    }

    public void drawCenteredStringWithShadow(String text, float x, float y, int color) {
        glTranslated(0.5, 0.5, 0.0);
        this.renderString(text, x - this.getWidth(text) / 2.0F, y, color, true);
        glTranslated(-0.5, -0.5, 0.0);
        this.renderString(text, x - this.getWidth(text) / 2.0F, y, color, false);
    }

    public float getWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        float width = 0.0F;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == FORMATTER || (i > 0 ? text.charAt(i - 1) : '.') == FORMATTER || !this.isValid(character)) {
                continue;
            }
            CharacterData characterData = this.charData[character];
            width += (characterData.width - (2 * this.margin)) / 2.0F;
        }
        return width;
    }

    public float getHeight(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        float height = 0.0F;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if ((i > 0 ? text.charAt(i - 1) : '.') == FORMATTER || character == FORMATTER || !this.isValid(character)) {
                continue;
            }
            CharacterData characterData = this.charData[character];
            height = Math.max(height, characterData.height);
        }
        return (height - this.margin) / 2.0F;
    }

    public float getHeight() {
        return this.getHeight("A");
    }

    private void generateTextures() {
        for (int i = 0; i < 256; i++) {
            char character = (char) i;
            if (this.isValid(character)) {
                this.setup(character);
            }
        }
    }

    private void setup(char character) {
        BufferedImage utilityImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D utilityGraphics = (Graphics2D) utilityImage.getGraphics();
        utilityGraphics.setFont(this.font);
        FontMetrics fontMetrics = utilityGraphics.getFontMetrics();
        Rectangle2D bounds = fontMetrics.getStringBounds(String.valueOf(character), utilityGraphics);
        BufferedImage characterImage = new BufferedImage(
                (int) StrictMath.ceil(bounds.getWidth() + (2 * this.margin)),
                (int) StrictMath.ceil(bounds.getHeight()),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D graphics = (Graphics2D) characterImage.getGraphics();
        graphics.setFont(this.font);
        graphics.setColor(new Color(255, 255, 255, 0));
        graphics.fillRect(0, 0, characterImage.getWidth(), characterImage.getHeight());
        graphics.setColor(Color.WHITE);
        if (this.antiAlias) {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        if (this.fractionalMetrics) {
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        }
        graphics.drawString(String.valueOf(character), this.margin, fontMetrics.getAscent());
        int textureId = glGenTextures();
        this.createTexture(textureId, characterImage);
        this.charData[character] = new CharacterData(characterImage.getWidth(), characterImage.getHeight(), textureId);
        utilityGraphics.dispose();
        graphics.dispose();
    }

    private void createTexture(int textureId, BufferedImage image) {
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
        }
        buffer.flip();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
    }

    private void renderString(String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();
        glScaled(0.5, 0.5, 1.0);
        x -= this.margin / 2.0F;
        y -= 2.0F;
        x *= 2.0F;
        y *= 2.0F;
        boolean underlined = false;
        boolean strikethrough = false;
        boolean obfuscated = false;
        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        float multiplier = shadow ? 4.0F : 1.0F;
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(red / multiplier, green / multiplier, blue / multiplier, alpha);
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            char previous = i > 0 ? text.charAt(i - 1) : '.';
            if (previous == FORMATTER) {
                continue;
            }
            if (character == FORMATTER && i < text.length() - 1) {
                int index = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1)));
                if (index < 16) {
                    obfuscated = false;
                    strikethrough = false;
                    underlined = false;
                    if (index < 0) {
                        index = 15;
                    }
                    if (shadow) {
                        index += 16;
                    }
                    int textColor = this.colorCodes[index];
                    glColor4f(
                            (float) (textColor >> 16 & 255) / 255.0F,
                            (float) (textColor >> 8 & 255) / 255.0F,
                            (float) (textColor & 255) / 255.0F,
                            alpha
                    );
                } else if (index == 16) {
                    obfuscated = true;
                } else if (index == 18) {
                    strikethrough = true;
                } else if (index == 19) {
                    underlined = true;
                } else {
                    obfuscated = false;
                    strikethrough = false;
                    underlined = false;
                    glColor4f(1.0F / multiplier, 1.0F / multiplier, 1.0F / multiplier, alpha);
                }
                continue;
            }
            if (!this.isValid(character)) {
                continue;
            }
            if (obfuscated) {
                character += (char) RANDOM.nextInt(Math.max(1, 256 - character));
            }
            CharacterData characterData = this.charData[character];
            this.drawChar(characterData, x, y);
            if (strikethrough) {
                this.drawLine(x, y + characterData.height / 2.0F, x + characterData.width, y + characterData.height / 2.0F, 3.0F);
            }
            if (underlined) {
                this.drawLine(x, y + characterData.height - 15.0F, x + characterData.width, y + characterData.height - 15.0F, 3.0F);
            }
            x += characterData.width - (2 * this.margin);
        }
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private boolean isValid(char character) {
        return character > 10 && character < 256 && character != 127;
    }

    private void drawChar(CharacterData characterData, float x, float y) {
        characterData.bind();
        glBegin(GL_QUADS);
        glTexCoord2f(0.0F, 0.0F);
        glVertex2d(x, y);
        glTexCoord2f(0.0F, 1.0F);
        glVertex2d(x, y + characterData.height);
        glTexCoord2f(1.0F, 1.0F);
        glVertex2d(x + characterData.width, y + characterData.height);
        glTexCoord2f(1.0F, 0.0F);
        glVertex2d(x + characterData.width, y);
        glEnd();
        GlStateManager.bindTexture(0);
    }

    private void drawLine(float x, float y, float x2, float y2, float width) {
        glDisable(GL_TEXTURE_2D);
        glLineWidth(width);
        glBegin(GL_LINES);
        glVertex2f(x, y);
        glVertex2f(x2, y2);
        glEnd();
        glEnable(GL_TEXTURE_2D);
    }

    private void generateColors() {
        for (int index = 0; index < 32; index++) {
            int shadow = (index >> 3 & 1) * 85;
            int red = (index >> 2 & 1) * 170 + shadow;
            int green = (index >> 1 & 1) * 170 + shadow;
            int blue = (index & 1) * 170 + shadow;
            if (index == 6) {
                red += 85;
            }
            if (index >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }
            this.colorCodes[index] = (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
        }
    }

    public static class CharacterData {
        private final int textureId;
        private final float width;
        private final float height;

        private CharacterData(float width, float height, int textureId) {
            this.textureId = textureId;
            this.width = width;
            this.height = height;
        }

        private void bind() {
            GlStateManager.bindTexture(this.textureId);
        }
    }
}
