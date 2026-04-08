package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.DataWatcher.WatchableObject;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class HealthDebug extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat DF = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));

    private int lastTickProcessed = -1;

    public HealthDebug() {
        super("HealthDebug", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()
                || event.isCancelled()
                || event.getType() != EventType.RECEIVE
                || mc.thePlayer == null
                || mc.theWorld == null) {
            return;
        }

        if (event.getPacket() instanceof S06PacketUpdateHealth) {
            float diff = ((S06PacketUpdateHealth) event.getPacket()).getHealth() - mc.thePlayer.getHealth();
            this.logHealthDiff(diff);
            return;
        }

        if (event.getPacket() instanceof S1CPacketEntityMetadata) {
            S1CPacketEntityMetadata packet = (S1CPacketEntityMetadata) event.getPacket();
            if (packet.getEntityId() != mc.thePlayer.getEntityId()) {
                return;
            }

            for (WatchableObject watchableObject : packet.func_149376_c()) {
                if (watchableObject.getDataValueId() == 6) {
                    float diff = (Float) watchableObject.getObject() - mc.thePlayer.getHealth();
                    this.logHealthDiff(diff);
                    return;
                }
            }
        }
    }

    private void logHealthDiff(float diff) {
        if (diff == 0.0F || this.lastTickProcessed == mc.thePlayer.ticksExisted) {
            return;
        }

        this.lastTickProcessed = mc.thePlayer.ticksExisted;
        ChatUtil.sendFormatted(
                String.format(
                        "%sHealth: %s&l%s&r (&otick: %d&r)&r",
                        Myau.clientName,
                        diff > 0.0F ? "&a" : "&c",
                        DF.format(diff),
                        mc.thePlayer.ticksExisted
                )
        );
    }

    @Override
    public void onEnabled() {
        this.lastTickProcessed = -1;
    }

    @Override
    public void onDisabled() {
        this.lastTickProcessed = -1;
    }
}
