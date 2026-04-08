package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.potion.Potion;

public class CriticalCheck extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public CriticalCheck() {
        super("CriticalCheck", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()
                || event.isCancelled()
                || event.getType() != EventType.SEND
                || mc.thePlayer == null
                || mc.theWorld == null
                || !(event.getPacket() instanceof C02PacketUseEntity)) {
            return;
        }

        C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
        if (packet.getAction() != C02PacketUseEntity.Action.ATTACK) {
            return;
        }

        Entity target = packet.getEntityFromWorld(mc.theWorld);
        if (!(target instanceof EntityLivingBase)) {
            return;
        }

        if (this.isCriticalHit()) {
            ChatUtil.sendFormatted(String.format("%s&aCrit!&r", Myau.clientName));
        }
    }

    private boolean isCriticalHit() {
        return !mc.thePlayer.onGround
                && mc.thePlayer.fallDistance > 0.0F
                && !mc.thePlayer.isOnLadder()
                && !mc.thePlayer.isInWater()
                && !mc.thePlayer.isPotionActive(Potion.blindness)
                && mc.thePlayer.ridingEntity == null;
    }
}
