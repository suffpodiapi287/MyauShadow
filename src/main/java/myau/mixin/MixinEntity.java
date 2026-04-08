package myau.mixin;

import myau.Myau;
import myau.event.EventManager;
import myau.events.KnockbackEvent;
import myau.events.SafeWalkEvent;
import myau.management.ITruePositionEntity;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {Entity.class}, priority = 9999)
public abstract class MixinEntity implements ITruePositionEntity {
    @Shadow
    public World worldObj;
    @Shadow
    public double posX;
    @Shadow
    public double posY;
    @Shadow
    public double posZ;
    @Shadow
    public double motionX;
    @Shadow
    public double motionY;
    @Shadow
    public double motionZ;
    @Shadow
    public float rotationYaw;
    @Shadow
    public float rotationPitch;
    @Shadow
    public float prevRotationYaw;
    @Shadow
    public float prevRotationPitch;
    @Shadow
    public boolean onGround;

    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double trueX;
    private double trueY;
    private double trueZ;
    private boolean truePosition;

    @Shadow
    public boolean isRiding() {
        return false;
    }

    @Inject(
            method = {"setVelocity"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void setVelocity(double double1, double double2, double double3, CallbackInfo callbackInfo) {
        if ((Entity) ((Object) this) instanceof EntityPlayerSP) {
            KnockbackEvent event = new KnockbackEvent(double1, double2, double3);
            EventManager.call(event);
            if (event.isCancelled()) {
                callbackInfo.cancel();
                this.motionX = event.getX();
                this.motionY = event.getY();
                this.motionZ = event.getZ();
            }
        }
    }

    @Inject(
            method = {"setAngles"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void setAngles(CallbackInfo callbackInfo) {
        if ((Entity) ((Object) this) instanceof EntityPlayerSP && Myau.rotationManager != null && Myau.rotationManager.isRotated()) {
            callbackInfo.cancel();
        }
    }

    @ModifyVariable(
            method = {"moveEntity"},
            ordinal = 0,
            at = @At("STORE"),
            name = {"flag"}
    )
    private boolean moveEntity(boolean boolean1) {
        if ((Entity) ((Object) this) instanceof EntityPlayerSP) {
            SafeWalkEvent event = new SafeWalkEvent(boolean1);
            EventManager.call(event);
            return event.isSafeWalk();
        } else {
            return boolean1;
        }
    }

    @Override
    public double getLerpX() {
        return this.lerpX;
    }

    @Override
    public void setLerpX(double value) {
        this.lerpX = value;
    }

    @Override
    public double getLerpY() {
        return this.lerpY;
    }

    @Override
    public void setLerpY(double value) {
        this.lerpY = value;
    }

    @Override
    public double getLerpZ() {
        return this.lerpZ;
    }

    @Override
    public void setLerpZ(double value) {
        this.lerpZ = value;
    }

    @Override
    public double getTrueX() {
        return this.trueX;
    }

    @Override
    public void setTrueX(double value) {
        this.trueX = value;
    }

    @Override
    public double getTrueY() {
        return this.trueY;
    }

    @Override
    public void setTrueY(double value) {
        this.trueY = value;
    }

    @Override
    public double getTrueZ() {
        return this.trueZ;
    }

    @Override
    public void setTrueZ(double value) {
        this.trueZ = value;
    }

    @Override
    public boolean hasTruePosition() {
        return this.truePosition;
    }

    @Override
    public void setTruePosition(boolean value) {
        this.truePosition = value;
    }
}
