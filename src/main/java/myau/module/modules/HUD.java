package myau.module.modules;

import myau.Myau;
import myau.enums.BlinkModules;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.Render2DEvent;
import myau.events.TickEvent;
import myau.mixin.IAccessorGuiChat;
import myau.module.Module;
import myau.notification.NotificationManager;
import myau.property.properties.*;
import myau.util.ColorUtil;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import myau.util.font.ManagedFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private List<Module> activeModules = new ArrayList<>();
    public final ModeProperty colorMode = new ModeProperty(
            "color", 3, new String[]{"RAINBOW", "CHROMA", "ASTOLFO", "CUSTOM1", "CUSTOM12", "CUSTOM123"}
    );
    public final FloatProperty colorSpeed = new FloatProperty("color-speed", 1.0F, 0.5F, 1.5F);
    public final PercentProperty colorSaturation = new PercentProperty("color-saturation", 50);
    public final PercentProperty colorBrightness = new PercentProperty("color-brightness", 100);
    public final ColorProperty custom1 = new ColorProperty("custom-color-1", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 3 || this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom2 = new ColorProperty("custom-color-2", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom3 = new ColorProperty("custom-color-3", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 5);
    public final ModeProperty posX = new ModeProperty("position-x", 0, new String[]{"LEFT", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 0, new String[]{"TOP", "BOTTOM"});
    public final ModeProperty fontMode = new ModeProperty("font-mode", FontManager.indexOfManagedFont("Minecraft"), FontManager.MANAGED_FONT_MODES);
    public final FloatProperty fontSize = new FloatProperty("font-size", 12.0F, 8.0F, 30.0F);
    public final IntProperty offsetX = new IntProperty("offset-x", 2, 0, 255);
    public final IntProperty offsetY = new IntProperty("offset-y", 2, 0, 255);
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final PercentProperty background = new PercentProperty("background", 25);
    public final BooleanProperty showBar = new BooleanProperty("bar", true);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final FloatProperty colorDistance = new FloatProperty("color-dist", 50.0F, 10.0F, 100.0F);
    public final BooleanProperty suffixes = new BooleanProperty("suffixes", true);
    public final BooleanProperty lowerCase = new BooleanProperty("lower-case", false);
    public final BooleanProperty chatOutline = new BooleanProperty("chat-outline", true);
    public final BooleanProperty blinkTimer = new BooleanProperty("blink-timer", true);
    public final BooleanProperty toggleSound = new BooleanProperty("toggle-sounds", true);
    public final BooleanProperty toggleAlerts = new BooleanProperty("toggle-alerts", false);

    public HUD() {
        super("HUD", true, true);
    }

    private ManagedFont getArraylistFont() {
        return FontManager.getManagedFont(this.fontMode.getModeString(), Math.round(this.fontSize.getValue()));
    }

    private String getModuleName(Module module) {
        String moduleName = module.getName();
        if (this.lowerCase.getValue()) {
            moduleName = moduleName.toLowerCase(Locale.ROOT);
        }
        return moduleName;
    }

    private String[] getModuleSuffix(Module module) {
        String[] moduleSuffix = module.getSuffix();
        if (this.lowerCase.getValue()) {
            for (int i = 0; i < moduleSuffix.length; i++) {
                moduleSuffix[i] = moduleSuffix[i].toLowerCase(Locale.ROOT);
            }
        }
        return moduleSuffix;
    }

    private int calculateStringWidth(String string, String[] suffixes) {
        ManagedFont font = this.getArraylistFont();
        float width = font.getStringWidth(string);
        if (this.suffixes.getValue()) {
            for (String suffix : suffixes) {
                width += 3.0F + font.getStringWidth(suffix);
            }
        }
        return Math.round(width);
    }

    private int getModuleWidth(Module module) {
        return this.calculateStringWidth(this.getModuleName(module), this.getModuleSuffix(module));
    }

    private float getArraylistTextHeight() {
        return Math.max(1.0F, this.getArraylistFont().getHeight() - 1.0F);
    }

    private void drawArraylistText(ManagedFont font, String text, float x, float y, int color) {
        if (this.shadow.getValue()) {
            font.drawStringWithShadow(text, x, y, color);
        } else {
            font.drawString(text, x, y, color);
        }
    }

    private void renderArraylist(float rowHeight, long time) {
        ManagedFont font = this.getArraylistFont();
        float x = this.offsetX.getValue()
                + (1.0F + (this.showBar.getValue() ? (this.shadow.getValue() ? 2.0F : 1.0F) : 0.0F)) * this.scale.getValue();
        float y = this.offsetY.getValue() + this.scale.getValue();

        if (this.posX.getValue() == 1) {
            x = new ScaledResolution(mc).getScaledWidth() - x;
        }
        if (this.posY.getValue() == 1) {
            y = new ScaledResolution(mc).getScaledHeight() - y - rowHeight * this.scale.getValue();
        }

        long offset = 0L;
        for (Module module : this.activeModules) {
            String moduleName = this.getModuleName(module);
            String[] moduleSuffix = this.getModuleSuffix(module);
            float totalWidth = this.calculateStringWidth(moduleName, moduleSuffix) - (this.shadow.getValue() ? 0.0F : 1.0F);
            int color = this.getColor(time, offset).getRGB();

            RenderUtil.enableRenderState();
            if (this.background.getValue() > 0) {
                RenderUtil.drawRect(
                        x / this.scale.getValue() - 1.0F - (this.posX.getValue() == 0 ? 0.0F : totalWidth),
                        y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : (this.shadow.getValue() ? 1.0F : 0.0F)),
                        x / this.scale.getValue() + 1.0F + (this.posX.getValue() == 0 ? totalWidth : 0.0F),
                        y / this.scale.getValue() + rowHeight + (this.posY.getValue() == 0 ? (this.shadow.getValue() ? 1.0F : 0.0F) : (offset == 0L ? 1.0F : 0.0F)),
                        new Color(0.0F, 0.0F, 0.0F, this.background.getValue() / 100.0F).getRGB()
                );
            }
            if (this.showBar.getValue()) {
                if (this.shadow.getValue()) {
                    RenderUtil.drawRect(
                            x / this.scale.getValue() + (this.posX.getValue() == 0 ? -3.0F : 1.0F),
                            y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 1.0F),
                            x / this.scale.getValue() + (this.posX.getValue() == 0 ? -2.0F : 2.0F),
                            y / this.scale.getValue() + rowHeight + (this.posY.getValue() == 0 ? 1.0F : (offset == 0L ? 1.0F : 0.0F)),
                            color
                    );
                    RenderUtil.drawRect(
                            x / this.scale.getValue() + (this.posX.getValue() == 0 ? -2.0F : 2.0F),
                            y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 1.0F),
                            x / this.scale.getValue() + (this.posX.getValue() == 0 ? -1.0F : 3.0F),
                            y / this.scale.getValue() + rowHeight + (this.posY.getValue() == 0 ? 1.0F : (offset == 0L ? 1.0F : 0.0F)),
                            (color & 16579836) >> 2 | color & 0xFF000000
                    );
                } else {
                    RenderUtil.drawRect(
                            x / this.scale.getValue() + (this.posX.getValue() == 0 ? -2.0F : 1.0F),
                            y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 0.0F),
                            x / this.scale.getValue() + (this.posX.getValue() == 0 ? -1.0F : 2.0F),
                            y / this.scale.getValue() + rowHeight + (this.posY.getValue() == 0 ? 0.0F : (offset == 0L ? 1.0F : 0.0F)),
                            color
                    );
                }
            }
            RenderUtil.disableRenderState();
            GlStateManager.disableDepth();

            float baseX = x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F);
            float baseY = y / this.scale.getValue();
            this.drawArraylistText(font, moduleName, baseX, baseY, color);

            if (this.suffixes.getValue() && moduleSuffix.length > 0) {
                float suffixX = baseX + font.getStringWidth(moduleName) + 3.0F;
                for (String suffix : moduleSuffix) {
                    this.drawArraylistText(font, suffix, suffixX, baseY, ChatColors.GRAY.toAwtColor());
                    suffixX += font.getStringWidth(suffix) + (this.shadow.getValue() ? 3.0F : 2.0F);
                }
            }

            y += (rowHeight + (this.shadow.getValue() ? 1.0F : 0.0F)) * this.scale.getValue() * (this.posY.getValue() == 0 ? 1.0F : -1.0F);
            offset++;
        }
    }

    private float getColorCycle(long time, long offset) {
        long speed = (long) (3000.0D / Math.pow(Math.min(Math.max(0.5F, this.colorSpeed.getValue()), 1.5F), 3.0D));
        return 1.0F - (float) (Math.abs(time - offset * 300L) % speed) / speed;
    }

    public Color getColor(long time) {
        return this.getColor(time, 0L);
    }

    public Color getColor(long time, long offset) {
        Color color = Color.WHITE;
        switch (this.colorMode.getValue()) {
            case 0:
                color = ColorUtil.fromHSB(this.getColorCycle(time, offset), 1.0F, 1.0F);
                break;
            case 1:
                color = ColorUtil.fromHSB(this.getColorCycle(time / 3L, 0L), 1.0F, 1.0F);
                break;
            case 2:
                float cycle = this.getColorCycle(time, offset);
                if (cycle % 1.0F < 0.5F) {
                    cycle = 1.0F - cycle % 1.0F;
                }
                color = ColorUtil.fromHSB(cycle, 1.0F, 1.0F);
                break;
            case 3:
                color = new Color(this.custom1.getValue());
                break;
            case 4:
                double cycle1 = this.getColorCycle(time, offset);
                color = ColorUtil.interpolate(
                        (float) (2.0D * Math.abs(cycle1 - Math.floor(cycle1 + 0.5D))),
                        new Color(this.custom1.getValue()),
                        new Color(this.custom2.getValue())
                );
                break;
            case 5:
                double cycle2 = this.getColorCycle(time, offset);
                float floor = (float) (2.0D * Math.abs(cycle2 - Math.floor(cycle2 + 0.5D)));
                if (floor <= 0.5F) {
                    color = ColorUtil.interpolate(floor * 2.0F, new Color(this.custom1.getValue()), new Color(this.custom2.getValue()));
                } else {
                    color = ColorUtil.interpolate((floor - 0.5F) * 2.0F, new Color(this.custom2.getValue()), new Color(this.custom3.getValue()));
                }
                break;
            default:
                break;
        }

        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(
                hsb[0],
                hsb[1] * (this.colorSaturation.getValue() / 100.0F),
                hsb[2] * (this.colorBrightness.getValue() / 100.0F)
        );
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            this.activeModules = Myau.moduleManager.modules.values().stream()
                    .filter(module -> module.isEnabled() && !module.isHidden())
                    .sorted(Comparator.comparingInt(this::getModuleWidth).reversed())
                    .collect(Collectors.toList());
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.chatOutline.getValue() && mc.currentScreen instanceof GuiChat) {
            String text = ((IAccessorGuiChat) mc.currentScreen).getInputField().getText().trim();
            if (Myau.commandManager != null && Myau.commandManager.isTypingCommand(text)) {
                RenderUtil.enableRenderState();
                RenderUtil.drawOutlineRect(
                        2.0F,
                        mc.currentScreen.height - 14.0F,
                        mc.currentScreen.width - 2.0F,
                        mc.currentScreen.height - 2.0F,
                        1.5F,
                        0,
                        this.getColor(System.currentTimeMillis()).getRGB()
                );
                RenderUtil.disableRenderState();
            }
        }

        if (this.isEnabled() && !mc.gameSettings.showDebugInfo) {
            float rowHeight = this.getArraylistTextHeight();
            ManagedFont font = this.getArraylistFont();

            GlStateManager.pushMatrix();
            GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);
            long time = System.currentTimeMillis();
            this.renderArraylist(rowHeight, time);

            if (this.blinkTimer.getValue()) {
                long offset = this.activeModules.size();
                BlinkModules blinkingModule = Myau.blinkManager.getBlinkingModule();
                boolean renderManualBlink = Myau.blinkManager.isManualBlinking();
                if (renderManualBlink || blinkingModule != BlinkModules.NONE && blinkingModule != BlinkModules.AUTO_BLOCK) {
                    long movementPacketSize = renderManualBlink ? Myau.blinkManager.countManualMovement() : Myau.blinkManager.countMovement();
                    int queuedPackets = renderManualBlink ? Myau.blinkManager.countManualQueuedPackets() : Myau.blinkManager.countQueuedPackets();
                    int displayCount = movementPacketSize > 0L ? (int) movementPacketSize : queuedPackets;
                    if (displayCount > 0) {
                        String text = String.valueOf(displayCount);
                        GlStateManager.enableBlend();
                        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        if (this.shadow.getValue()) {
                            font.drawStringWithShadow(
                                    text,
                                    new ScaledResolution(mc).getScaledWidth() / 2.0F / this.scale.getValue() - font.getStringWidth(text) / 2.0F,
                                    new ScaledResolution(mc).getScaledHeight() / 5.0F * 3.0F / this.scale.getValue(),
                                    this.getColor(time, offset).getRGB() & 16777215 | -1090519040
                            );
                        } else {
                            font.drawString(
                                    text,
                                    new ScaledResolution(mc).getScaledWidth() / 2.0F / this.scale.getValue() - font.getStringWidth(text) / 2.0F,
                                    new ScaledResolution(mc).getScaledHeight() / 5.0F * 3.0F / this.scale.getValue(),
                                    this.getColor(time, offset).getRGB() & 16777215 | -1090519040
                            );
                        }
                        GlStateManager.disableBlend();
                    }
                }
            }

            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }

        if (this.toggleAlerts.getValue()) {
            NotificationManager.render();
        }
    }
}
