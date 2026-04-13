package myau.module.modules;

import myau.management.TruePositionManager;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class ForwardTrack extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty espMode = new ModeProperty("esp-mode", 1, new String[]{"BOX", "MODEL", "WIREFRAME"});
    public final FloatProperty wireframeWidth = new FloatProperty("wireframe-width", 1.0F, 0.5F, 5.0F, () -> this.espMode.getValue() == 2);
    public final ColorProperty espColor = new ColorProperty("esp-color", Color.GREEN.getRGB(), () -> this.espMode.getValue() != 1);

    public ForwardTrack() {
        super("ForwardTrack", false);
    }

    public void includeEntityTruePos(Entity entity, Runnable action) {
        if (!this.isEnabled() || action == null || !this.shouldTrack(entity)) {
            return;
        }

        Vec3 trackedPosition = this.usePosition(entity);
        if (trackedPosition == null) {
            return;
        }

        this.runWithSimulatedPosition(entity, trackedPosition, action);
    }

    public Vec3 getCombatPosition(Entity entity) {
        if (!this.isEnabled() || entity == null || !this.shouldTrack(entity)) {
            return null;
        }

        return this.usePosition(entity);
    }

    public AxisAlignedBB getCombatBoundingBox(Entity entity) {
        if (!(entity instanceof EntityLivingBase) || !this.isEnabled() || !this.shouldTrack(entity)) {
            return null;
        }

        Vec3 trackedPosition = this.usePosition(entity);
        return trackedPosition == null ? null : this.buildBoundingBox(entity, trackedPosition, true);
    }

    public AxisAlignedBB getMouseOverBoundingBox(Entity entity) {
        if (!this.isEnabled() || entity == null || !this.shouldTrack(entity)) {
            return null;
        }

        Vec3 trackedPosition = this.usePosition(entity);
        return trackedPosition == null ? null : this.buildBoundingBox(entity, trackedPosition, false);
    }

    @myau.event.EventTarget
    public void onRender(myau.events.Render3DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        for (Object loadedEntity : mc.theWorld.loadedEntityList) {
            if (!(loadedEntity instanceof EntityLivingBase)) {
                continue;
            }

            EntityLivingBase entity = (EntityLivingBase) loadedEntity;
            if (!this.shouldTrack(entity)) {
                continue;
            }

            Vec3 trackedPosition = this.usePosition(entity);
            if (trackedPosition == null) {
                continue;
            }

            double renderPosX = ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
            double renderPosY = ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
            double renderPosZ = ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();

            double x = trackedPosition.xCoord - renderPosX;
            double y = trackedPosition.yCoord - renderPosY;
            double z = trackedPosition.zCoord - renderPosZ;

            switch (this.espMode.getValue()) {
                case 0: {
                    AxisAlignedBB axisAlignedBB = this.buildBoundingBox(entity, trackedPosition, false).offset(-renderPosX, -renderPosY, -renderPosZ);
                    Color color = this.getRenderColor();
                    RenderUtil.enableRenderState();
                    RenderUtil.drawFilledBox(axisAlignedBB, color.getRed(), color.getGreen(), color.getBlue());
                    RenderUtil.drawBoundingBox(axisAlignedBB, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 1.25F);
                    RenderUtil.disableRenderState();
                    break;
                }
                case 1:
                    GL11.glPushMatrix();
                    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

                    GL11.glColor4f(0.6F, 0.6F, 0.6F, 1.0F);
                    mc.getRenderManager().doRenderEntity(
                            entity,
                            x,
                            y,
                            z,
                            entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * event.getPartialTicks(),
                            event.getPartialTicks(),
                            true
                    );

                    GL11.glPopAttrib();
                    GL11.glPopMatrix();
                    break;
                case 2:
                    GL11.glPushMatrix();
                    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

                    GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glDisable(GL11.GL_LIGHTING);
                    GL11.glDisable(GL11.GL_DEPTH_TEST);
                    GL11.glEnable(GL11.GL_LINE_SMOOTH);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    GL11.glLineWidth(this.wireframeWidth.getValue());

                    Color wireColor = this.getRenderColor();
                    GL11.glColor4f(wireColor.getRed() / 255.0F, wireColor.getGreen() / 255.0F, wireColor.getBlue() / 255.0F, wireColor.getAlpha() / 255.0F);
                    mc.getRenderManager().doRenderEntity(
                            entity,
                            x,
                            y,
                            z,
                            entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * event.getPartialTicks(),
                            event.getPartialTicks(),
                            true
                    );
                    GL11.glColor4f(wireColor.getRed() / 255.0F, wireColor.getGreen() / 255.0F, wireColor.getBlue() / 255.0F, wireColor.getAlpha() / 255.0F);
                    mc.getRenderManager().doRenderEntity(
                            entity,
                            x,
                            y,
                            z,
                            entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * event.getPartialTicks(),
                            event.getPartialTicks(),
                            true
                    );

                    GL11.glPopAttrib();
                    GL11.glPopMatrix();
                    break;
                default:
                    break;
            }
        }
    }

    private boolean shouldTrack(Entity entity) {
        if (!(entity instanceof EntityLivingBase)) {
            return false;
        }

        EntityLivingBase livingBase = (EntityLivingBase) entity;
        if (livingBase == mc.thePlayer || livingBase.isDead || livingBase.deathTime > 0) {
            return false;
        }

        if (livingBase instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) livingBase;
            return !TeamUtil.isFriend(player) && !TeamUtil.isBot(player) && !TeamUtil.isSameTeam(player);
        }

        return true;
    }

    private Vec3 usePosition(Entity entity) {
        if (entity == null) {
            return null;
        }

        if (!mc.isSingleplayer()) {
            Vec3 interpolatedPosition = TruePositionManager.getInterpolatedPosition(entity);
            if (interpolatedPosition != null) {
                return interpolatedPosition;
            }

            Vec3 truePosition = TruePositionManager.getTruePosition(entity);
            if (truePosition != null) {
                return truePosition;
            }
        }

        return entity.getPositionVector();
    }

    private AxisAlignedBB buildBoundingBox(Entity entity, Vec3 position, boolean expandBorder) {
        AxisAlignedBB boundingBox = entity.getEntityBoundingBox().offset(position.xCoord - entity.posX, position.yCoord - entity.posY, position.zCoord - entity.posZ);
        if (!expandBorder) {
            return boundingBox;
        }

        float borderSize = entity.getCollisionBorderSize();
        return boundingBox.expand(borderSize, borderSize, borderSize);
    }

    private void runWithSimulatedPosition(Entity entity, Vec3 position, Runnable action) {
        double oldPosX = entity.posX;
        double oldPosY = entity.posY;
        double oldPosZ = entity.posZ;
        double oldPrevPosX = entity.prevPosX;
        double oldPrevPosY = entity.prevPosY;
        double oldPrevPosZ = entity.prevPosZ;
        double oldLastTickPosX = entity.lastTickPosX;
        double oldLastTickPosY = entity.lastTickPosY;
        double oldLastTickPosZ = entity.lastTickPosZ;

        try {
            entity.setPosition(position.xCoord, position.yCoord, position.zCoord);
            entity.prevPosX = position.xCoord;
            entity.prevPosY = position.yCoord;
            entity.prevPosZ = position.zCoord;
            entity.lastTickPosX = position.xCoord;
            entity.lastTickPosY = position.yCoord;
            entity.lastTickPosZ = position.zCoord;
            action.run();
        } finally {
            entity.setPosition(oldPosX, oldPosY, oldPosZ);
            entity.prevPosX = oldPrevPosX;
            entity.prevPosY = oldPrevPosY;
            entity.prevPosZ = oldPrevPosZ;
            entity.lastTickPosX = oldLastTickPosX;
            entity.lastTickPosY = oldLastTickPosY;
            entity.lastTickPosZ = oldLastTickPosZ;
        }
    }

    private Color getRenderColor() {
        int color = this.espMode.getValue() == 1 ? 0x999999 : this.espColor.getValue();
        return new Color(MathHelper.clamp_int(color, 0, 0xFFFFFF));
    }
}
