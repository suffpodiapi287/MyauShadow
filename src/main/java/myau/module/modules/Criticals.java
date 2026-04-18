package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.PacketEvent;
import myau.mixin.IAccessorC03PacketPlayer;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.PacketUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C03PacketPlayer;

public class Criticals extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty(
            "mode",
            0,
            new String[]{"Packet", "NCPPacket", "BlocksMC", "BlocksMC2", "NoGround", "Hop", "TPHop", "Jump", "LowJump", "CustomMotion", "Visual"}
    );
    public final ModeProperty critTiming = new ModeProperty("crit-timing", 0, new String[]{"Always", "OnGround", "OffGround"});
    public final IntProperty delay = new IntProperty("delay", 0, 0, 500);
    public final IntProperty hurtTime = new IntProperty("hurt-time", 10, 0, 10);
    public final FloatProperty customMotionY = new FloatProperty("custom-y", 0.2F, 0.01F, 0.42F, () -> this.mode.getValue() == 9);
    private final TimerUtil timer = new TimerUtil();

    public Criticals() {
        super("Criticals", false);
    }

    @Override
    public void onEnabled() {
        if (this.mode.getValue() == 4 && mc.thePlayer != null && mc.thePlayer.onGround) {
            mc.thePlayer.jump();
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (!(event.getTarget() instanceof EntityLivingBase)) {
            return;
        }

        if (!this.isCritTimingAllowed()) {
            return;
        }

        EntityLivingBase target = (EntityLivingBase) event.getTarget();
        if (!this.canDoCritical(target)) {
            return;
        }

        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ;

        switch (this.mode.getValue()) {
            case 0: // PACKET
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.0625D, z, true));
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                mc.thePlayer.onCriticalHit(target);
                break;
            case 1: // NCP_PACKET
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.11D, z, false));
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.1100013579D, z, false));
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.0000013579D, z, false));
                mc.thePlayer.onCriticalHit(target);
                break;
            case 2: // BLOCKS_MC
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.001091981D, z, true));
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                break;
            case 3: // BLOCKS_MC_2
                if (mc.thePlayer.ticksExisted % 4 == 0) {
                    PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.0011D, z, true));
                    PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                }
                break;
            case 4: // NO_GROUND
                break;
            case 5: // HOP
                mc.thePlayer.motionY = 0.1D;
                mc.thePlayer.fallDistance = 0.1F;
                mc.thePlayer.onGround = false;
                break;
            case 6: // TP_HOP
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.02D, z, false));
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.01D, z, false));
                mc.thePlayer.setPosition(x, y + 0.01D, z);
                break;
            case 7: // JUMP
                mc.thePlayer.motionY = 0.42D;
                break;
            case 8: // LOW_JUMP
                mc.thePlayer.motionY = 0.3425D;
                break;
            case 9: // CUSTOM_MOTION
                mc.thePlayer.motionY = this.customMotionY.getValue();
                break;
            case 10: // VISUAL
                mc.thePlayer.onCriticalHit(target);
        }

        this.timer.reset();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.isCancelled() || event.getType() != EventType.SEND) {
            return;
        }

        if (this.mode.getValue() == 4 && event.getPacket() instanceof C03PacketPlayer) {
            ((IAccessorC03PacketPlayer) event.getPacket()).setOnGround(false);
        }
    }

    private boolean isCritTimingAllowed() {
        switch (this.critTiming.getValue()) {
            case 1: // OnGround
                return mc.thePlayer.onGround;
            case 2: // OffGround
                return !mc.thePlayer.onGround;
            default: // Always
                return true;
        }
    }

    private boolean canDoCritical(EntityLivingBase target) {
        if (!mc.thePlayer.onGround
                || mc.thePlayer.isOnLadder()
                || mc.thePlayer.isInWater()
                || mc.thePlayer.isInLava()
                || mc.thePlayer.ridingEntity != null) {
            return false;
        }

        if (target.hurtTime > this.hurtTime.getValue()) {
            return false;
        }

        Fly fly = (Fly) Myau.moduleManager.modules.get(Fly.class);
        if (fly != null && fly.isEnabled()) {
            return false;
        }

        return this.timer.hasTimeElapsed(this.delay.getValue().longValue());
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
