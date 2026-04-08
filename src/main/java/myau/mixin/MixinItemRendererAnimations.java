package myau.mixin;

import myau.config.AnimationConfig;
import myau.config.AnimationMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = ItemRenderer.class, priority = 999)
public abstract class MixinItemRendererAnimations {
    private float spin;

    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    protected abstract void transformFirstPersonItem(float equipProgress, float swingProgress);

    @Redirect(
            method = "renderItemInFirstPerson",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemRenderer;transformFirstPersonItem(FF)V",
                    ordinal = 2
            )
    )
    private void skipTransform(ItemRenderer instance, float equipProgress, float swingProgress) {
    }

    @Inject(
            method = "renderItemInFirstPerson",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemRenderer;doBlockTransformations()V"
            )
    )
    private void applyAnimTransform(float partialTicks, CallbackInfo callbackInfo) {
        if (!AnimationConfig.isEnabled()) {
            return;
        }

        AnimationConfig.sync();

        IAccessorItemRendererAnimations accessor = (IAccessorItemRendererAnimations) this;
        float equippedProgress = accessor.getEquippedProgress();
        float prevEquippedProgress = accessor.getPrevEquippedProgress();
        float equip = 1.0F - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks);
        GL11.glTranslated(AnimationConfig.getHandX() / 100.0D, AnimationConfig.getHandY() / 100.0D, 0.0D);

        AbstractClientPlayer player = this.mc.thePlayer;
        float swingProgress = player.getSwingProgress(partialTicks);
        float sine = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        float sqrtSwing = MathHelper.sqrt_float(swingProgress);
        float sineSquared = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);

        AnimationMode mode = AnimationConfig.getMode();
        if (mode == AnimationMode.EXHIBITION) {
            GL11.glTranslated(0.0D, -0.1D, 0.0D);
            this.transformFirstPersonItem(equip / 2.0F, 0.0F);
            GL11.glTranslatef(0.1F, 0.4F, -0.1F);
            GL11.glRotated(-sine * 30.0F, sine / 2.0F, 0.0D, 9.0D);
            GL11.glRotated(-sine * 50.0F, 0.8D, sine / 2.0F, 0.0D);
        } else if (mode == AnimationMode.SIGMA) {
            this.transformFirstPersonItem(equip * 0.5F, 0.0F);
            GL11.glRotated(-sine * 27.5F, -8.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 45.0F, 1.0D, sine / 2.0F, 0.0D);
            GL11.glTranslated(-0.1D, 0.3D, 0.1D);
        } else if (mode == AnimationMode.VANILLA) {
            GL11.glTranslated(0.0D, 0.05D, -0.1D);
            this.transformFirstPersonItem(equip, swingProgress);
        } else if (mode == AnimationMode.PLAIN) {
            GL11.glTranslated(0.0D, 0.05D, 0.0D);
            this.transformFirstPersonItem(equip, 0.0F);
        } else if (mode == AnimationMode.SPIN) {
            GL11.glRotated(this.spin, 0.0D, 0.0D, -0.1D);
            this.transformFirstPersonItem(equip, 0.0F);
            this.spin = -(System.currentTimeMillis() / 2L % 360L);
        } else if (mode == AnimationMode.ETB) {
            GL11.glTranslated(0.0D, -0.1D, 0.0D);
            this.transformFirstPersonItem(equip, 0.0F);
            GL11.glTranslatef(0.1F, 0.4F, -0.1F);
            GL11.glRotated(-sine * 35.0F, -8.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 70.0F, 1.5D, -0.4D, 0.0D);
        } else if (mode == AnimationMode.DORTWARE) {
            float alternate = MathHelper.sin(sqrtSwing * (float) Math.PI - 3.0F);
            this.transformFirstPersonItem(equip, 0.0F);
            GL11.glRotated(-sine * 10.0F, 0.0D, 15.0D, 200.0D);
            GL11.glRotated(-sine * 10.0F, 300.0D, sine / 2.0F, 1.0D);
            GL11.glTranslated(3.4D, 0.3D, -0.4D);
            GL11.glTranslatef(-2.1F, -0.2F, 0.1F);
            GL11.glRotated(alternate * 13.0F, -10.0D, -1.4D, -10.0D);
        } else if (mode == AnimationMode.AVATAR) {
            GL11.glTranslatef(0.56F, -0.52F, -0.72F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(sineSquared * -20.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(sine * -20.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(sine * -40.0F, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(0.4F, 0.4F, 0.4F);
        } else if (mode == AnimationMode.SWONG) {
            this.transformFirstPersonItem(equip / 2.0F, 0.0F);
            GL11.glRotated(-sine * 20.0F, sine / 2.0F, 0.0D, 9.0D);
            GL11.glRotated(-sine * 30.0F, 1.0D, sine / 2.0F, 0.0D);
        } else if (mode == AnimationMode.SWANG) {
            this.transformFirstPersonItem(equip / 2.0F, swingProgress);
            GL11.glRotated(sine * 15.0F, -sine, 0.0D, 9.0D);
            GL11.glRotated(sine * 40.0F, 1.0D, -sine / 2.0F, 0.0D);
        } else if (mode == AnimationMode.SWANK) {
            this.transformFirstPersonItem(equip / 2.0F, swingProgress);
            GL11.glRotated(sine * 30.0F, -sine, 0.0D, 9.0D);
            GL11.glRotated(sine * 40.0F, 1.0D, -sine, 0.0D);
        } else if (mode == AnimationMode.STYLES) {
            this.transformFirstPersonItem(equip, 0.0F);
            GL11.glTranslatef(-0.05F, 0.2F, 0.0F);
            GL11.glRotated(-sine * 35.0F, -8.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 70.0F, 1.0D, -0.4D, 0.0D);
        } else if (mode == AnimationMode.NUDGE) {
            GL11.glTranslated(-0.1D, 0.09D, 0.0D);
            GL11.glRotated(0.0D, -320.0D, 320.0D, 0.0D);
            this.transformFirstPersonItem(0.0F, 1.0F);
            float nudgeSine = MathHelper.sin(sqrtSwing * 3.0F);
            float nudgeAlt = MathHelper.sin(sqrtSwing * 4.9415927F);
            GL11.glRotated(-nudgeSine * 60.0F, -90.0D, -nudgeAlt, 10.0D);
            GL11.glRotated(-nudgeSine * 110.0F, 15.0D, nudgeAlt, 0.0D);
        } else if (mode == AnimationMode.PUNCH) {
            this.transformFirstPersonItem(equip, 0.0F);
            GL11.glTranslatef(0.1F, 0.2F, 0.3F);
            GL11.glRotated(-sine * 30.0F, -5.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 10.0F, 1.0D, -0.4D, -0.5D);
        } else if (mode == AnimationMode.SLIDE) {
            GL11.glTranslated(-0.1D, 0.15D, 0.0D);
            this.transformFirstPersonItem(0.0F, 0.0F);
            float slideSine = MathHelper.sin(sqrtSwing * 2.9415927F);
            GL11.glTranslatef(-0.05F, 0.0F, 0.35F);
            GL11.glRotated(-slideSine * 30.0F, -15.0D, slideSine, 10.0D);
            GL11.glRotated(-slideSine * 70.0D, 5.0D, -slideSine, 0.0D);
        } else if (mode == AnimationMode.JIGSAW) {
            GL11.glTranslatef(0.56F, -0.42F, -0.72F);
            GL11.glTranslatef(0.1F * sine, 0.0F, -0.22F * sine);
            GL11.glTranslatef(0.0F, sineSquared * -0.15F, 0.0F);
            GL11.glRotated(sineSquared * 45.0F, 0.0D, 1.0D, 0.0D);
            GL11.glRotated(sineSquared * -20.0F, 0.0D, 1.0D, 0.0D);
            GL11.glRotated(sine * -20.0F, 0.0D, 0.0D, 1.0D);
            GL11.glRotated(sine * -80.0F, 1.0D, 0.0D, 0.0D);
        }
    }

    @Inject(
            method = "renderItemInFirstPerson",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void applyScale(float partialTicks, CallbackInfo callbackInfo) {
        if (!AnimationConfig.isEnabled()) {
            return;
        }

        AnimationConfig.sync();
        double scale = AnimationConfig.getScale() / 100.0D;
        GL11.glScaled(scale, scale, scale);
    }
}
