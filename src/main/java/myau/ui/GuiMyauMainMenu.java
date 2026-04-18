package myau.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class GuiMyauMainMenu extends GuiMainMenu {
    private static final int THEME_BUTTON_ID = 14;
    private static final int MAIN_BACKGROUND_FALLBACK_WIDTH = 1920;
    private static final int MAIN_BACKGROUND_FALLBACK_HEIGHT = 1080;
    private static final ResourceLocation MAIN_LOGO = new ResourceLocation("myau", "menu/mainmenu_logo.png");
    private static final int MAIN_LOGO_FALLBACK_WIDTH = 1023;
    private static final int MAIN_LOGO_FALLBACK_HEIGHT = 633;
    private static final float MAIN_LOGO_TOP = 12.0F;
    private static final float MAIN_LOGO_WIDTH_FACTOR = 0.42F;
    private static final float MAIN_LOGO_MIN_WIDTH_FACTOR = 0.28F;
    private static final float MAIN_LOGO_MAX_WIDTH_FACTOR = 0.52F;
    private static final float LOGO_TO_MENU_GAP = 10.0F;
    private final Map<GuiButton, Boolean> hiddenButtonStates = new IdentityHashMap<GuiButton, Boolean>();
    private final Map<ResourceLocation, TextureSize> textureSizes = new HashMap<ResourceLocation, TextureSize>();

    @Override
    public void initGui() {
        super.initGui();
        replaceMainButtonsWithLiquidStyle();
        relayoutButtonsLikeLiquidBounce();
        updateThemeButtonLabel();
        clearSplashText();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        clearSplashText();
        hideMainButtonsForVanillaPass();
        super.drawScreen(mouseX, mouseY, partialTicks);
        restoreButtonsAfterVanillaPass();
        clearSplashText();

        drawMainBackground();
        drawMenuForm();
        redrawButtons(mouseX, mouseY);
        drawMainLogo();
    }

    private void relayoutButtonsLikeLiquidBounce() {
        final int baseY = getMenuBaseY();
        final int leftColumn = this.width / 2 - 100;
        final int rightColumn = this.width / 2 + 2;
        final int formLeft = this.width / 2 - 114;

        for (Object object : this.buttonList) {
            if (!(object instanceof GuiButton)) {
                continue;
            }

            GuiButton button = (GuiButton) object;

            switch (button.id) {
                case 1:
                    button.xPosition = leftColumn;
                    button.yPosition = baseY;
                    button.width = 98;
                    break;
                case 2:
                    button.xPosition = rightColumn;
                    button.yPosition = baseY;
                    button.width = 98;
                    break;
                case 6:
                    button.xPosition = leftColumn;
                    button.yPosition = baseY + 24;
                    button.width = 98;
                    break;
                case 14:
                    button.xPosition = rightColumn;
                    button.yPosition = baseY + 24;
                    button.width = 98;
                    break;
                case 0:
                    button.xPosition = leftColumn;
                    button.yPosition = baseY + 48;
                    button.width = 98;
                    break;
                case 4:
                    button.xPosition = rightColumn;
                    button.yPosition = baseY + 48;
                    button.width = 98;
                    break;
                case 5:
                    button.xPosition = formLeft - button.width - 8;
                    button.yPosition = baseY + 48;
                    break;
                default:
                    break;
            }
        }
    }

    private void drawMenuForm() {
        final int baseY = getMenuBaseY();
        final int left = this.width / 2 - 114;
        final int right = this.width / 2 + 114;
        final int top = baseY - 10;
        final int bottom = baseY + 76;

        Gui.drawRect(left, top, right, bottom, 0x66000000);
        Gui.drawRect(left + 1, top + 1, right - 1, bottom - 1, 0x3D000000);
        Gui.drawRect(left, top, right, top + 1, 0xA0000000);
        Gui.drawRect(left, bottom - 1, right, bottom, 0xA0000000);
        Gui.drawRect(left, top, left + 1, bottom, 0xA0000000);
        Gui.drawRect(right - 1, top, right, bottom, 0xA0000000);
    }

    private void redrawButtons(int mouseX, int mouseY) {
        for (Object object : this.buttonList) {
            if (!(object instanceof GuiButton)) {
                continue;
            }

            GuiButton button = (GuiButton) object;
            if (button.visible) {
                button.drawButton(this.mc, mouseX, mouseY);
            }
        }
    }

    private void drawMainBackground() {
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

    private void drawMainLogo() {
        TextureSize logoTexture = getTextureSize(
                MAIN_LOGO,
                MAIN_LOGO_FALLBACK_WIDTH,
                MAIN_LOGO_FALLBACK_HEIGHT
        );

        final float logoWidth = getLogoDrawWidth();
        final float logoHeight = logoWidth / (logoTexture.width / (float) logoTexture.height);
        final float x = (this.width - logoWidth) / 2.0F;
        final float y = MAIN_LOGO_TOP;

        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        this.mc.getTextureManager().bindTexture(MAIN_LOGO);
        Gui.drawScaledCustomSizeModalRect(
                Math.round(x),
                Math.round(y),
                0.0F,
                0.0F,
                logoTexture.width,
                logoTexture.height,
                Math.round(logoWidth),
                Math.round(logoHeight),
                logoTexture.width,
                logoTexture.height
        );

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void clearSplashText() {
        if (setSplashField("splashText") || setSplashField("field_73975_c")) {
            return;
        }

        try {
            for (Field field : GuiMainMenu.class.getDeclaredFields()) {
                if (field.getType() != String.class || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                field.setAccessible(true);
                field.set(this, "");
            }
        } catch (Throwable ignored) {
        }
    }

    private boolean setSplashField(String fieldName) {
        try {
            Field splashField = GuiMainMenu.class.getDeclaredField(fieldName);
            splashField.setAccessible(true);
            splashField.set(this, "");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private float getLogoDrawWidth() {
        float preferred = this.width * MAIN_LOGO_WIDTH_FACTOR;
        float minWidth = this.width * MAIN_LOGO_MIN_WIDTH_FACTOR;
        float maxWidth = this.width * MAIN_LOGO_MAX_WIDTH_FACTOR;
        return MathHelper.clamp_float(preferred, minWidth, maxWidth);
    }

    private float getLogoDrawHeight() {
        TextureSize logoTexture = getTextureSize(
                MAIN_LOGO,
                MAIN_LOGO_FALLBACK_WIDTH,
                MAIN_LOGO_FALLBACK_HEIGHT
        );
        return getLogoDrawWidth() / (logoTexture.width / (float) logoTexture.height);
    }

    private int getMenuBaseY() {
        int preferred = Math.round(MAIN_LOGO_TOP + getLogoDrawHeight() + LOGO_TO_MENU_GAP);
        int vanillaLike = this.height / 4 + 24;
        int baseY = Math.max(preferred, vanillaLike);
        return Math.min(baseY, this.height - 96);
    }

    private void replaceMainButtonsWithLiquidStyle() {
        for (int index = 0; index < this.buttonList.size(); index++) {
            Object object = this.buttonList.get(index);
            if (!(object instanceof GuiButton)) {
                continue;
            }

            GuiButton original = (GuiButton) object;
            if (!isMainActionButton(original.id) || original instanceof GuiLiquidStyleButton) {
                continue;
            }

            GuiLiquidStyleButton replacement = new GuiLiquidStyleButton(
                    original.id,
                    original.xPosition,
                    original.yPosition,
                    original.width,
                    original.height,
                    original.displayString
            );
            replacement.enabled = original.enabled;
            replacement.visible = original.visible;
            this.buttonList.set(index, replacement);
        }
    }

    private void hideMainButtonsForVanillaPass() {
        this.hiddenButtonStates.clear();
        for (Object object : this.buttonList) {
            if (!(object instanceof GuiButton)) {
                continue;
            }

            GuiButton button = (GuiButton) object;
            if (!isMainActionButton(button.id)) {
                continue;
            }

            this.hiddenButtonStates.put(button, button.visible);
            button.visible = false;
        }
    }

    private void restoreButtonsAfterVanillaPass() {
        for (Map.Entry<GuiButton, Boolean> entry : this.hiddenButtonStates.entrySet()) {
            entry.getKey().visible = entry.getValue();
        }
        this.hiddenButtonStates.clear();
    }

    private boolean isMainActionButton(int id) {
        return id == 1 || id == 2 || id == 6 || id == 14 || id == 0 || id == 4;
    }

    private void updateThemeButtonLabel() {
        GuiButton themeButton = getButtonById(THEME_BUTTON_ID);
        if (themeButton != null) {
            themeButton.displayString = "Theme";
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

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == THEME_BUTTON_ID) {
            this.mc.displayGuiScreen(new GuiTheme(this));
            return;
        }

        super.actionPerformed(button);
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

    private static final class GuiLiquidStyleButton extends GuiButton {
        private static final float HOVER_ANIMATION_DURATION_SECONDS = 0.18F;
        private long lastFrameTimeNanos = -1L;
        private float hoverProgress;

        private GuiLiquidStyleButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
            super(buttonId, x, y, widthIn, heightIn, buttonText);
            this.hoverProgress = 0.0F;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!this.visible) {
                return;
            }

            this.hovered = mouseX >= this.xPosition
                    && mouseY >= this.yPosition
                    && mouseX < this.xPosition + this.width
                    && mouseY < this.yPosition + this.height;

            long now = System.nanoTime();
            if (this.lastFrameTimeNanos < 0L) {
                this.lastFrameTimeNanos = now;
            }

            float deltaSeconds = (now - this.lastFrameTimeNanos) / 1_000_000_000.0F;
            this.lastFrameTimeNanos = now;
            deltaSeconds = MathHelper.clamp_float(deltaSeconds, 0.0F, 0.05F);

            float targetProgress = this.enabled && this.hovered ? 1.0F : 0.0F;
            float animationStep = deltaSeconds / HOVER_ANIMATION_DURATION_SECONDS;
            if (targetProgress > this.hoverProgress) {
                this.hoverProgress = Math.min(this.hoverProgress + animationStep, targetProgress);
            } else {
                this.hoverProgress = Math.max(this.hoverProgress - animationStep, targetProgress);
            }
            float easedProgress = easeOutCubic(this.hoverProgress);

            int left = this.xPosition;
            int top = this.yPosition;
            int right = this.xPosition + this.width;
            int bottom = this.yPosition + this.height;

            int baseTopColor = this.enabled ? 0x78000000 : 0x80505050;
            int baseBottomColor = this.enabled ? 0x78000000 : 0x80505050;
            this.drawGradientRect(left, top, right, bottom, baseTopColor, baseBottomColor);

            int hoverRight = left + Math.round(this.width * easedProgress);
            hoverRight = MathHelper.clamp_int(hoverRight, left + 1, right - 1);
            if (hoverRight > left + 1) {
                this.drawGradientRect(left + 1, top + 1, hoverRight, bottom - 1, 0xCC008B8B, 0xCC00008B);
            }

            Gui.drawRect(left, top, right, top + 1, 0xA0182738);
            Gui.drawRect(left, bottom - 1, right, bottom, 0xA0111824);
            Gui.drawRect(left, top, left + 1, bottom, 0xA0182738);
            Gui.drawRect(right - 1, top, right, bottom, 0xA0111824);

            int textColor = !this.enabled ? 0xA0A0A0 : (this.hovered ? 0xFFFFFF : 0xE6EAF2);
            this.drawCenteredString(
                    mc.fontRendererObj,
                    this.displayString,
                    this.xPosition + this.width / 2,
                    this.yPosition + (this.height - 8) / 2,
                    textColor
            );

            GlStateManager.resetColor();
        }

        private static float easeOutCubic(float value) {
            float clamped = MathHelper.clamp_float(value, 0.0F, 1.0F);
            float inverse = 1.0F - clamped;
            return 1.0F - inverse * inverse * inverse;
        }
    }
}
