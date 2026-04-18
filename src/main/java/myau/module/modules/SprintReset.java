package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.MoveInputEvent;
import myau.events.TickEvent;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.KeyBindUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MathHelper;

import java.lang.reflect.Field;

public class SprintReset extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final TimerUtil timer = new TimerUtil();
    private EntityLivingBase target;
    private boolean isBlocking;

    public final ModeProperty mode = new ModeProperty(
            "mode",
            0,
            new String[]{"RESPRINT", "WTAP", "SNEAK", "BLOCK", "PACKET", "LESS_PACKET"}
    );
    public final BooleanProperty fast = new BooleanProperty("fast", false, () -> this.mode.getValue() == 0);
    public final IntProperty resetTime = new IntProperty(
            "reset-time",
            50,
            1,
            300,
            () -> {
                int currentMode = this.mode.getValue();
                return currentMode == 1 || currentMode == 2 || currentMode == 3;
            }
    );
    public final ModeProperty fallbackMode = new ModeProperty(
            "fallback-mode",
            1,
            new String[]{"RESPRINT", "WTAP", "PACKET", "LESS_PACKET"},
            () -> this.mode.getValue() == 3
    );
    public final BooleanProperty angleDiffCheck = new BooleanProperty("angle-diff-check", false);
    public final BooleanProperty notWhileHurt = new BooleanProperty("not-while-hurt", false);

    public SprintReset() {
        super("SprintReset", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null || event.getType() != EventType.PRE) {
            return;
        }

        this.target = this.resolveTarget(8.0F);
        if (this.target == null) {
            return;
        }

        float calcYaw = (float) (MathHelper.atan2(mc.thePlayer.posZ - this.target.posZ, mc.thePlayer.posX - this.target.posX) * 180.0D / Math.PI - 90.0D);
        float diffX = Math.abs(MathHelper.wrapAngleTo180_float(calcYaw - this.target.rotationYawHead));

        if ((this.angleDiffCheck.getValue() && diffX > 120.0F) || (this.notWhileHurt.getValue() && mc.thePlayer.hurtTime != 0)) {
            return;
        }

        if (this.target.hurtTime == 10) {
            switch (this.mode.getValue()) {
                case 1:
                case 2:
                    this.timer.reset();
                    break;
                case 3:
                    if (this.isHoldingSword()) {
                        this.timer.reset();
                    } else {
                        this.reset(true);
                    }
                    break;
                default:
                    break;
            }

            if (this.mode.getValue() != 1 && this.mode.getValue() != 3 && this.mode.getValue() != 2) {
                this.reset(false);
            }
        }
    }

    private void reset(boolean fallback) {
        int selectedMode = fallback ? this.getFallbackMappedMode() : this.mode.getValue();

        switch (selectedMode) {
            case 0:
                if (!this.fast.getValue()) {
                    if (!this.setPlayerIntField("reSprint", 2)) {
                        if (mc.thePlayer.isSprinting()) {
                            mc.thePlayer.setSprinting(false);
                        }
                        mc.thePlayer.setSprinting(true);
                    }
                } else {
                    mc.thePlayer.sprintingTicksLeft = 0;
                }
                break;
            case 1:
            case 2:
                this.timer.reset();
                break;
            case 4:
                mc.thePlayer.sendQueue.addToSendQueue(
                        new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING)
                );
                mc.thePlayer.sendQueue.addToSendQueue(
                        new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING)
                );
                if (!this.setPlayerBooleanField("serverSprintState", true)) {
                    mc.thePlayer.setSprinting(true);
                }
                break;
            case 5:
                if (mc.thePlayer.isSprinting()) {
                    mc.thePlayer.setSprinting(false);
                }
                mc.getNetHandler().addToSendQueue(
                        new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING)
                );
                if (!this.setPlayerBooleanField("serverSprintState", true)) {
                    mc.thePlayer.setSprinting(true);
                }
                break;
            default:
                break;
        }
    }

    private int getFallbackMappedMode() {
        switch (this.fallbackMode.getValue()) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 4;
            case 3:
                return 5;
            default:
                return 1;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || event.getType() != EventType.PRE) {
            return;
        }

        if (this.mode.getValue() == 3) {
            if (this.target != null) {
                boolean active = !this.timer.hasTimeElapsed(this.resetTime.getValue());
                KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), active);
                this.isBlocking = active;
            } else if (this.isBlocking) {
                KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                this.isBlocking = false;
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (!this.timer.hasTimeElapsed(this.resetTime.getValue())) {
            if (this.mode.getValue() == 1 || (this.mode.getValue() == 3 && this.fallbackMode.getValue() == 1)) {
                mc.thePlayer.movementInput.moveForward = 0.0F;
            }

            if (this.mode.getValue() == 2) {
                mc.thePlayer.movementInput.sneak = true;
            }
        }
    }

    private EntityLivingBase resolveTarget(float maxDistance) {
        EntityLivingBase candidate = null;
        float distance = maxDistance;

        AntiBot antiBot = (AntiBot) Myau.moduleManager.modules.get(AntiBot.class);

        for (EntityPlayer entity : mc.theWorld.playerEntities) {
            if (entity == mc.thePlayer) {
                continue;
            }

            if (antiBot != null && antiBot.isEnabled() && antiBot.isBot(entity)) {
                continue;
            }

            float tempDistance = mc.thePlayer.getDistanceToEntity(entity) - 0.5657F;
            if (tempDistance <= distance) {
                candidate = entity;
                distance = tempDistance;
            }
        }

        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.getTarget() != null) {
            return killAura.getTarget();
        }

        return candidate;
    }

    private boolean isHoldingSword() {
        ItemStack itemStack = mc.thePlayer.getCurrentEquippedItem();
        return itemStack != null && itemStack.getItem() instanceof ItemSword;
    }

    private boolean setPlayerIntField(String fieldName, int value) {
        Field field = this.findField(mc.thePlayer.getClass(), fieldName);
        if (field == null) {
            return false;
        }

        try {
            field.setAccessible(true);
            field.setInt(mc.thePlayer, value);
            return true;
        } catch (IllegalAccessException ignored) {
            return false;
        }
    }

    private boolean setPlayerBooleanField(String fieldName, boolean value) {
        Field field = this.findField(mc.thePlayer.getClass(), fieldName);
        if (field == null) {
            return false;
        }

        try {
            field.setAccessible(true);
            field.setBoolean(mc.thePlayer, value);
            return true;
        } catch (IllegalAccessException ignored) {
            return false;
        }
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }

        return null;
    }

    @Override
    public void onDisabled() {
        if (this.isBlocking) {
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            this.isBlocking = false;
        }

        this.target = null;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
