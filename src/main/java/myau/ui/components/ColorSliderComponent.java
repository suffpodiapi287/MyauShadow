package myau.ui.components;

import myau.property.properties.ColorProperty;
import myau.ui.BlackStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;

public class ColorSliderComponent extends BlackSettingComponent {
    private final ColorProperty property;
    private boolean draggingHue;
    private boolean draggingSat;
    private boolean draggingBri;
    private float hue;
    private float saturation;
    private float brightness;

    public ColorSliderComponent(ColorProperty property, ModuleComponent parentModule, int offsetY) {
        super(parentModule, offsetY);
        this.property = property;

        Color color = new Color(property.getValue());
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    @Override
    public void draw(AtomicInteger offset) {
        int x = left();
        int y = this.y;
        int width = innerWidth();

        requestWidth(Math.max(146, Minecraft.getMinecraft().fontRendererObj.getStringWidth(displayName(property.getName())) + 20));
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(displayName(property.getName()), x, y + 2, BlackStyle.TEXT);

        if (!draggingHue && !draggingSat && !draggingBri) {
            Color color = new Color(property.getValue());
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
            hue = hsb[0];
            saturation = hsb[1];
            brightness = hsb[2];
        }

        int colorPreviewSize = 8;
        int colorPreviewX = x + width - colorPreviewSize;
        int colorPreviewY = y + 2;
        int previewColor = Color.HSBtoRGB(hue, saturation, brightness);
        BlackStyle.drawBorderedRect(colorPreviewX - 2, colorPreviewY, colorPreviewX + colorPreviewSize, colorPreviewY + colorPreviewSize, BlackStyle.BORDER, previewColor);

        int baseY = y + 13;
        int satY = baseY + 6;
        int briY = satY + 6;
        drawHueBar(x, baseY, width);
        drawPointer(x, baseY, width, hue);
        drawGradientRect(x, satY, x + width, satY + 4, Color.WHITE.getRGB(), Color.getHSBColor(hue, 1f, 1f).getRGB());
        drawPointer(x, satY, width, saturation);
        drawGradientRect(x, briY, x + width, briY + 4, Color.BLACK.getRGB(), Color.getHSBColor(hue, saturation, 1f).getRGB());
        drawPointer(x, briY, width, brightness);
    }

    private void drawHueBar(int x, int y, int width) {
        for (int i = 0; i < width; i++) {
            float hue = (float) i / (float) width;
            int color = Color.HSBtoRGB(hue, 1f, 1f);
            Gui.drawRect(x + i, y, x + i + 1, y + 4, color);
        }
    }

    private void drawPointer(int x, int y, int width, float value) {
        int posX = x + (int) (width * value);
        Gui.drawRect(posX - 1, y, posX + 1, y + 4, new Color(0, 0, 0, 200).getRGB());
    }

    @Override
    public void update(int mouseX, int mouseY) {
        super.update(mouseX, mouseY);

        int baseX = left();
        int width = innerWidth();
        boolean changed = false;

        if (draggingHue) {
            hue = getSliderValue(mouseX, baseX, width);
            changed = true;
        }
        if (draggingSat) {
            saturation = getSliderValue(mouseX, baseX, width);
            changed = true;
        }
        if (draggingBri) {
            brightness = getSliderValue(mouseX, baseX, width);
            changed = true;
        }

        if (changed) {
            int signed = Color.HSBtoRGB(hue, saturation, brightness);
            property.setValue(new Color(signed).getRGB());
        }
    }

    private float getSliderValue(int mouseX, int startX, int width) {
        double value = Math.min(width, Math.max(0, mouseX - startX));
        return (float) roundToPrecision(value / width, 3);
    }

    private static double roundToPrecision(double value, int precision) {
        BigDecimal decimal = new BigDecimal(value);
        decimal = decimal.setScale(precision, RoundingMode.HALF_UP);
        return decimal.doubleValue();
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (button != 0 || !parentModule.panelExpand) {
            return;
        }

        int baseY = this.y + 13;
        if (isHovered(mouseX, mouseY, baseY)) {
            draggingHue = true;
        } else if (isHovered(mouseX, mouseY, baseY + 6)) {
            draggingSat = true;
        } else if (isHovered(mouseX, mouseY, baseY + 12)) {
            draggingBri = true;
        }
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
        draggingHue = false;
        draggingSat = false;
        draggingBri = false;
    }

    private boolean isHovered(int mx, int my, int sliderY) {
        int startX = left();
        int endX = startX + innerWidth();
        return mx >= startX && mx <= endX && my >= sliderY && my <= sliderY + 4;
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
    }

    @Override
    public int getHeight() {
        return 31;
    }

    private void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        float sa = (float) (startColor >> 24 & 255) / 255.0F;
        float sr = (float) (startColor >> 16 & 255) / 255.0F;
        float sg = (float) (startColor >> 8 & 255) / 255.0F;
        float sb = (float) (startColor & 255) / 255.0F;
        float ea = (float) (endColor >> 24 & 255) / 255.0F;
        float er = (float) (endColor >> 16 & 255) / 255.0F;
        float eg = (float) (endColor >> 8 & 255) / 255.0F;
        float eb = (float) (endColor & 255) / 255.0F;

        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer world = tessellator.getWorldRenderer();
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_ALPHA_TEST);
        org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        org.lwjgl.opengl.GL11.glShadeModel(org.lwjgl.opengl.GL11.GL_SMOOTH);
        world.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        world.pos(right, top, 0).color(er, eg, eb, ea).endVertex();
        world.pos(left, top, 0).color(sr, sg, sb, sa).endVertex();
        world.pos(left, bottom, 0).color(sr, sg, sb, sa).endVertex();
        world.pos(right, bottom, 0).color(er, eg, eb, ea).endVertex();
        tessellator.draw();
        org.lwjgl.opengl.GL11.glShadeModel(org.lwjgl.opengl.GL11.GL_FLAT);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_ALPHA_TEST);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
    }
}
