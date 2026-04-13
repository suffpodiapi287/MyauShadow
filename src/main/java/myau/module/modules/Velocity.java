package myau.module.modules;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.enums.DelayModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.*;
import myau.mixin.IAccessorEntity;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.ChatUtil;
import myau.util.KeyBindUtil;
import myau.util.MoveUtil;
import myau.util.PacketUtil;
import myau.util.PlayerUtil;
import myau.util.RandomUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;

public class Velocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int GRIM_REDUCE_WINDOW = 14;
    private static final float GRIM_REDUCE_RANGE = 7.0F;
    private static final int MODE_VANILLA = 0;
    private static final int MODE_JUMP = 1;
    private static final int MODE_JUMP2 = 2;
    private static final int MODE_DELAY = 3;
    private static final int MODE_REVERSE = 4;
    private static final int MODE_LEGIT_TEST = 5;
    private static final int MODE_LEGIT = 6;
    private static final int MODE_INTAVE_14_3_3 = 8;
    private static final int MODE_PREDICTION_A = 9;
    private static final int MODE_GRIM_REDUCE = 10;
    private static final int STATE_BOTH = 0;
    private static final int STATE_GROUND = 1;
    private static final int STATE_AIR = 2;

    private int chanceCounter = 0;
    private int delayChanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean jumpFlag = false;
    private boolean reverseFlag = false;
    private boolean delayActive = false;

    private boolean shouldJump = false;
    private int jumpCooldown = 0;
    private boolean legitPending = false;
    private boolean intave1433Pending = false;
    private int intave1433Stage = 0;
    private boolean predictionPending = false;
    private boolean predictionClicked = false;
    private boolean smartIntaveReceivedVelocity = false;
    private int intaveTick = 0;
    private int intaveDamageTick = 0;
    private int grimReduceTicks = 0;

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"VANILLA", "JUMP", "JUMP2", "DELAY", "REVERSE", "LEGIT_TEST", "LEGIT", "INTAVE14_3_3", "PREDICTION_A", "GRIM_REDUCE"});
    public final ModeProperty velocityState = new ModeProperty("state", 0, new String[]{"BOTH", "GROUND", "AIR"});
    public final IntProperty delayTicks = new IntProperty("delay-ticks", 3, 1, 20, () -> this.mode.getValue() == MODE_DELAY);
    public final PercentProperty delayChance = new PercentProperty("delay-chance", 100, () -> this.mode.getValue() == MODE_DELAY);
    public final PercentProperty chance = new PercentProperty("chance", 100, this::usesChanceSetting);
    public final PercentProperty horizontal = new PercentProperty("horizontal", 0, this::usesHorizontalVerticalSettings);
    public final PercentProperty vertical = new PercentProperty("vertical", 100, this::usesHorizontalVerticalSettings);
    public final PercentProperty explosionHorizontal = new PercentProperty("explosions-horizontal", 100);
    public final PercentProperty explosionVertical = new PercentProperty("explosions-vertical", 100);
    public final BooleanProperty fakeCheck = new BooleanProperty("fake-check", true, this::usesFakeCheckSetting);
    public final BooleanProperty debugLog = new BooleanProperty("debug-log", false);
    public final BooleanProperty legitDisableInAir = new BooleanProperty("disable-in-air", true, () -> this.mode.getValue() == MODE_LEGIT);
    public final IntProperty predictionMinClicks = new IntProperty("prediction-min-clicks", 1, 1, 20, () -> this.mode.getValue() == MODE_PREDICTION_A);
    public final IntProperty predictionMaxClicks = new IntProperty("prediction-max-clicks", 2, 1, 20, () -> this.mode.getValue() == MODE_PREDICTION_A);
    public final BooleanProperty grimReduceRequireSwing = new BooleanProperty("require-swing", false, () -> this.mode.getValue() == MODE_GRIM_REDUCE);

    private boolean isInLiquidOrWeb() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb();
    }

    private boolean canDelay() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        return mc.thePlayer.onGround && (!killAura.isEnabled() || !killAura.shouldAutoBlock());
    }

    public Velocity() {
        super("Velocity", false);
    }

    private boolean usesChanceSetting() {
        int mode = this.mode.getValue();
        return mode == MODE_VANILLA
                || mode == MODE_JUMP
                || mode == MODE_DELAY
                || mode == MODE_REVERSE
                || mode == MODE_LEGIT_TEST
                || mode == MODE_LEGIT;
    }

    private boolean usesHorizontalVerticalSettings() {
        int mode = this.mode.getValue();
        return mode == MODE_VANILLA
                || mode == MODE_JUMP
                || mode == MODE_DELAY
                || mode == MODE_REVERSE
                || mode == MODE_LEGIT_TEST
                || mode == MODE_LEGIT;
    }

    private boolean usesFakeCheckSetting() {
        int mode = this.mode.getValue();
        return mode == MODE_VANILLA
                || mode == MODE_JUMP
                || mode == MODE_DELAY
                || mode == MODE_REVERSE
                || mode == MODE_LEGIT_TEST
                || mode == MODE_LEGIT
                || mode == MODE_INTAVE_14_3_3
                || mode == MODE_PREDICTION_A
                || mode == MODE_GRIM_REDUCE;
    }

    private boolean rollChance(int chance) {
        return Math.random() * 100.0D < (double) chance;
    }

    private boolean isNearGround(double offset) {
        return !mc.theWorld.getCollidingBoundingBoxes(
                mc.thePlayer,
                mc.thePlayer.getEntityBoundingBox().offset(0.0D, -offset, 0.0D)
        ).isEmpty();
    }

    private boolean passesVelocityState() {
        int state = this.velocityState.getValue();
        return state == STATE_BOTH
                || (state == STATE_GROUND ? mc.thePlayer.onGround : !mc.thePlayer.onGround);
    }

    private boolean shouldCheckVelocityState(double x, double y, double z) {
        return y > 0.0D && (x != 0.0D || z != 0.0D);
    }

    private boolean shouldCheckVelocityState(S12PacketEntityVelocity packet) {
        return packet.getMotionY() > 0 && (packet.getMotionX() != 0 || packet.getMotionZ() != 0);
    }

    private boolean shouldCheckVelocityState(S27PacketExplosion packet) {
        return (mc.thePlayer.motionY + (double) packet.func_149144_d()) > 0.0D
                && ((mc.thePlayer.motionX + (double) packet.func_149149_c()) != 0.0D
                || (mc.thePlayer.motionZ + (double) packet.func_149147_e()) != 0.0D);
    }

    private void restoreMovementKeys() {
        if (mc.currentScreen == null) {
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), GameSettings.isKeyDown(mc.gameSettings.keyBindForward));
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), GameSettings.isKeyDown(mc.gameSettings.keyBindBack));
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), GameSettings.isKeyDown(mc.gameSettings.keyBindJump));
        }
    }

    private void armSmartIntavePredLegit() {
        this.legitPending = true;
        this.predictionPending = true;
        this.predictionClicked = false;
        this.smartIntaveReceivedVelocity = true;
        this.intaveTick = 0;
    }

    private boolean isValidPredictionTarget(EntityLivingBase target) {
        return this.isValidCombatTarget(target, 3.0F);
    }

    private boolean isValidCombatTarget(EntityLivingBase target, float range) {
        if (target == null || target == mc.thePlayer || target.isDead || target.deathTime > 0) {
            return false;
        }

        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) target;
            if (TeamUtil.isFriend(player) || TeamUtil.isBot(player) || TeamUtil.isSameTeam(player)) {
                return false;
            }
        }

        return mc.thePlayer.getDistanceToEntity(target) <= range;
    }

    private EntityLivingBase findPredictionTarget() {
        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            EntityLivingBase target = (EntityLivingBase) mc.objectMouseOver.entityHit;
            if (this.isValidPredictionTarget(target)) {
                return target;
            }
        }

        EntityLivingBase nearest = null;
        double nearestDistance = 3.0D;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase target = (EntityLivingBase) entity;
                if (this.isValidPredictionTarget(target)) {
                    double distance = mc.thePlayer.getDistanceToEntity(target);
                    if (distance <= nearestDistance) {
                        nearestDistance = distance;
                        nearest = target;
                    }
                }
            }
        }

        return nearest;
    }

    private boolean hasBadPacketState() {
        return Myau.playerStateManager != null
                && (Myau.playerStateManager.attacking
                || Myau.playerStateManager.digging
                || Myau.playerStateManager.placing
                || Myau.playerStateManager.swapping
                || Myau.playerStateManager.swinging);
    }

    private EntityLivingBase findGrimReduceTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura != null) {
            EntityLivingBase killAuraTarget = killAura.getTarget();
            if (this.isValidCombatTarget(killAuraTarget, GRIM_REDUCE_RANGE)) {
                return killAuraTarget;
            }
        }

        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            EntityLivingBase target = (EntityLivingBase) mc.objectMouseOver.entityHit;
            if (this.isValidCombatTarget(target, GRIM_REDUCE_RANGE)) {
                return target;
            }
        }

        EntityLivingBase nearest = null;
        double nearestDistance = GRIM_REDUCE_RANGE;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase target = (EntityLivingBase) entity;
                if (this.isValidCombatTarget(target, GRIM_REDUCE_RANGE)) {
                    double distance = mc.thePlayer.getDistanceToEntity(target);
                    if (distance <= nearestDistance) {
                        nearestDistance = distance;
                        nearest = target;
                    }
                }
            }
        }

        return nearest;
    }

    @EventTarget(Priority.LOWEST)
    public void onGrimReduceUpdate(UpdateEvent event) {
        if (!this.isEnabled()
                || event.getType() != EventType.PRE
                || mc.thePlayer == null
                || mc.theWorld == null
                || this.mode.getValue() != MODE_GRIM_REDUCE) {
            return;
        }

        if (this.grimReduceTicks <= 0) {
            return;
        }

        --this.grimReduceTicks;

        if (mc.thePlayer.ticksExisted <= 20) {
            return;
        }

        if (this.grimReduceRequireSwing.getValue() && !mc.thePlayer.isSwingInProgress) {
            return;
        }

        if (this.hasBadPacketState()) {
            return;
        }

        EntityLivingBase target = this.findGrimReduceTarget();
        if (target == null) {
            return;
        }

        PacketUtil.sendPacket(new C0APacketAnimation());
        mc.playerController.attackEntity(mc.thePlayer, target);
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            this.pendingExplosion = false;
            this.allowNext = true;
        } else if (!this.allowNext || !(Boolean) this.fakeCheck.getValue()) {
            this.allowNext = true;
            if (this.shouldCheckVelocityState(event.getX(), event.getY(), event.getZ()) && !this.passesVelocityState()) {
                this.pendingExplosion = false;
                return;
            }
            if (this.pendingExplosion) {
                this.pendingExplosion = false;
                if (this.explosionHorizontal.getValue() > 0) {
                    event.setX(event.getX() * (double) this.explosionHorizontal.getValue() / 100.0);
                    event.setZ(event.getZ() * (double) this.explosionHorizontal.getValue() / 100.0);
                } else {
                    event.setX(mc.thePlayer.motionX);
                    event.setZ(mc.thePlayer.motionZ);
                }
                if (this.explosionVertical.getValue() > 0) {
                    event.setY(event.getY() * (double) this.explosionVertical.getValue() / 100.0);
                } else {
                    event.setY(mc.thePlayer.motionY);
                }
            } else if (this.mode.getValue() == MODE_LEGIT) {
                this.legitPending = true;
            } else if (this.mode.getValue() == MODE_INTAVE_14_3_3) {
                this.intave1433Pending = true;
                this.intave1433Stage = 0;
            } else if (this.mode.getValue() == MODE_PREDICTION_A) {
                this.predictionPending = true;
                this.predictionClicked = false;
            } else if (this.mode.getValue() == MODE_GRIM_REDUCE) {
                this.grimReduceTicks = GRIM_REDUCE_WINDOW;
            } else {
                this.chanceCounter = this.chanceCounter % 100 + this.chance.getValue();
                if (this.chanceCounter >= 100) {
                    this.jumpFlag = (this.mode.getValue() == MODE_JUMP || this.mode.getValue() == MODE_DELAY) && event.getY() > 0.0;
                    this.delayActive = this.mode.getValue() == MODE_REVERSE;
                    if (this.horizontal.getValue() > 0) {
                        event.setX(event.getX() * (double) this.horizontal.getValue() / 100.0);
                        event.setZ(event.getZ() * (double) this.horizontal.getValue() / 100.0);
                    } else {
                        event.setX(mc.thePlayer.motionX);
                        event.setZ(mc.thePlayer.motionZ);
                    }
                    if (this.vertical.getValue() > 0) {
                        event.setY(event.getY() * (double) this.vertical.getValue() / 100.0);
                    } else {
                        event.setY(mc.thePlayer.motionY);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.POST) {
            if (this.reverseFlag
                    && (
                    this.canDelay()
                            || this.isInLiquidOrWeb()
                            || Myau.delayManager.getDelay() >= (long) this.delayTicks.getValue()
            )) {
                Myau.delayManager.setDelayState(false, DelayModules.VELOCITY);
                this.reverseFlag = false;
            }
            if (this.delayActive) {
                MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                this.delayActive = false;
            }

            if (this.mode.getValue() == MODE_LEGIT_TEST) {
                int hurtTime = mc.thePlayer.hurtTime;

                if (hurtTime >= 8) {
                    if (jumpCooldown <= 0) {
                        shouldJump = true;
                        jumpCooldown = 2;
                    }
                } else if (hurtTime <= 1) {
                    shouldJump = false;
                    jumpCooldown = 0;
                }

                if (shouldJump && mc.thePlayer.onGround && jumpCooldown <= 0) {
                    mc.thePlayer.jump();
                    shouldJump = false;
                }

                if (jumpCooldown > 0) {
                    jumpCooldown--;
                }
            }

            if (this.mode.getValue() == MODE_LEGIT && this.legitPending) {
                boolean canUseLegit = !this.legitDisableInAir.getValue() || this.isNearGround(0.5D);
                if (!canUseLegit && this.mode.getValue() == MODE_LEGIT) {
                    this.legitPending = false;
                } else if (canUseLegit && mc.thePlayer.maxHurtResistantTime == mc.thePlayer.hurtResistantTime && mc.thePlayer.maxHurtResistantTime != 0) {
                    if (this.rollChance(this.chance.getValue())) {
                        double horizontalScale = (double) this.horizontal.getValue() / 100.0D;
                        double verticalScale = (double) this.vertical.getValue() / 100.0D;
                        mc.thePlayer.motionX *= horizontalScale;
                        mc.thePlayer.motionZ *= horizontalScale;
                        mc.thePlayer.motionY *= verticalScale;
                    }
                    this.legitPending = false;
                } else if (mc.thePlayer.hurtResistantTime <= 0) {
                    this.legitPending = false;
                }
            }

            if (this.mode.getValue() == MODE_INTAVE_14_3_3 && this.intave1433Pending) {
                if (this.intave1433Stage == 0 && mc.thePlayer.hurtTime == 10) {
                    mc.thePlayer.motionX *= -1.0D;
                    mc.thePlayer.motionZ *= -1.0D;
                    this.intave1433Stage = 1;
                } else if (this.intave1433Stage == 1 && mc.thePlayer.hurtTime == 9 && mc.thePlayer.onGround) {
                    mc.thePlayer.motionX *= 0.9D;
                    mc.thePlayer.motionZ *= 0.9D;
                    this.intave1433Pending = false;
                    this.intave1433Stage = 0;
                } else if (mc.thePlayer.hurtTime <= 0) {
                    this.intave1433Pending = false;
                    this.intave1433Stage = 0;
                }
            }

            if (this.mode.getValue() == MODE_PREDICTION_A && this.predictionPending) {
                if (!PlayerUtil.isJumping()
                        && mc.thePlayer.isSprinting()
                        && mc.thePlayer.onGround
                        && mc.thePlayer.hurtTime == 9) {
                    mc.thePlayer.jump();
                    this.predictionPending = false;
                    this.predictionClicked = false;
                } else if (mc.thePlayer.hurtTime <= 0) {
                    this.predictionPending = false;
                    this.predictionClicked = false;
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()
                || event.getType() != EventType.POST
                || mc.thePlayer == null
                || mc.theWorld == null
                || this.mode.getValue() != MODE_PREDICTION_A
                || !this.predictionPending
                || this.predictionClicked
                || mc.thePlayer.hurtTime != 10
                || mc.thePlayer.isBlocking()) {
            return;
        }

        EntityLivingBase target = this.findPredictionTarget();
        if (target == null) {
            return;
        }

        int minClicks = Math.min(this.predictionMinClicks.getValue(), this.predictionMaxClicks.getValue());
        int maxClicks = Math.max(this.predictionMinClicks.getValue(), this.predictionMaxClicks.getValue());
        int clicks = (int) RandomUtil.nextLong(minClicks, maxClicks);

        for (int i = 0; i < clicks; ++i) {
            mc.thePlayer.swingItem();
            PlayerUtil.attackEntity(target);
        }

        this.predictionClicked = true;
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer.onGround && mc.thePlayer.isSprinting() && !mc.thePlayer.isPotionActive(Potion.jump) && !this.isInLiquidOrWeb()) {
                mc.thePlayer.movementInput.jump = true;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
                if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                    if (this.shouldCheckVelocityState(packet) && !this.passesVelocityState()) {
                        return;
                    }

                    if (this.mode.getValue() == MODE_JUMP2) {
                        if (!this.rollChance(this.chance.getValue())) {
                            return;
                        }

                        event.setCancelled(true);
                        this.jumpFlag = packet.getMotionY() > 0;
                        return;
                    }

                    LongJump longJump = (LongJump) Myau.moduleManager.modules.get(LongJump.class);
                    if (this.mode.getValue() == MODE_DELAY
                            && !this.reverseFlag
                            && !this.canDelay()
                            && !this.isInLiquidOrWeb()
                            && !this.pendingExplosion
                            && (!this.allowNext || !(Boolean) this.fakeCheck.getValue())
                            && (!longJump.isEnabled() || !longJump.canStartJump())) {
                        this.delayChanceCounter = this.delayChanceCounter % 100 + this.delayChance.getValue();
                        if (this.delayChanceCounter >= 100) {
                            Myau.delayManager.setDelayState(true, DelayModules.VELOCITY);
                            Myau.delayManager.delayedPacket.offer(packet);
                            event.setCancelled(true);
                            this.reverseFlag = true;
                            return;
                        }
                    }
                    if (this.debugLog.getValue()) {
                        ChatUtil.sendFormatted(
                                String.format(
                                        "%sVelocity (&otick: %d, x: %.2f, y: %.2f, z: %.2f&r)&r",
                                        Myau.clientName,
                                        mc.thePlayer.ticksExisted,
                                        (double) packet.getMotionX() / 8000.0,
                                        (double) packet.getMotionY() / 8000.0,
                                        (double) packet.getMotionZ() / 8000.0
                                )
                        );
                    }
                }
            } else if (!(event.getPacket() instanceof S27PacketExplosion)) {
                if (event.getPacket() instanceof S19PacketEntityStatus) {
                    S19PacketEntityStatus packet = (S19PacketEntityStatus) event.getPacket();
                    Entity entity = packet.getEntity(mc.theWorld);
                    if (entity != null && entity.equals(mc.thePlayer) && packet.getOpCode() == 2) {
                        this.allowNext = false;
                    }
                }
            } else {
                S27PacketExplosion packet = (S27PacketExplosion) event.getPacket();
                if (packet.func_149149_c() != 0.0F || packet.func_149144_d() != 0.0F || packet.func_149147_e() != 0.0F) {
                    if (this.shouldCheckVelocityState(packet) && !this.passesVelocityState()) {
                        return;
                    }

                    if (this.mode.getValue() == MODE_JUMP2) {
                        if (!this.rollChance(this.chance.getValue())) {
                            return;
                        }

                        event.setCancelled(true);
                        this.jumpFlag = packet.func_149144_d() > 0.0F;
                        return;
                    }

                    this.pendingExplosion = true;
                    if (this.explosionHorizontal.getValue() == 0 || this.explosionVertical.getValue() == 0) {
                        event.setCancelled(true);
                    }
                    if (this.debugLog.getValue()) {
                        ChatUtil.sendFormatted(
                                String.format(
                                        "%sExplosion (&otick: %d, x: %.2f, y: %.2f, z: %.2f&r)&r",
                                        Myau.clientName,
                                        mc.thePlayer.ticksExisted,
                                        mc.thePlayer.motionX + (double) packet.func_149149_c(),
                                        mc.thePlayer.motionY + (double) packet.func_149144_d(),
                                        mc.thePlayer.motionZ + (double) packet.func_149147_e()
                                )
                        );
                    }
                }
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.onDisabled();
    }

    @Override
    public void onDisabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.chanceCounter = 0;
        this.delayChanceCounter = 0;
        this.jumpFlag = false;
        this.reverseFlag = false;
        this.delayActive = false;
        this.shouldJump = false;
        this.jumpCooldown = 0;
        this.legitPending = false;
        this.intave1433Pending = false;
        this.intave1433Stage = 0;
        this.predictionPending = false;
        this.predictionClicked = false;
        this.smartIntaveReceivedVelocity = false;
        this.intaveTick = 0;
        this.intaveDamageTick = 0;
        this.grimReduceTicks = 0;
        this.restoreMovementKeys();
    }

    @Override
    public void verifyValue(String value) {
        if (this.predictionMinClicks.getName().equals(value) && this.predictionMinClicks.getValue() > this.predictionMaxClicks.getValue()) {
            this.predictionMaxClicks.setValue(this.predictionMinClicks.getValue());
        } else if (this.predictionMaxClicks.getName().equals(value) && this.predictionMaxClicks.getValue() < this.predictionMinClicks.getValue()) {
            this.predictionMinClicks.setValue(this.predictionMaxClicks.getValue());
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
