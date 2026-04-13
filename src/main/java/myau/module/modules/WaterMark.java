package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.TextProperty;
import myau.util.font.FontManager;
import myau.util.font.ManagedFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class WaterMark extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Exhibition", "Modern"});
    public final ModeProperty exhibitionFontMode = new ModeProperty("font-mode", FontManager.indexOfManagedFont("Minecraft"), FontManager.MANAGED_FONT_MODES, () -> mode.getValue() == 0);
    public final FloatProperty exhibitionFontSize = new FloatProperty("font-size", 20.0F, 8.0F, 30.0F, () -> mode.getValue() == 0);

    public final TextProperty modernText = new TextProperty("Text", "MyauShadow", () -> mode.getValue() == 1);
    public final BooleanProperty shadow = new BooleanProperty("Shadow", true, () -> mode.getValue() == 1);
    public final BooleanProperty enableGlow = new BooleanProperty("Glow", true);

    public WaterMark() {
        super("WaterMark", false, false);
    }

    private ManagedFont getExhibitionFont() {
        return FontManager.getManagedFont(this.exhibitionFontMode.getModeString(), Math.round(this.exhibitionFontSize.getValue()));
    }

    private float getStringWidth(String text) {
        ManagedFont fr = getExhibitionFont();
        if (fr != null) {
            return fr.getStringWidth(text);
        }
        return mc.fontRendererObj.getStringWidth(text);
    }

    private void drawStringWithShadow(String text, float x, float y, int color) {
        ManagedFont fr = getExhibitionFont();
        if (fr != null) {
            fr.drawStringWithShadow(text, x, y, color);
        } else {
            mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) return;

        if (mode.getValue() == 0) {
            renderExhibition();
        } else {
            renderModern();
        }
    }

    private void renderModern() {
        ManagedFont fr = FontManager.getManagedFont("Nunito Bold", 48);
        boolean customFont = fr != null;

        HUD hud = (HUD) Myau.moduleManager.getModule("HUD");

        String text = modernText.getValue();
        float x = 4.0f;
        float y = 4.0f;
        long time = System.currentTimeMillis();

        GlStateManager.pushMatrix();

        char[] characters = text.toCharArray();
        float currentX = x;

        for (int i = 0; i < characters.length; i++) {
            String charStr = String.valueOf(characters[i]);

            int color = 0xFFFFFFFF;
            if (hud != null) {
                long offset = (long) (i * hud.colorDistance.getValue());
                color = hud.getColor(time, offset).getRGB();
            }

            if (customFont) {
                if (shadow.getValue()) {
                    fr.drawStringWithShadow(charStr, currentX, y, color);
                } else {
                    fr.drawString(charStr, currentX, y, color);
                }
                currentX += fr.getStringWidth(charStr);
            } else {
                mc.fontRendererObj.drawString(charStr, currentX, y, color, shadow.getValue());
                currentX += mc.fontRendererObj.getStringWidth(charStr);
            }
        }

        GlStateManager.popMatrix();
    }

    private void renderExhibition() {
        int fps = Minecraft.getDebugFPS();
        int ping = 0;

        if (mc.thePlayer != null && mc.theWorld != null) {
            if (mc.thePlayer.sendQueue != null && mc.thePlayer.sendQueue.getPlayerInfo(mc.thePlayer.getUniqueID()) != null) {
                ping = mc.thePlayer.sendQueue.getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime();
            }
        }

        String exhibitionText = "Myau";
        String restText = "Shadow ";
        String fpsValue = fps + "FPS";
        String pingValue = ping + "ms";

        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);

        float x = 2.0f;
        float y = 2.0f;

        if (getExhibitionFont() != null) {
            y += 1.0f;
        }

        GlStateManager.pushMatrix();

        long time = System.currentTimeMillis();
        int rainbowColor = hud != null ? hud.getColor(time).getRGB() : 0xFFFFFFFF;

        drawStringWithShadow(exhibitionText, x, y, rainbowColor);
        float currentX = x + getStringWidth(exhibitionText);

        int whiteColor = 0xFFFFFFFF;
        drawStringWithShadow(restText, currentX, y, whiteColor);
        currentX += getStringWidth(restText);

        int grayColor = 0xFFAAAAAA;
        drawStringWithShadow("[", currentX, y, grayColor);
        currentX += getStringWidth("[");

        drawStringWithShadow(fpsValue, currentX, y, whiteColor);
        currentX += getStringWidth(fpsValue);

        drawStringWithShadow("]", currentX, y, grayColor);
        currentX += getStringWidth("]");

        String space = " ";
        drawStringWithShadow(space, currentX, y, whiteColor);
        currentX += getStringWidth(space);

        drawStringWithShadow("[", currentX, y, grayColor);
        currentX += getStringWidth("[");

        drawStringWithShadow(pingValue, currentX, y, whiteColor);
        currentX += getStringWidth(pingValue);

        drawStringWithShadow("]", currentX, y, grayColor);

        GlStateManager.popMatrix();
    }
}
