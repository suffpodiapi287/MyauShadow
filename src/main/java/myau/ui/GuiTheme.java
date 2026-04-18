package myau.ui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class GuiTheme extends GuiScreen {
    private static final int BACK_BUTTON_ID = 0;
    private static final int THEME_BUTTON_BASE_ID = 100;
    private static final int MAIN_BACKGROUND_FALLBACK_WIDTH = 1920;
    private static final int MAIN_BACKGROUND_FALLBACK_HEIGHT = 1080;
    private final GuiScreen previousScreen;
    private final Map<ResourceLocation, TextureSize> textureSizes = new HashMap<ResourceLocation, TextureSize>();

    public GuiTheme(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        int themeCount = MenuThemeManager.getThemeCount();
        int columns = Math.min(4, Math.max(1, themeCount));
        int rows = (themeCount + columns - 1) / columns;
        int buttonWidth = 98;
        int buttonHeight = 20;
        int gapX = 4;
        int gapY = 4;

        int totalWidth = columns * buttonWidth + (columns - 1) * gapX;
        int totalHeight = rows * buttonHeight + (rows - 1) * gapY;
        int startX = this.width / 2 - totalWidth / 2;
        int startY = this.height / 2 - totalHeight / 2 - 10;

        for (int index = 0; index < themeCount; index++) {
            int column = index % columns;
            int row = index / columns;
            int x = startX + column * (buttonWidth + gapX);
            int y = startY + row * (buttonHeight + gapY);
            this.buttonList.add(new GuiButton(THEME_BUTTON_BASE_ID + index, x, y, buttonWidth, buttonHeight, ""));
        }

        this.buttonList.add(new GuiButton(BACK_BUTTON_ID, this.width / 2 - 60, startY + totalHeight + 10, 120, 20, "Back"));
        refreshThemeButtonLabels();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawThemeBackground();
        Gui.drawRect(0, 0, this.width, 24, 0x55000000);
        this.drawCenteredString(this.fontRendererObj, "Theme Selector", this.width / 2, 8, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BACK_BUTTON_ID) {
            this.mc.displayGuiScreen(this.previousScreen == null ? new GuiMyauMainMenu() : this.previousScreen);
            return;
        }

        int themeCount = MenuThemeManager.getThemeCount();
        if (button.id >= THEME_BUTTON_BASE_ID && button.id < THEME_BUTTON_BASE_ID + themeCount) {
            MenuThemeManager.setSelectedThemeIndex(button.id - THEME_BUTTON_BASE_ID);
            refreshThemeButtonLabels();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.previousScreen == null ? new GuiMyauMainMenu() : this.previousScreen);
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void refreshThemeButtonLabels() {
        int selectedIndex = MenuThemeManager.getSelectedThemeIndex();
        int themeCount = MenuThemeManager.getThemeCount();

        for (int index = 0; index < themeCount; index++) {
            GuiButton button = getButtonById(THEME_BUTTON_BASE_ID + index);
            if (button == null) {
                continue;
            }

            String label = "Theme " + (index + 1);
            button.displayString = index == selectedIndex ? "> " + label : label;
        }
    }

    private GuiButton getButtonById(int buttonId) {
        for (Object object : this.buttonList) {
            if (object instanceof GuiButton) {
                GuiButton button = (GuiButton) object;
                if (button.id == buttonId) {
                    return button;
                }
            }
        }

        return null;
    }

    private void drawThemeBackground() {
        ResourceLocation backgroundResource = MenuThemeManager.getCurrentBackground();
        TextureSize backgroundTexture = getTextureSize(
                backgroundResource,
                MAIN_BACKGROUND_FALLBACK_WIDTH,
                MAIN_BACKGROUND_FALLBACK_HEIGHT
        );

        float backgroundAspect = backgroundTexture.width / (float) backgroundTexture.height;
        float screenAspect = this.width / (float) this.height;
        int drawWidth;
        int drawHeight;

        if (screenAspect > backgroundAspect) {
            drawWidth = this.width;
            drawHeight = Math.round(this.width / backgroundAspect);
        } else {
            drawHeight = this.height;
            drawWidth = Math.round(this.height * backgroundAspect);
        }

        int x = (this.width - drawWidth) / 2;
        int y = (this.height - drawHeight) / 2;

        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        this.mc.getTextureManager().bindTexture(backgroundResource);
        boolean isUpscaling = drawWidth > backgroundTexture.width || drawHeight > backgroundTexture.height;
        int textureFilter = isUpscaling ? GL11.GL_NEAREST : GL11.GL_LINEAR;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, textureFilter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, textureFilter);
        Gui.drawScaledCustomSizeModalRect(
                x,
                y,
                0.0F,
                0.0F,
                backgroundTexture.width,
                backgroundTexture.height,
                drawWidth,
                drawHeight,
                backgroundTexture.width,
                backgroundTexture.height
        );

        GlStateManager.popMatrix();
    }

    private TextureSize getTextureSize(ResourceLocation texture, int fallbackWidth, int fallbackHeight) {
        TextureSize cached = this.textureSizes.get(texture);
        if (cached != null) {
            return cached;
        }

        int width = Math.max(1, fallbackWidth);
        int height = Math.max(1, fallbackHeight);

        boolean isRemoteTheme = MenuThemeManager.isRemoteTheme(texture);
        int[] cachedRemoteSize = MenuThemeManager.getCachedTextureSize(texture);
        if (cachedRemoteSize != null && cachedRemoteSize.length >= 2) {
            width = Math.max(1, cachedRemoteSize[0]);
            height = Math.max(1, cachedRemoteSize[1]);
        } else if (!isRemoteTheme) {
            try (InputStream stream = this.mc.getResourceManager().getResource(texture).getInputStream()) {
                BufferedImage image = ImageIO.read(stream);
                if (image != null && image.getWidth() > 0 && image.getHeight() > 0) {
                    width = image.getWidth();
                    height = image.getHeight();
                }
            } catch (Throwable ignored) {
            }
        }

        TextureSize resolved = new TextureSize(width, height);
        if (!isRemoteTheme || cachedRemoteSize != null) {
            this.textureSizes.put(texture, resolved);
        }
        return resolved;
    }

    private static final class TextureSize {
        private final int width;
        private final int height;

        private TextureSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
