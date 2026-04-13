package myau.module.modules;

import myau.Myau;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.util.ColorUtil;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import myau.util.TimerUtil;
import myau.util.font.FontManager;
import myau.util.font.ManagedFont;
import myau.property.properties.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TargetHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat healthFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    private static final DecimalFormat diffFormat = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));
    private static final int MODE_MYAU = 0;
    private static final int MODE_NORMAL = 1;
    private static final int MODE_HVH = 2;
    private static final int MODE_RAVEN = 3;
    private static final int MODE_BUTT = 4;
    private static final int MODE_LEGIT = 5;
    private static final int MODE_DIABLO = 6;
    private static final int MODE_EXHIBITION = 7;
    private final TimerUtil lastAttackTimer = new TimerUtil();
    private final TimerUtil animTimer = new TimerUtil();
    private EntityLivingBase lastTarget = null;
    private EntityLivingBase target = null;
    private ResourceLocation headTexture = null;
    private float oldHealth = 0.0F;
    private float newHealth = 0.0F;
    private float maxHealth = 1.0F;
    public final ModeProperty mode = new ModeProperty(
            "mode",
            MODE_MYAU,
            new String[]{"MYAU", "NORMAL", "HVH", "RAVEN", "BUTT", "LEGIT", "DIABLO", "EXHIBITION"}
    );
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"DEFAULT", "HUD"});
    public final ModeProperty posX = new ModeProperty("position-x", 1, new String[]{"LEFT", "MIDDLE", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 1, new String[]{"TOP", "MIDDLE", "BOTTOM"});
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final FloatProperty fontSize = new FloatProperty("font-size", 16.0F, 8.0F, 30.0F);
    public final IntProperty offX = new IntProperty("offset-x", 0, -255, 255);
    public final IntProperty offY = new IntProperty("offset-y", 40, -255, 255);
    public final PercentProperty background = new PercentProperty("background", 25);
    public final ModeProperty fontMode = new ModeProperty("font-mode", FontManager.indexOfManagedFont("OpenSans Medium"), FontManager.MANAGED_FONT_MODES);
    public final BooleanProperty head = new BooleanProperty("head", true);
    public final BooleanProperty indicator = new BooleanProperty("indicator", true);
    public final BooleanProperty outline = new BooleanProperty("outline", false);
    public final BooleanProperty animations = new BooleanProperty("animations", true);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty kaOnly = new BooleanProperty("ka-only", true);
    public final BooleanProperty chatPreview = new BooleanProperty("chat-preview", false);

    private static final class LayoutMetrics {
        private final float width;
        private final float height;

        private LayoutMetrics(float width, float height) {
            this.width = width;
            this.height = height;
        }
    }

    public TargetHUD() {
        super("TargetHUD", false, true);
    }

    private EntityLivingBase resolveTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura.isEnabled() && killAura.isAttackAllowed() && TeamUtil.isEntityLoaded(killAura.getTarget())) {
            return killAura.getTarget();
        }
        if (!this.kaOnly.getValue()) {
            Entity pointedEntity = mc.pointedEntity;
            if (pointedEntity instanceof EntityLivingBase
                    && !(pointedEntity instanceof EntityArmorStand)
                    && TeamUtil.isEntityLoaded(pointedEntity)) {
                return (EntityLivingBase) pointedEntity;
            }
            if (!this.lastAttackTimer.hasTimeElapsed(1500L) && TeamUtil.isEntityLoaded(this.lastTarget)) {
                return this.lastTarget;
            }
        }
        return this.chatPreview.getValue() && mc.currentScreen instanceof GuiChat ? mc.thePlayer : null;
    }

    private ResourceLocation getSkin(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(entityLivingBase.getName());
            if (playerInfo != null) {
                return playerInfo.getLocationSkin();
            }
        }
        return null;
    }

    private Color getTargetColor(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            if (TeamUtil.isFriend((EntityPlayer) entityLivingBase)) {
                return Myau.friendManager.getColor();
            }
            if (TeamUtil.isTarget((EntityPlayer) entityLivingBase)) {
                return Myau.targetManager.getColor();
            }
        }
        switch (this.color.getValue()) {
            case 0:
                if (!(entityLivingBase instanceof EntityPlayer)) {
                    return new Color(-1);
                }
                return TeamUtil.getTeamColor((EntityPlayer) entityLivingBase, 1.0F);
            case 1:
                int rgb = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
                return new Color(rgb);
            default:
                return new Color(-1);
        }
    }

    private boolean shouldRenderHead() {
        return this.head.getValue() && this.headTexture != null;
    }

    private float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private int scaledAlpha(int minimum, int maximum) {
        return minimum + Math.round((maximum - minimum) * (this.background.getValue() / 100.0F));
    }

    private String getOutcomeLetter(float playerHealth, float targetHealth, String evenLetter) {
        if (playerHealth > targetHealth) {
            return "W";
        }
        if (playerHealth < targetHealth) {
            return "L";
        }
        return evenLetter;
    }

    private int getOutcomeColor(float playerHealth, float targetHealth) {
        if (playerHealth > targetHealth) {
            return new Color(90, 255, 90).getRGB();
        }
        if (playerHealth < targetHealth) {
            return new Color(255, 90, 90).getRGB();
        }
        return new Color(255, 215, 90).getRGB();
    }

    private int resolveFontSize(int defaultSize) {
        return Math.max(8, Math.round(defaultSize * (this.fontSize.getValue() / 16.0F)));
    }

    private ManagedFont getFont(int size) {
        return FontManager.getManagedFont(this.fontMode.getModeString(), this.resolveFontSize(size));
    }

    private float getTextWidth(ManagedFont font, String text) {
        if (font != null) {
            return font.getStringWidth(text);
        }
        return mc.fontRendererObj.getStringWidth(text);
    }

    private float getTextHeight(ManagedFont font, String text) {
        if (font != null) {
            return font.getHeight();
        }
        return mc.fontRendererObj.FONT_HEIGHT;
    }

    private void drawText(ManagedFont font, String text, float x, float y, int color) {
        if (font != null) {
            if (this.shadow.getValue()) {
                font.drawStringWithShadow(text, x, y, color);
            } else {
                font.drawString(text, x, y, color);
            }
            return;
        }
        if (this.shadow.getValue()) {
            mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
        } else {
            mc.fontRendererObj.drawString(text, x, y, color, false);
        }
    }

    private void drawCenteredText(ManagedFont font, String text, float centerX, float y, int color) {
        this.drawText(font, text, centerX - this.getTextWidth(font, text) / 2.0F, y, color);
    }

    private String truncateText(ManagedFont font, String text, float maxWidth) {
        if (text == null || text.isEmpty() || this.getTextWidth(font, text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        String trimmed = text;
        while (!trimmed.isEmpty() && this.getTextWidth(font, trimmed + ellipsis) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ellipsis;
    }

    private void drawOutlineBox(float x, float y, float width, float height, int backgroundColor, int outlineColor) {
        RenderUtil.drawRect(x, y, x + width, y + height, backgroundColor);
        if (outlineColor != 0) {
            RenderUtil.drawRect(x, y, x + width, y + 1.0F, outlineColor);
            RenderUtil.drawRect(x, y + height - 1.0F, x + width, y + height, outlineColor);
            RenderUtil.drawRect(x, y, x + 1.0F, y + height, outlineColor);
            RenderUtil.drawRect(x + width - 1.0F, y, x + width, y + height, outlineColor);
        }
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

    private void drawGlowLayers(float x, float y, float width, float height, float radius, Color color) {
        Color outer = new Color(color.getRed(), color.getGreen(), color.getBlue(), 28);
        Color middle = new Color(color.getRed(), color.getGreen(), color.getBlue(), 18);
        Color inner = new Color(color.getRed(), color.getGreen(), color.getBlue(), 10);
        this.drawRoundedRect(x - 3.0F, y - 3.0F, width + 6.0F, height + 6.0F, radius + 3.0F, outer.getRGB());
        this.drawRoundedRect(x - 2.0F, y - 2.0F, width + 4.0F, height + 4.0F, radius + 2.0F, middle.getRGB());
        this.drawRoundedRect(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, radius + 1.0F, inner.getRGB());
    }

    private void drawHorizontalGradient(float x, float y, float width, float height, Color leftColor, Color rightColor) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(x, y + height, 0.0D).color(leftColor.getRed(), leftColor.getGreen(), leftColor.getBlue(), leftColor.getAlpha()).endVertex();
        worldRenderer.pos(x + width, y + height, 0.0D).color(rightColor.getRed(), rightColor.getGreen(), rightColor.getBlue(), rightColor.getAlpha()).endVertex();
        worldRenderer.pos(x + width, y, 0.0D).color(rightColor.getRed(), rightColor.getGreen(), rightColor.getBlue(), rightColor.getAlpha()).endVertex();
        worldRenderer.pos(x, y, 0.0D).color(leftColor.getRed(), leftColor.getGreen(), leftColor.getBlue(), leftColor.getAlpha()).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
    }

    private void drawHead(float x, float y, float size) {
        if (!this.shouldRenderHead()) {
            return;
        }
        int drawSize = Math.round(size);
        GlStateManager.color(1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(this.headTexture);
        Gui.drawScaledCustomSizeModalRect(Math.round(x), Math.round(y), 8.0F, 8.0F, 8, 8, drawSize, drawSize, 64.0F, 64.0F);
        Gui.drawScaledCustomSizeModalRect(Math.round(x), Math.round(y), 40.0F, 8.0F, 8, 8, drawSize, drawSize, 64.0F, 64.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F);
    }

    private void drawItem(ItemStack itemStack, float x, float y) {
        if (itemStack != null) {
            RenderUtil.renderItemInGUI(itemStack, Math.round(x), Math.round(y));
        }
    }

    private void drawArmorAndHeldItems(EntityPlayer player, float startX, float y, boolean heldFirst) {
        float offset = startX;
        if (heldFirst && player.getHeldItem() != null) {
            this.drawItem(player.getHeldItem(), offset, y);
            offset += 16.0F;
        }
        for (int i = 0; i < 4; i++) {
            ItemStack armor = player.getCurrentArmor(i);
            if (armor != null) {
                this.drawItem(armor, offset, y);
                offset += 16.0F;
            }
        }
        if (!heldFirst && player.getHeldItem() != null) {
            this.drawItem(player.getHeldItem(), offset, y);
        }
    }

    private LayoutMetrics getMyauLayout(EntityLivingBase entityLivingBase, float playerHealthHearts, float targetHealthHearts) {
        ManagedFont nameFont = this.getFont(18);
        ManagedFont infoFont = this.getFont(14);
        float absorptionHearts = entityLivingBase.getAbsorptionAmount() / 2.0F;
        String targetNameText = ChatColors.formatColor(String.format("&r%s&r", TeamUtil.stripName(entityLivingBase)));
        String healthText = ChatColors.formatColor(
                String.format(
                        "&r&fHP: %s%s&r",
                        healthFormat.format(targetHealthHearts),
                        absorptionHearts > 0.0F ? String.format(" &6(+%s)", healthFormat.format(absorptionHearts)) : ""
                )
        );
        String statusText = ChatColors.formatColor(String.format("&r&l%s&r", this.getOutcomeLetter(playerHealthHearts, targetHealthHearts, "D")));
        String diffText = ChatColors.formatColor(
                String.format("&r%s&r", targetHealthHearts == playerHealthHearts ? "0.0" : diffFormat.format(playerHealthHearts - targetHealthHearts))
        );
        float headOffset = this.shouldRenderHead() ? 25.0F : 0.0F;
        float contentWidth = Math.max(
                this.getTextWidth(nameFont, targetNameText) + (this.indicator.getValue() ? 4.0F + this.getTextWidth(nameFont, statusText) : 0.0F),
                this.getTextWidth(infoFont, healthText) + (this.indicator.getValue() ? 4.0F + this.getTextWidth(infoFont, diffText) : 0.0F)
        );
        return new LayoutMetrics(Math.max(headOffset + 70.0F, headOffset + 4.0F + contentWidth), 27.0F);
    }

    private LayoutMetrics getNormalLayout(EntityLivingBase entityLivingBase) {
        ManagedFont nameFont = this.getFont(20);
        float nameWidth = this.getTextWidth(nameFont, entityLivingBase.getName());
        return new LayoutMetrics(Math.max(this.shouldRenderHead() ? 100.0F : 90.0F, nameWidth + (this.shouldRenderHead() ? 36.0F : 10.0F)), 46.0F);
    }

    private LayoutMetrics getHvhLayout(EntityLivingBase entityLivingBase) {
        ManagedFont font = this.getFont(14);
        String diffText = "diff: " + diffFormat.format((mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) - (entityLivingBase.getHealth() + entityLivingBase.getAbsorptionAmount()));
        float width = Math.max(
                100.0F,
                Math.max(this.getTextWidth(font, entityLivingBase.getName()) + 4.0F, this.getTextWidth(font, diffText) + 4.0F)
        );
        return new LayoutMetrics(width, 35.0F);
    }

    private LayoutMetrics getButtLayout(EntityLivingBase entityLivingBase) {
        ManagedFont font = this.getFont(16);
        return new LayoutMetrics(Math.max(120.0F, 60.0F + this.getTextWidth(font, entityLivingBase.getName())), 50.0F);
    }

    private LayoutMetrics getLegitLayout(EntityLivingBase entityLivingBase) {
        ManagedFont font = this.getFont(18);
        return new LayoutMetrics(Math.max(80.0F, this.getTextWidth(font, entityLivingBase.getName()) + 45.0F), 40.0F);
    }

    private LayoutMetrics getLayoutMetrics(EntityLivingBase entityLivingBase, float playerHealthHearts, float targetHealthHearts) {
        switch (this.mode.getValue()) {
            case MODE_NORMAL:
                return this.getNormalLayout(entityLivingBase);
            case MODE_HVH:
                return this.getHvhLayout(entityLivingBase);
            case MODE_RAVEN:
                return new LayoutMetrics(150.0F, 50.0F);
            case MODE_BUTT:
                return this.getButtLayout(entityLivingBase);
            case MODE_LEGIT:
                return this.getLegitLayout(entityLivingBase);
            case MODE_DIABLO:
                return new LayoutMetrics(133.0F, 47.0F);
            case MODE_EXHIBITION:
                return new LayoutMetrics(140.0F, 50.0F);
            case MODE_MYAU:
            default:
                return this.getMyauLayout(entityLivingBase, playerHealthHearts, targetHealthHearts);
        }
    }

    private float resolveRenderX(LayoutMetrics metrics, ScaledResolution scaledResolution) {
        float x = this.offX.getValue().floatValue() / this.scale.getValue();
        switch (this.posX.getValue()) {
            case 1:
                x += scaledResolution.getScaledWidth() / this.scale.getValue() / 2.0F - metrics.width / 2.0F;
                break;
            case 2:
                x *= -1.0F;
                x += scaledResolution.getScaledWidth() / this.scale.getValue() - metrics.width;
                break;
            default:
                break;
        }
        return x;
    }

    private float resolveRenderY(LayoutMetrics metrics, ScaledResolution scaledResolution) {
        float y = this.offY.getValue().floatValue() / this.scale.getValue();
        switch (this.posY.getValue()) {
            case 1:
                y += scaledResolution.getScaledHeight() / this.scale.getValue() / 2.0F - metrics.height / 2.0F;
                break;
            case 2:
                y *= -1.0F;
                y += scaledResolution.getScaledHeight() / this.scale.getValue() - metrics.height;
                break;
            default:
                break;
        }
        return y;
    }

    private void renderMyauMode(LayoutMetrics metrics, EntityLivingBase entityLivingBase, float playerHealthHearts, float targetHealthHearts, float healthRatio, Color targetColor) {
        ManagedFont nameFont = this.getFont(18);
        ManagedFont infoFont = this.getFont(14);
        float absorptionHearts = entityLivingBase.getAbsorptionAmount() / 2.0F;
        Color barColor = this.color.getValue() == 0 ? ColorUtil.getHealthBlend(healthRatio) : targetColor;
        Color deltaColor = new Color(this.getOutcomeColor(playerHealthHearts, targetHealthHearts), true);
        String targetNameText = ChatColors.formatColor(String.format("&r%s&r", TeamUtil.stripName(entityLivingBase)));
        String healthText = ChatColors.formatColor(
                String.format(
                        "&r&fHP: %s%s&r",
                        healthFormat.format(targetHealthHearts),
                        absorptionHearts > 0.0F ? String.format(" &6(+%s)", healthFormat.format(absorptionHearts)) : ""
                )
        );
        String statusText = ChatColors.formatColor(String.format("&r&l%s&r", this.getOutcomeLetter(playerHealthHearts, targetHealthHearts, "D")));
        String diffText = ChatColors.formatColor(
                String.format("&r%s&r", targetHealthHearts == playerHealthHearts ? "0.0" : diffFormat.format(playerHealthHearts - targetHealthHearts))
        );
        float headOffset = this.shouldRenderHead() ? 25.0F : 0.0F;

        RenderUtil.enableRenderState();
        this.drawOutlineBox(
                0.0F,
                0.0F,
                metrics.width,
                metrics.height,
                new Color(0, 0, 0, Math.round(255.0F * this.background.getValue() / 100.0F)).getRGB(),
                this.outline.getValue() ? targetColor.getRGB() : 0
        );
        RenderUtil.drawRect(headOffset + 2.0F, 22.0F, metrics.width - 2.0F, 25.0F, ColorUtil.darker(barColor, 0.2F).getRGB());
        RenderUtil.drawRect(headOffset + 2.0F, 22.0F, headOffset + 2.0F + healthRatio * (metrics.width - headOffset - 4.0F), 25.0F, barColor.getRGB());
        RenderUtil.disableRenderState();

        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        this.drawText(nameFont, targetNameText, headOffset + 2.0F, 2.0F, -1);
        this.drawText(infoFont, healthText, headOffset + 2.0F, 12.0F, -1);
        if (this.indicator.getValue()) {
            this.drawText(nameFont, statusText, metrics.width - 2.0F - this.getTextWidth(nameFont, statusText), 2.0F, deltaColor.getRGB());
            this.drawText(infoFont, diffText, metrics.width - 2.0F - this.getTextWidth(infoFont, diffText), 12.0F, ColorUtil.darker(deltaColor, 0.8F).getRGB());
        }
        this.drawHead(2.0F, 2.0F, 23.0F);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
    }

    private void renderNormalMode(LayoutMetrics metrics, EntityLivingBase entityLivingBase, float playerHealthRaw, float targetHealthRaw, float targetMaxHealthRaw, float healthRatio, Color targetColor) {
        ManagedFont titleFont = this.getFont(14);
        ManagedFont nameFont = this.getFont(20);
        ManagedFont infoFont = this.getFont(16);
        float headerHeight = 12.0F;
        float textX = this.shouldRenderHead() ? 34.0F : 4.0F;
        float percent = this.clamp(targetHealthRaw / Math.max(targetMaxHealthRaw, 0.1F), 0.0F, 1.0F) * 100.0F;

        RenderUtil.enableRenderState();
        RenderUtil.drawRect(0.0F, 0.0F, metrics.width, metrics.height, new Color(0, 0, 0, this.scaledAlpha(90, 180)).getRGB());
        RenderUtil.drawRect(0.0F, 0.0F, metrics.width, headerHeight, new Color(255, 255, 255, this.scaledAlpha(20, 90)).getRGB());
        this.drawHorizontalGradient(
                textX,
                headerHeight + 21.0F,
                Math.max(0.0F, (metrics.width - textX - 2.0F) * healthRatio),
                11.0F,
                ColorUtil.interpolate(0.25F, targetColor, Color.WHITE),
                ColorUtil.darker(targetColor, 0.7F)
        );
        RenderUtil.disableRenderState();

        this.drawCenteredText(titleFont, "Target HUD", metrics.width / 2.0F, headerHeight / 2.0F - this.getTextHeight(titleFont, "A") / 2.0F, -1);
        this.drawHead(2.0F, headerHeight + 2.0F, 30.0F);
        this.drawText(nameFont, entityLivingBase.getName(), textX, headerHeight, -1);
        this.drawText(infoFont, healthFormat.format(targetHealthRaw) + " (" + Math.round(percent) + "%)", textX, headerHeight + this.getTextHeight(nameFont, "A"), -1);
        String letter = this.getOutcomeLetter(playerHealthRaw, targetHealthRaw, "D");
        this.drawText(infoFont, letter, metrics.width - this.getTextWidth(infoFont, letter) - 2.0F, headerHeight + this.getTextHeight(nameFont, "A"), this.getOutcomeColor(playerHealthRaw, targetHealthRaw));
    }

    private void renderHvhMode(LayoutMetrics metrics, EntityLivingBase entityLivingBase, float playerHealthRaw, float targetHealthRaw, float playerRatio, float targetRatio) {
        ManagedFont font = this.getFont(14);
        String diffText = "diff: " + diffFormat.format(playerHealthRaw - targetHealthRaw);
        String wl = EnumChatFormatting.GREEN + "W";
        if (targetHealthRaw == playerHealthRaw) {
            wl = EnumChatFormatting.YELLOW + "E";
        } else if (targetHealthRaw > playerHealthRaw) {
            wl = EnumChatFormatting.RED + "L";
        }

        RenderUtil.enableRenderState();
        RenderUtil.drawRect(0.0F, 0.0F, metrics.width, metrics.height, new Color(69, 69, 69, this.scaledAlpha(45, 110)).getRGB());
        RenderUtil.drawRect(1.0F, 22.0F, 1.0F + (metrics.width - 2.0F) * playerRatio, 24.0F, new Color(94, 255, 0).getRGB());
        RenderUtil.drawRect(1.0F, 25.0F, 1.0F + (metrics.width - 2.0F) * targetRatio, 27.0F, new Color(255, 0, 0).getRGB());
        RenderUtil.disableRenderState();

        this.drawText(font, entityLivingBase.getName(), 2.0F, 2.0F, Color.WHITE.getRGB());
        this.drawText(font, diffText, metrics.width - 2.0F - this.getTextWidth(font, diffText), 12.0F, Color.WHITE.getRGB());
        this.drawText(font, wl, 2.0F, 12.0F, Color.WHITE.getRGB());
    }

    private void renderRavenMode(LayoutMetrics metrics, EntityLivingBase entityLivingBase, float playerHealthRaw, float targetHealthRaw, float healthRatio, Color targetColor) {
        ManagedFont font = this.getFont(16);
        String name = this.truncateText(font, entityLivingBase.getName(), 120.0F);
        Color secondary = ColorUtil.darker(targetColor, 0.8F);
        Color primary = ColorUtil.interpolate(0.35F, targetColor, Color.WHITE);

        RenderUtil.enableRenderState();
        this.drawRoundedRect(0.0F, 0.0F, metrics.width, metrics.height, 5.0F, new Color(0, 0, 0, this.scaledAlpha(80, 150)).getRGB());
        this.drawRoundedRect(5.0F, metrics.height - 20.0F, 105.0F * healthRatio, 6.0F, 6.0F, secondary.getRGB());
        this.drawRoundedRect(5.0F, metrics.height - 20.0F, 105.0F * healthRatio, 3.0F, 6.0F, primary.getRGB());
        RenderUtil.disableRenderState();

        this.drawText(font, name, 11.0F, 17.0F, Color.WHITE.getRGB());
        String status = this.getOutcomeLetter(playerHealthRaw, targetHealthRaw, "E");
        this.drawText(font, status, metrics.width - 14.0F, 17.0F, this.getOutcomeColor(playerHealthRaw, targetHealthRaw));
    }

    private void renderButtMode(LayoutMetrics metrics, EntityLivingBase entityLivingBase, float targetHealthRaw, float healthRatio, Color targetColor) {
        ManagedFont titleFont = this.getFont(16);
        ManagedFont infoFont = this.getFont(14);
        Color barColor = Color.WHITE;

        RenderUtil.enableRenderState();
        this.drawGlowLayers(0.0F, 0.0F, metrics.width, metrics.height, 10.0F, new Color(20, 20, 20, 120));
        this.drawRoundedRect(0.0F, 0.0F, metrics.width, metrics.height, 10.0F, new Color(20, 20, 20, this.scaledAlpha(100, 180)).getRGB());
        this.drawRoundedRect(50.0F, metrics.height - 20.0F, metrics.width - 60.0F, 10.0F, 3.0F, new Color(0, 0, 0, this.scaledAlpha(30, 90)).getRGB());
        this.drawRoundedRect(50.0F, metrics.height - 20.0F, (metrics.width - 60.0F) * healthRatio, 10.0F, 3.0F, barColor.getRGB());
        this.drawRoundedRect(9.0F, 8.0F, 34.0F, 34.0F, 5.0F, new Color(255, 255, 255, 35).getRGB());
        RenderUtil.disableRenderState();

        this.drawHead(10.0F, 9.0F, 32.0F);
        this.drawText(titleFont, entityLivingBase.getName(), 50.0F, 10.0F, -1);
    }

    private void renderLegitMode(LayoutMetrics metrics, EntityLivingBase entityLivingBase, float targetHealthRaw, float healthRatio, Color targetColor) {
        ManagedFont nameFont = this.getFont(18);
        ManagedFont infoFont = this.getFont(14);
        float nameWidth = this.getTextWidth(nameFont, entityLivingBase.getName());

        RenderUtil.enableRenderState();
        this.drawGlowLayers(0.0F, 0.0F, metrics.width, metrics.height, 5.0F, new Color(20, 20, 20, 100));
        this.drawRoundedRect(0.0F, 0.0F, metrics.width, metrics.height, 5.0F, new Color(20, 20, 20, this.scaledAlpha(90, 170)).getRGB());
        this.drawRoundedRect(37.5F, 25.0F, nameWidth, 6.0F, 3.0F, new Color(40, 40, 40).getRGB());
        this.drawRoundedRect(37.5F, 25.0F, nameWidth * healthRatio, 6.0F, 3.0F, targetColor.getRGB());
        RenderUtil.disableRenderState();

        this.drawHead(7.5F, 7.5F, 25.0F);
        this.drawText(nameFont, entityLivingBase.getName(), 37.5F, 9.0F, -1);
    }

    private void renderDiabloMode(LayoutMetrics metrics, EntityLivingBase entityLivingBase, float targetHealthRaw, float healthRatio, Color accentColor) {
        ManagedFont nameFont = this.getFont(16);
        ManagedFont infoFont = this.getFont(13);
        RenderUtil.enableRenderState();
        RenderUtil.drawRect(0.0F, 0.0F, metrics.width, metrics.height, new Color(45, 45, 45).getRGB());
        RenderUtil.drawRect(1.0F, 1.0F, metrics.width - 1.0F, metrics.height - 1.0F, new Color(25, 25, 25).getRGB());
        RenderUtil.drawRect(49.0F, metrics.height - 14.0F, metrics.width - 6.0F, metrics.height - 4.0F, new Color(35, 35, 35).getRGB());
        RenderUtil.drawRect(49.0F, metrics.height - 14.0F, 49.0F + (metrics.width - 55.0F) * healthRatio, metrics.height - 4.0F, accentColor.getRGB());
        RenderUtil.disableRenderState();

        this.drawHead(4.0F, 4.0F, 39.0F);
        this.drawText(nameFont, this.truncateText(nameFont, entityLivingBase.getName(), 74.0F), 49.0F, 6.0F, -1);

        if (entityLivingBase instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entityLivingBase;
            float offset = 50.0F;
            if (player.getHeldItem() != null) {
                this.drawItem(player.getHeldItem(), offset, 15.0F);
                offset += 16.0F;
            }
            for (int i = 0; i < 4; i++) {
                ItemStack armor = player.getCurrentArmor(i);
                if (armor != null) {
                    this.drawItem(armor, offset, 15.0F);
                    offset += 16.0F;
                }
            }
        }
    }

    private void renderExhibitionMode(LayoutMetrics metrics, EntityLivingBase entityLivingBase, float targetHealthRaw, float healthRatio) {
        ManagedFont titleFont = this.getFont(17);
        ManagedFont infoFont = this.getFont(13);

        RenderUtil.enableRenderState();
        RenderUtil.drawRect(0.0F, 0.0F, metrics.width, metrics.height, new Color(40, 40, 40).getRGB());
        RenderUtil.drawRect(2.0F, 2.0F, metrics.width - 2.0F, metrics.height - 2.0F, new Color(25, 25, 25).getRGB());
        RenderUtil.drawRect(40.0F, 20.0F, metrics.width - 10.0F, 24.0F, new Color(50, 50, 50).getRGB());
        RenderUtil.drawRect(40.0F, 20.0F, 40.0F + (metrics.width - 50.0F) * healthRatio, 24.0F, new Color(221, 239, 22).getRGB());
        RenderUtil.disableRenderState();

        GlStateManager.pushMatrix();
        GuiInventory.drawEntityOnScreen(20, 43, 20, 100.0F, -75.0F, entityLivingBase);
        GlStateManager.popMatrix();

        this.drawText(titleFont, this.truncateText(titleFont, entityLivingBase.getName(), 90.0F), 40.0F, 7.0F, -1);
        this.drawText(infoFont, "HP:" + healthFormat.format(targetHealthRaw) + " | Dist:" + Math.round(entityLivingBase.getDistanceToEntity(mc.thePlayer)), 40.0F, 27.0F, -1);

        for (int i = 0; i <= 9; i++) {
            float lineX = 40.0F + ((metrics.width - 50.0F) / 10.0F) * i;
            RenderUtil.drawLine(lineX, 20.0F, lineX, 24.0F, 1.0F, new Color(0, 0, 0).getRGB());
        }

        if (entityLivingBase instanceof EntityPlayer) {
            this.drawArmorAndHeldItems((EntityPlayer) entityLivingBase, 40.0F, 32.0F, false);
        }
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        EntityLivingBase previousTarget = this.target;
        this.target = this.resolveTarget();
        if (this.target == null) {
            return;
        }

        float targetAbsorptionHearts = this.target.getAbsorptionAmount() / 2.0F;
        float targetHealthHearts = this.target.getHealth() / 2.0F + targetAbsorptionHearts;
        float targetMaxHealthHearts = Math.max(0.1F, this.target.getMaxHealth() / 2.0F + targetAbsorptionHearts);
        float playerHealthHearts = (mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) / 2.0F;

        if (this.target != previousTarget) {
            this.headTexture = null;
            this.animTimer.setTime();
            this.oldHealth = targetHealthHearts;
            this.newHealth = targetHealthHearts;
            this.maxHealth = targetMaxHealthHearts;
        }

        if (!this.animations.getValue() || this.animTimer.hasTimeElapsed(150L)) {
            this.oldHealth = this.newHealth;
            this.newHealth = targetHealthHearts;
            this.maxHealth = targetMaxHealthHearts;
            if (this.oldHealth != this.newHealth) {
                this.animTimer.reset();
            }
        } else {
            this.maxHealth = targetMaxHealthHearts;
        }

        ResourceLocation resourceLocation = this.getSkin(this.target);
        if (resourceLocation != null) {
            this.headTexture = resourceLocation;
        }

        float elapsedTime = (float) Math.min(Math.max(this.animTimer.getElapsedTime(), 0L), 150L);
        float healthRatio = this.clamp(
                RenderUtil.lerpFloat(this.newHealth, this.oldHealth, elapsedTime / 150.0F) / Math.max(this.maxHealth, 0.1F),
                0.0F,
                1.0F
        );
        float targetHealthRaw = this.target.getHealth() + this.target.getAbsorptionAmount();
        float targetMaxHealthRaw = Math.max(0.1F, this.target.getMaxHealth() + this.target.getAbsorptionAmount());
        float playerHealthRaw = mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount();
        float playerRatio = this.clamp(
                playerHealthRaw / Math.max(mc.thePlayer.getMaxHealth() + mc.thePlayer.getAbsorptionAmount(), 0.1F),
                0.0F,
                1.0F
        );
        float targetRatio = this.clamp(targetHealthRaw / targetMaxHealthRaw, 0.0F, 1.0F);
        Color targetColor = this.getTargetColor(this.target);
        LayoutMetrics metrics = this.getLayoutMetrics(this.target, playerHealthHearts, targetHealthHearts);
        ScaledResolution scaledResolution = new ScaledResolution(mc);

        GlStateManager.pushMatrix();
        GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 1.0F);
        GlStateManager.translate(this.resolveRenderX(metrics, scaledResolution), this.resolveRenderY(metrics, scaledResolution), -450.0F);

        switch (this.mode.getValue()) {
            case MODE_NORMAL:
                this.renderNormalMode(metrics, this.target, playerHealthRaw, targetHealthRaw, targetMaxHealthRaw, healthRatio, targetColor);
                break;
            case MODE_HVH:
                this.renderHvhMode(metrics, this.target, playerHealthRaw, targetHealthRaw, playerRatio, targetRatio);
                break;
            case MODE_RAVEN:
                this.renderRavenMode(metrics, this.target, playerHealthRaw, targetHealthRaw, healthRatio, targetColor);
                break;
            case MODE_BUTT:
                this.renderButtMode(metrics, this.target, targetHealthRaw, healthRatio, targetColor);
                break;
            case MODE_LEGIT:
                this.renderLegitMode(metrics, this.target, targetHealthRaw, healthRatio, targetColor);
                break;
            case MODE_DIABLO:
                this.renderDiabloMode(metrics, this.target, targetHealthRaw, healthRatio, new Color(212, 106, 173));
                break;
            case MODE_EXHIBITION:
                this.renderExhibitionMode(metrics, this.target, targetHealthRaw, healthRatio);
                break;
            case MODE_MYAU:
            default:
                this.renderMyauMode(metrics, this.target, playerHealthHearts, targetHealthHearts, healthRatio, targetColor);
                break;
        }

        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }


    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() != Action.ATTACK) {
                return;
            }
            Entity entity = packet.getEntityFromWorld(mc.theWorld);
            if (entity instanceof EntityLivingBase) {
                if (entity instanceof EntityArmorStand) {
                    return;
                }
                this.lastAttackTimer.reset();
                this.lastTarget = (EntityLivingBase) entity;
            }
        }
    }
}
