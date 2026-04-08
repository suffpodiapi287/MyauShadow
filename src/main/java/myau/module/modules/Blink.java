package myau.module.modules;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.management.BlinkManager;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.TeamUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class Blink extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"SENT", "RECEIVED", "BOTH", "ABUSE"});
    public final BooleanProperty pulse = new BooleanProperty("pulse", false);
    public final IntProperty pulseDelay = new IntProperty("pulse-delay", 1000, 500, 5000, () -> this.pulse.getValue());
    public final BooleanProperty fakePlayer = new BooleanProperty("fake-player", true);
    public final FloatProperty abuseEnterRange = new FloatProperty("abuse-enter-range", 3.0F, 1.0F, 3.0F, this::isAbuseMode);
    public final FloatProperty abuseExitRange = new FloatProperty("abuse-exit-range", 6.0F, 3.0F, 6.0F, this::isAbuseMode);

    private final TimerUtil pulseTimer = new TimerUtil();

    public Blink() {
        super("Blink", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST || mc.thePlayer == null) {
            return;
        }

        if (mc.thePlayer.isDead || mc.thePlayer.ticksExisted <= 10) {
            Myau.blinkManager.setManualBlinkState(false, this.getDirection());
            return;
        }

        if (this.isAbuseMode()) {
            this.handleAbuseTick();
            return;
        }

        if (!Myau.blinkManager.isManualBlinking()) {
            Myau.blinkManager.setManualBlinkState(true, this.getDirection());
            if (this.fakePlayer.getValue()) {
                Myau.blinkManager.addManualFakePlayer();
            }
        }

        switch (this.mode.getValue()) {
            case 0:
                Myau.blinkManager.syncManualSent();
                break;
            case 1:
                Myau.blinkManager.syncManualReceived();
                break;
            default:
                break;
        }

        if (this.pulse.getValue() && this.pulseTimer.hasTimeElapsed(this.pulseDelay.getValue())) {
            Myau.blinkManager.setManualBlinkState(false, this.getDirection());
            if (this.fakePlayer.getValue()) {
                Myau.blinkManager.addManualFakePlayer();
            }
            Myau.blinkManager.setManualBlinkState(true, this.getDirection());
            this.pulseTimer.reset();
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (!this.isEnabled() || Myau.blinkManager.getManualPositions().isEmpty()) {
            return;
        }

        Color color = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
        double renderPosX = ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
        double renderPosY = ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
        double renderPosZ = ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();

        synchronized (Myau.blinkManager.getManualPositions()) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            mc.entityRenderer.disableLightmap();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 1.0F);

            for (Vec3 pos : Myau.blinkManager.getManualPositions()) {
                GL11.glVertex3d(pos.xCoord - renderPosX, pos.yCoord - renderPosY, pos.zCoord - renderPosZ);
            }

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnd();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }
    }

    @Override
    public void onEnabled() {
        this.pulseTimer.reset();
        Myau.blinkManager.clearManual();
        if (!this.isAbuseMode()) {
            Myau.blinkManager.setManualBlinkState(true, this.getDirection());
            if (this.fakePlayer.getValue()) {
                Myau.blinkManager.addManualFakePlayer();
            }
        }
    }

    @Override
    public void onDisabled() {
        if (mc.thePlayer == null) {
            return;
        }

        Myau.blinkManager.setManualBlinkState(false, this.getDirection());
    }

    @Override
    public void verifyValue(String value) {
        if (!this.isEnabled()) {
            return;
        }

        if (this.mode.getName().equals(value)) {
            Myau.blinkManager.setManualBlinkState(false, this.getDirection());
            if (!this.isAbuseMode()) {
                Myau.blinkManager.setManualBlinkState(true, this.getDirection());
                if (this.fakePlayer.getValue()) {
                    Myau.blinkManager.addManualFakePlayer();
                }
            }
        } else if (this.fakePlayer.getName().equals(value)) {
            if (this.fakePlayer.getValue()) {
                if (Myau.blinkManager.isManualBlinking()) {
                    Myau.blinkManager.addManualFakePlayer();
                }
            } else {
                Myau.blinkManager.removeManualFakePlayer();
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{
                CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())
        };
    }

    private BlinkManager.BlinkDirection getDirection() {
        switch (this.mode.getValue()) {
            case 1:
            case 3:
                return BlinkManager.BlinkDirection.RECEIVE;
            case 2:
                return BlinkManager.BlinkDirection.BOTH;
            default:
                return BlinkManager.BlinkDirection.SEND;
        }
    }

    private boolean isAbuseMode() {
        return this.mode.getValue() == 3;
    }

    private void handleAbuseTick() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        boolean currentlyBlinking = Myau.blinkManager.isManualBlinking();

        if (!this.shouldAbuseBlink(killAura, currentlyBlinking)) {
            if (currentlyBlinking) {
                Myau.blinkManager.setManualBlinkState(false, this.getDirection());
                this.pulseTimer.reset();
            }
            return;
        }

        if (!currentlyBlinking) {
            Myau.blinkManager.setManualBlinkState(true, this.getDirection());
            if (this.fakePlayer.getValue()) {
                Myau.blinkManager.addManualFakePlayer();
            }
            this.pulseTimer.reset();
            return;
        }

        if (this.pulse.getValue() && this.pulseTimer.hasTimeElapsed(this.pulseDelay.getValue())) {
            Myau.blinkManager.setManualBlinkState(false, this.getDirection());
            if (this.fakePlayer.getValue()) {
                Myau.blinkManager.addManualFakePlayer();
            }
            Myau.blinkManager.setManualBlinkState(true, this.getDirection());
            this.pulseTimer.reset();
        }
    }

    private boolean shouldAbuseBlink(KillAura killAura, boolean currentlyBlinking) {
        if (killAura == null || !killAura.isEnabled() || !killAura.isAttackAllowed()) {
            return false;
        }

        EntityLivingBase target = killAura.getTarget();
        if (target == null || target.isDead || !TeamUtil.isEntityLoaded(target)) {
            return false;
        }

        EntityOtherPlayerMP fakePlayer = Myau.blinkManager.getManualFakePlayer();
        double distance = currentlyBlinking && fakePlayer != null && TeamUtil.isEntityLoaded(fakePlayer)
                ? fakePlayer.getDistanceToEntity(target)
                : mc.thePlayer.getDistanceToEntity(target);
        return distance <= (currentlyBlinking ? this.abuseExitRange.getValue() : this.abuseEnterRange.getValue());
    }
}
