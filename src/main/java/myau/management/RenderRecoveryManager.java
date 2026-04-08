package myau.management;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

public class RenderRecoveryManager {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private EntityPlayerSP lastPlayer = null;
    private boolean wasDead = false;

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.POST) {
            return;
        }

        if (mc.theWorld == null || mc.thePlayer == null) {
            this.lastPlayer = null;
            this.wasDead = false;
            return;
        }

        EntityPlayerSP player = mc.thePlayer;
        boolean isDead = player.isDead || player.getHealth() <= 0.0F;
        boolean playerChanged = this.lastPlayer != null && this.lastPlayer != player;
        boolean respawned = this.wasDead && !isDead;

        if ((playerChanged || respawned) && mc.renderGlobal != null) {
            mc.renderGlobal.loadRenderers();
        }

        this.lastPlayer = player;
        this.wasDead = isDead;
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.lastPlayer = null;
        this.wasDead = false;
    }
}
