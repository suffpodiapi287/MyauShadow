package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.util.KeyBindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.EntityLivingBase;

public class AutoWalk extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty stopNearTarget = new BooleanProperty("stop-near-target", true);
    public final FloatProperty stopDistance = new FloatProperty("stop-distance", 2.0F, 0.0F, 3.0F);
    public final BooleanProperty autoSpaceHold = new BooleanProperty("auto-space-hold", true);

    public AutoWalk() {
        super("AutoWalk", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }

        if (mc.thePlayer == null) {
            return;
        }

        EntityLivingBase target = mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase
                ? (EntityLivingBase) mc.objectMouseOver.entityHit
                : null;

        boolean shouldStop = this.stopNearTarget.getValue()
                && target != null
                && target.isEntityAlive()
                && mc.thePlayer.getDistanceToEntity(target) <= this.stopDistance.getValue();
        boolean shouldWalk = !shouldStop;

        KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), shouldWalk);
        KeyBindUtil.setKeyBindState(
                mc.gameSettings.keyBindJump.getKeyCode(),
                this.autoSpaceHold.getValue() && shouldWalk
        );
    }

    private void syncToPhysicalInput() {
        KeyBindUtil.setKeyBindState(
                mc.gameSettings.keyBindForward.getKeyCode(),
                GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
        );
        KeyBindUtil.setKeyBindState(
                mc.gameSettings.keyBindJump.getKeyCode(),
                GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
        );
    }

    @Override
    public void onDisabled() {
        this.syncToPhysicalInput();
    }
}
