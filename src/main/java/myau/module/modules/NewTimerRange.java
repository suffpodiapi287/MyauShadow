package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.LoadWorldEvent;
import myau.events.Render3DEvent;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class NewTimerRange extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty delay = new IntProperty("delay", 100, 0, 3000);
    public final FloatProperty farStartRange = new FloatProperty("far-start-range", 3.8F, 0.1F, 10.0F);
    public final FloatProperty nearToStopDistance = new FloatProperty("near-to-stop-distance", 3.0F, 0.1F, 8.0F);
    public final FloatProperty timerBoost = new FloatProperty("timer-boost", 10.0F, 1.1F, 35.0F);
    public final FloatProperty lowTimer = new FloatProperty("low-timer", 0.2F, 0.0F, 0.99F);
    public final IntProperty slowTick = new IntProperty("slow-tick", 10, 0, 10);
    public final IntProperty fastTick = new IntProperty("fast-tick", 10, 0, 10);
    public final BooleanProperty onlyMoveForward = new BooleanProperty("only-move-forward", false);
    public final BooleanProperty onlyOnGround = new BooleanProperty("only-on-ground", false);
    public final BooleanProperty disableOnWorld = new BooleanProperty("disable-on-world", false);

    private final TimerUtil timeDelay = new TimerUtil();
    private long balance = 0L;
    private long lastLagTime = 0L;
    private long fastBalance = 0L;

    public NewTimerRange() {
        super("NewTimerRange", false);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (((IAccessorMinecraft) mc).getTimer().timerSpeed > 1.0F) {
            this.fastBalance++;
            if (this.fastBalance > this.fastTick.getValue()) {
                ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
                this.timeDelay.reset();
                this.fastBalance = 0L;
            }
        }

        if (this.shouldStop()) {
            if (((IAccessorMinecraft) mc).getTimer().timerSpeed != 1.0F) {
                ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
            }
        }

        final float partialTicks = ((IAccessorMinecraft) mc).getTimer().renderPartialTicks;
        for (Object loadedEntity : mc.theWorld.loadedEntityList) {
            if (!(loadedEntity instanceof EntityLivingBase)) {
                continue;
            }

            EntityLivingBase living = (EntityLivingBase) loadedEntity;
            final float width = living.width / 2.0F;
            final float height = living.height;
            final double posXNow = living.lastTickPosX + (living.posX - living.lastTickPosX) * partialTicks;
            final double posYNow = living.lastTickPosY + (living.posY - living.lastTickPosY) * partialTicks;
            final double posZNow = living.lastTickPosZ + (living.posZ - living.lastTickPosZ) * partialTicks;
            final double posX = living.posX;
            final double posY = living.posY;
            final double posZ = living.posZ;

            final AxisAlignedBB possibleBoundingBox = new AxisAlignedBB(
                    posX - width,
                    posY,
                    posZ - width,
                    posX + width,
                    posY + height,
                    posZ + width
            );
            final Vec3 positionEyes = mc.thePlayer.getPositionEyes(3.0F);
            final double bestX = MathHelper.clamp_double(positionEyes.xCoord, possibleBoundingBox.minX, possibleBoundingBox.maxX);
            final double bestY = MathHelper.clamp_double(positionEyes.yCoord, possibleBoundingBox.minY, possibleBoundingBox.maxY);
            final double bestZ = MathHelper.clamp_double(positionEyes.zCoord, possibleBoundingBox.minZ, possibleBoundingBox.maxZ);

            final AxisAlignedBB boundingBoxNow = new AxisAlignedBB(
                    posXNow - width,
                    posYNow,
                    posZNow - width,
                    posXNow + width,
                    posYNow + height,
                    posZNow + width
            );
            final double currentX = MathHelper.clamp_double(positionEyes.xCoord, boundingBoxNow.minX, boundingBoxNow.maxX);
            final double currentY = MathHelper.clamp_double(positionEyes.yCoord, boundingBoxNow.minY, boundingBoxNow.maxY);
            final double currentZ = MathHelper.clamp_double(positionEyes.zCoord, boundingBoxNow.minZ, boundingBoxNow.maxZ);

            final Vec3 currentPosEyes = mc.thePlayer.getPositionEyes(1.0F);
            final Vec3 targetEyes = living.getPositionEyes(1.0F);

            final Vec3 myPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
            final double diffX = mc.thePlayer.prevPosX - mc.thePlayer.posX;
            final double diffZ = mc.thePlayer.prevPosZ - mc.thePlayer.posZ;
            final Vec3 myPosBest = myPos.addVector(-diffX * 2.0, 0.0, -diffZ * 2.0);
            final Vec3 myPosBestLast = myPos.addVector(-diffX, 0.0, -diffZ);
            final double myPosForTargetX = myPosBestLast.xCoord + (myPosBest.xCoord - myPosBestLast.xCoord) / 3.0;
            final double myPosForTargetY = myPosBestLast.yCoord + (myPosBest.yCoord - myPosBestLast.yCoord) / 3.0;
            final double myPosForTargetZ = myPosBestLast.zCoord + (myPosBest.zCoord - myPosBestLast.zCoord) / 3.0;
            final float myWidth = living.width / 2.0F;
            final AxisAlignedBB myBB = new AxisAlignedBB(
                    myPosForTargetX - myWidth,
                    myPosForTargetY,
                    myPosForTargetZ - myWidth,
                    myPosForTargetX + myWidth,
                    myPosForTargetY + height,
                    myPosForTargetZ + myWidth
            );
            final double myBestX = MathHelper.clamp_double(targetEyes.xCoord, myBB.minX, myBB.maxX);
            final double myBestY = MathHelper.clamp_double(targetEyes.yCoord, myBB.minY, myBB.maxY);
            final double myBestZ = MathHelper.clamp_double(targetEyes.zCoord, myBB.minZ, myBB.maxZ);

            if (this.shouldStop()) {
                return;
            }

            if (this.timeDelay.hasTimeElapsed(this.delay.getValue())) {
                if (mc.thePlayer.hurtTime == 0
                        && currentPosEyes.distanceTo(new Vec3(currentX, currentY, currentZ)) <= this.farStartRange.getValue()
                        && targetEyes.distanceTo(new Vec3(myBestX, myBestY, myBestZ)) <= this.farStartRange.getValue()
                        && positionEyes.distanceTo(new Vec3(bestX, bestY, bestZ)) >= this.nearToStopDistance.getValue()
                        && this.isMoving()) {
                    if (this.balance < this.slowTick.getValue()) {
                        ((IAccessorMinecraft) mc).getTimer().timerSpeed = this.lowTimer.getValue();
                        if (System.currentTimeMillis() - this.lastLagTime >= 50L) {
                            this.balance++;
                            this.lastLagTime = System.currentTimeMillis();
                        }
                        return;
                    }

                    for (int i = 0; i < this.fastTick.getValue(); i++) {
                        mc.thePlayer.onUpdate();
                        ((IAccessorMinecraft) mc).getTimer().timerSpeed = this.timerBoost.getValue();
                    }
                    this.balance = 0L;
                }
            }
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        if (this.disableOnWorld.getValue()) {
            this.setEnabled(false);
        }
    }

    private boolean shouldStop() {
        if (mc.thePlayer == null) {
            return true;
        }

        if (this.onlyMoveForward.getValue() && !mc.gameSettings.keyBindForward.isKeyDown()) {
            return true;
        }

        if (this.onlyOnGround.getValue() && !mc.thePlayer.onGround) {
            return true;
        }

        KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
        EntityLivingBase target = killAura == null ? null : killAura.getTarget();
        if (target == null || !mc.theWorld.loadedEntityList.contains(target) || target.isDead || target.deathTime > 0) {
            return true;
        }

        if (mc.thePlayer.getDistanceToEntity(target) <= this.nearToStopDistance.getValue()) {
            return true;
        }

        return mc.thePlayer.hurtTime != 0;
    }

    private boolean isMoving() {
        return mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F;
    }

    @Override
    public void onEnabled() {
        this.balance = 0L;
        ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
        this.lastLagTime = 0L;
        this.fastBalance = 0L;
    }

    @Override
    public void onDisabled() {
        this.balance = 0L;
        if (mc.thePlayer != null) {
            ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
        }
        this.lastLagTime = 0L;
        this.fastBalance = 0L;
    }
}
