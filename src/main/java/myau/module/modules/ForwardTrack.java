package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.LoadWorldEvent;
import myau.events.Render3DEvent;
import myau.management.TruePositionManager;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.awt.*;

public class ForwardTrack extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final BooleanProperty playersOnly = new BooleanProperty("players-only", true);
    public final ModeProperty showPosition = new ModeProperty("show-position", 1, new String[]{"NONE", "DEFAULT", "HUD"});

    public ForwardTrack() {
        super("ForwardTrack", false);
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (!this.isEnabled() || this.showPosition.getValue() == 0 || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        Color color = this.getRenderColor();

        for (Object loadedEntity : mc.theWorld.loadedEntityList) {
            if (!(loadedEntity instanceof EntityLivingBase)) {
                continue;
            }

            EntityLivingBase entity = (EntityLivingBase) loadedEntity;
            if (entity == mc.thePlayer || entity.isDead || entity.deathTime > 0) {
                continue;
            }

            if (this.playersOnly.getValue() && !(entity instanceof EntityPlayer)) {
                continue;
            }

            AxisAlignedBB trueBox = this.getRenderTruePositionBox(entity);
            if (trueBox == null) {
                continue;
            }

            AxisAlignedBB renderBox = trueBox.offset(
                    -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                    -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                    -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()
            );

            RenderUtil.enableRenderState();
            RenderUtil.drawFilledBox(renderBox, color.getRed(), color.getGreen(), color.getBlue());
            RenderUtil.drawBoundingBox(renderBox, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 1.25F);
            RenderUtil.disableRenderState();
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
    }

    public Vec3 getTruePosition(Entity entity) {
        if (!this.isEnabled() || entity == null || mc.isSingleplayer()) {
            return null;
        }

        Vec3 truePosition = TruePositionManager.getTruePosition(entity);
        if (truePosition == null) {
            return null;
        }

        if (Math.abs(truePosition.xCoord - entity.posX) < 1.0E-3
                && Math.abs(truePosition.yCoord - entity.posY) < 1.0E-3
                && Math.abs(truePosition.zCoord - entity.posZ) < 1.0E-3) {
            return null;
        }

        return truePosition;
    }

    public AxisAlignedBB getTruePositionBox(EntityLivingBase entity) {
        Vec3 truePosition = this.getTruePosition(entity);
        if (truePosition == null) {
            return null;
        }

        float borderSize = entity.getCollisionBorderSize();
        return entity.getEntityBoundingBox()
                .expand(borderSize, borderSize, borderSize)
                .offset(truePosition.xCoord - entity.posX, truePosition.yCoord - entity.posY, truePosition.zCoord - entity.posZ);
    }

    private AxisAlignedBB getRenderTruePositionBox(EntityLivingBase entity) {
        Vec3 renderPosition = TruePositionManager.getInterpolatedPosition(entity);
        if (renderPosition == null) {
            return this.getTruePositionBox(entity);
        }

        float borderSize = entity.getCollisionBorderSize();
        return entity.getEntityBoundingBox()
                .expand(borderSize, borderSize, borderSize)
                .offset(renderPosition.xCoord - entity.posX, renderPosition.yCoord - entity.posY, renderPosition.zCoord - entity.posZ);
    }

    private Color getRenderColor() {
        if (this.showPosition.getValue() == 2) {
            return ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
        }
        return new Color(85, 255, 85, 160);
    }
}
