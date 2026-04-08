package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LivingUpdateEvent;
import myau.events.TickEvent;
import myau.management.RotationState;
import myau.mixin.IAccessorEntityLivingBase;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.util.KeyBindUtil;
import myau.util.MoveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.world.WorldSettings;

public class Sprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean wasSprinting = false;
    public final BooleanProperty foxFix = new BooleanProperty("fov-fix", true);
    public final BooleanProperty omni = new BooleanProperty("omni", true);
    public final BooleanProperty rotationsCheck = new BooleanProperty("rotations-check", false);
    public final BooleanProperty noCheckWhenScaffold = new BooleanProperty("no-check-when-scaffold", false, this.rotationsCheck::getValue);

    public Sprint() {
        super("Sprint", true, true);
    }

    public boolean shouldApplyFovFix(IAttributeInstance attribute) {
        if (!this.foxFix.getValue()) {
            return false;
        }

        AttributeModifier attributeModifier = ((IAccessorEntityLivingBase) mc.thePlayer).getSprintingSpeedBoostModifier();
        return attribute.getModifier(attributeModifier.getID()) == null && this.wasSprinting;
    }

    public boolean shouldKeepFov(boolean defaultState) {
        return this.foxFix.getValue() && !defaultState && this.wasSprinting;
    }

    private boolean shouldNotSprint() {
        if (mc.thePlayer == null) {
            return true;
        }

        NoSlow noSlow = (NoSlow) Myau.moduleManager.modules.get(NoSlow.class);
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);

        float inputYaw = MoveUtil.adjustYaw(mc.thePlayer.rotationYaw, (float) MoveUtil.getForwardValue(), (float) MoveUtil.getLeftValue());
        float yawDiff = Math.abs(MoveUtil.getAngleDifference(inputYaw, RotationState.getSmoothedYaw()));

        boolean lowFood = mc.thePlayer.getFoodStats().getFoodLevel() <= 6
                && mc.playerController != null
                && (mc.playerController.getCurrentGameType() == WorldSettings.GameType.SURVIVAL
                || mc.playerController.getCurrentGameType() == WorldSettings.GameType.ADVENTURE);
        boolean rotationsBlocking = RotationState.isActived()
                && this.rotationsCheck.getValue()
                && yawDiff > 30.0F
                && (!this.noCheckWhenScaffold.getValue() || scaffold == null || !scaffold.isEnabled());
        boolean noSlowBlocking = mc.thePlayer.isUsingItem() && noSlow != null && (!noSlow.isEnabled() || !noSlow.canSprint());
        boolean scaffoldBlocking = scaffold != null && scaffold.isEnabled() && scaffold.shouldStopSprintNow();

        return !MoveUtil.isForwardPressed()
                || mc.thePlayer.isSneaking()
                || lowFood
                || mc.thePlayer.isCollidedHorizontally
                || (this.isEnabled() && !this.omni.getValue() && !mc.gameSettings.keyBindForward.isKeyDown())
                || rotationsBlocking
                || noSlowBlocking
                || scaffoldBlocking;
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && mc.thePlayer != null) {
            mc.thePlayer.setSprinting(!this.shouldNotSprint());
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST && mc.thePlayer != null) {
            this.wasSprinting = mc.thePlayer.isSprinting();
        }
    }

    @Override
    public void onDisabled() {
        this.wasSprinting = false;
        KeyBindUtil.updateKeyState(mc.gameSettings.keyBindSprint.getKeyCode());
    }
}
