package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.management.ITruePositionEntity;
import myau.management.RotationState;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import myau.util.PacketUtil;
import myau.util.RotationUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class FakeLag extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final Deque<QueuedPacket> packetQueue = new ConcurrentLinkedDeque<>();
    private final Deque<PositionData> positions = new ConcurrentLinkedDeque<>();
    private final TimerUtil recoilTimer = new TimerUtil();
    private final ModelRenderData renderData = new ModelRenderData();
    private boolean wasNearEnemy = false;
    private boolean ignoreWholeTick = false;
    public final IntProperty delay = new IntProperty("delay", 550, 0, 1000);
    public final IntProperty recoilTime = new IntProperty("recoil-time", 750, 0, 2000);
    public final FloatProperty minAllowedDistToEnemy = new FloatProperty("min-allowed-dist-to-enemy", 1.5F, 0.0F, 6.0F);
    public final FloatProperty maxAllowedDistToEnemy = new FloatProperty("max-allowed-dist-to-enemy", 3.5F, 0.0F, 6.0F);
    public final BooleanProperty blinkOnAction = new BooleanProperty("blink-on-action", true);
    public final BooleanProperty pauseOnNoMove = new BooleanProperty("pause-on-no-move", true);
    public final BooleanProperty pauseOnChest = new BooleanProperty("pause-on-chest", false);
    public final BooleanProperty line = new BooleanProperty("line", true);
    public final ColorProperty lineColor = new ColorProperty("line-color", Color.GREEN.getRGB(), this.line::getValue);
    public final BooleanProperty renderModel = new BooleanProperty("render-model", false);
    public final PercentProperty modelOpacity = new PercentProperty("model-opacity", 55, this.renderModel::getValue);

    public FakeLag() {
        super("FakeLag", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null || event.isCancelled() || mc.thePlayer.isDead) {
            return;
        }

        Packet<?> packet = event.getPacket();

        // Keep action-blink responsive even when ignoreWholeTick is active.
        if (this.blinkOnAction.getValue() && packet instanceof C02PacketUseEntity) {
            this.flush(true);
            return;
        }

        if (ignoreWholeTick) return;

        if (this.maxAllowedDistToEnemy.getValue() > 0.0F && this.wasNearEnemy) {
            return;
        }

        if (this.pauseOnNoMove.getValue() && !this.isPlayerMoving()) {
            this.flush(true);
            return;
        }

        if (mc.thePlayer.getHealth() < mc.thePlayer.getMaxHealth()) {
            if (mc.thePlayer.hurtTime != 0) {
                this.flush(true);
                return;
            }
        }

        if (this.isScaffoldActive()) {
            this.flush(true);
            return;
        }

        if (this.pauseOnChest.getValue() && mc.currentScreen instanceof GuiContainer) {
            this.flush(true);
            return;
        }

        if (this.isIgnoredPacket(packet)) {
            return;
        }

        if (packet instanceof C0EPacketClickWindow
                || packet instanceof C0DPacketCloseWindow
                || packet instanceof C07PacketPlayerDigging
                || packet instanceof C08PacketPlayerBlockPlacement
                || packet instanceof C12PacketUpdateSign
                || packet instanceof C19PacketResourcePackStatus) {
            this.flush(true);
            return;
        }

        if (event.getType() == EventType.RECEIVE) {
            if (packet instanceof S08PacketPlayerPosLook) {
                this.flush(true);
                return;
            }

            if (packet instanceof S12PacketEntityVelocity && ((S12PacketEntityVelocity) packet).getEntityID() == mc.thePlayer.getEntityId()) {
                this.flush(true);
                return;
            }

            if (packet instanceof S27PacketExplosion) {
                S27PacketExplosion explosion = (S27PacketExplosion) packet;
                if (explosion.func_149149_c() != 0.0F || explosion.func_149144_d() != 0.0F || explosion.func_149147_e() != 0.0F) {
                    this.flush(true);
                }
            }
            return;
        }

        if (!this.recoilTimer.hasTimeElapsed(this.recoilTime.getValue())) {
            return;
        }

        if (mc.isSingleplayer() || mc.getCurrentServerData() == null) {
            this.flush(true);
            return;
        }

        if (event.getType() != EventType.SEND) {
            return;
        }

        event.setCancelled(true);
        this.packetQueue.addLast(new QueuedPacket(packet, System.currentTimeMillis()));

        if (packet instanceof C03PacketPlayer) {
            C03PacketPlayer playerPacket = (C03PacketPlayer) packet;
            if (playerPacket.isMoving()) {
                this.positions.addLast(
                        new PositionData(
                                new Vec3(playerPacket.getPositionX(), playerPacket.getPositionY(), playerPacket.getPositionZ()),
                                System.currentTimeMillis(),
                                mc.thePlayer.renderYawOffset,
                                playerPacket.getRotating() ? playerPacket.getYaw() : mc.thePlayer.rotationYaw,
                                playerPacket.getRotating() ? playerPacket.getPitch() : mc.thePlayer.rotationPitch,
                                mc.thePlayer.rotationYawHead,
                                mc.thePlayer.isSneaking()
                        )
                );
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (this.maxAllowedDistToEnemy.getValue() > 0.0F) {
            this.wasNearEnemy = false;
            if (this.checkNearEnemy()) {
                this.flush(true);
                this.wasNearEnemy = true;
                return;
            }
        }

        if (this.isBlinking() || mc.thePlayer.isDead || mc.thePlayer.isUsingItem()) {
            this.flush(true);
            return;
        }

        if (!this.recoilTimer.hasTimeElapsed(this.recoilTime.getValue())) {
            return;
        }

        long cutoff = System.currentTimeMillis() - this.delay.getValue();

        while (!this.packetQueue.isEmpty()) {
            QueuedPacket queuedPacket = this.packetQueue.peekFirst();
            if (queuedPacket == null || queuedPacket.time > cutoff) {
                break;
            }

            this.packetQueue.removeFirst();
            PacketUtil.sendPacketNoEvent(queuedPacket.packet);
        }

        while (!this.positions.isEmpty()) {
            PositionData positionData = this.positions.peekFirst();
            if (positionData == null || positionData.time > cutoff) {
                break;
            }
            this.positions.removeFirst();
        }
        ignoreWholeTick = false;
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (this.isBlinking() || this.positions.isEmpty()) {
            this.renderData.reset(mc.thePlayer);
            return;
        }

        PositionData renderPosition = this.positions.peekFirst();
        if (renderPosition == null) {
            return;
        }

        this.renderData.update(renderPosition);

        if (this.line.getValue()) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            mc.entityRenderer.disableLightmap();
            GL11.glBegin(GL11.GL_LINE_STRIP);
            Color color = new Color(this.lineColor.getValue());
            GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 1.0F);

            double renderPosX = ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
            double renderPosY = ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
            double renderPosZ = ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();

            for (PositionData positionData : this.positions) {
                GL11.glVertex3d(positionData.position.xCoord - renderPosX, positionData.position.yCoord - renderPosY, positionData.position.zCoord - renderPosZ);
            }

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnd();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }

        if (this.renderModel.getValue() && mc.gameSettings.thirdPersonView != 0) {
            this.renderRealPlayerGhost(event);
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        this.flush(false);
    }

    private boolean checkNearEnemy() {
        float minDistance = Math.min(this.minAllowedDistToEnemy.getValue(), this.maxAllowedDistToEnemy.getValue());
        float maxDistance = Math.max(this.minAllowedDistToEnemy.getValue(), this.maxAllowedDistToEnemy.getValue());
        if (maxDistance <= 0.0F || mc.thePlayer == null || mc.theWorld == null) {
            return false;
        }

        Vec3 playerPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        Vec3 serverPos = this.positions.peekFirst() != null ? this.positions.peekFirst().position : playerPos;
        AxisAlignedBB playerBox = mc.thePlayer.getEntityBoundingBox().offset(
                serverPos.xCoord - playerPos.xCoord,
                serverPos.yCoord - playerPos.yCoord,
                serverPos.zCoord - playerPos.zCoord
        );

        for (EntityPlayer otherPlayer : mc.theWorld.playerEntities) {
            if (otherPlayer == mc.thePlayer) {
                continue;
            }

            Vec3 trueEyes = this.getTruePositionEyes(otherPlayer);
            if (trueEyes == null) {
                continue;
            }

            double distance = RotationUtil.clampVecToBox(playerBox, trueEyes);
            if (distance >= minDistance && distance <= maxDistance) {
                return true;
            }
        }

        return false;
    }

    private boolean isIgnoredPacket(Packet<?> packet) {
        return packet instanceof C00Handshake
                || packet instanceof C00PacketServerQuery
                || packet instanceof C01PacketPing
                || packet instanceof C01PacketChatMessage
                || packet instanceof S01PacketPong;
    }

    private boolean isPlayerMoving() {
        return mc.thePlayer != null && (mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F);
    }

    private boolean isBlinking() {
        return Myau.blinkManager != null && Myau.blinkManager.isSendBlinking();
    }

    private boolean isScaffoldActive() {
        Scaffold scaffold = Myau.moduleManager == null ? null : (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        return scaffold != null && scaffold.isEnabled() && RotationState.isActived() && RotationState.getPriority() == 3.0F;
    }

    private Vec3 getTruePositionEyes(EntityPlayer player) {
        if (player instanceof ITruePositionEntity) {
            ITruePositionEntity trueEntity = (ITruePositionEntity) player;
            if (trueEntity.hasTruePosition()) {
                return new Vec3(trueEntity.getTrueX(), trueEntity.getTrueY() + player.getEyeHeight(), trueEntity.getTrueZ());
            }
        }
        return null;
    }

    private void flush(boolean setRecoil) {
        mc.addScheduledTask(() -> {
            if (setRecoil) recoilTimer.reset();

            QueuedPacket q;
            while ((q = packetQueue.pollFirst()) != null) {
                PacketUtil.sendPacketNoEvent(q.packet);
            }

            positions.clear();
            ignoreWholeTick = true;
        });
    }

    private interface RotationRenderCallback<T> {
        T call();
    }

    private <T> T runWithModifiedRotation(
            EntityPlayer entity,
            float prevYaw, float yaw,
            float prevPitch, float pitch,
            float prevBodyYaw, float bodyYaw,
            RotationRenderCallback<T> callback
    ) {
        float oldPrevYaw = entity.prevRotationYaw;
        float oldYaw = entity.rotationYaw;
        float oldPrevPitch = entity.prevRotationPitch;
        float oldPitch = entity.rotationPitch;

        float oldPrevHeadYaw = entity.prevRotationYawHead;
        float oldHeadYaw = entity.rotationYawHead;

        float oldPrevBodyYaw = entity.prevRenderYawOffset;
        float oldBodyYaw = entity.renderYawOffset;

        try {
            entity.prevRotationYaw = prevYaw;
            entity.rotationYaw = yaw;
            entity.prevRotationPitch = prevPitch;
            entity.rotationPitch = pitch;

            entity.prevRotationYawHead = yaw;
            entity.rotationYawHead = yaw;

            entity.prevRenderYawOffset = prevBodyYaw;
            entity.renderYawOffset = bodyYaw;

            return callback.call();
        } finally {
            entity.prevRotationYaw = oldPrevYaw;
            entity.rotationYaw = oldYaw;
            entity.prevRotationPitch = oldPrevPitch;
            entity.rotationPitch = oldPitch;

            entity.prevRotationYawHead = oldPrevHeadYaw;
            entity.rotationYawHead = oldHeadYaw;

            entity.prevRenderYawOffset = oldPrevBodyYaw;
            entity.renderYawOffset = oldBodyYaw;
        }
    }

    private void renderRealPlayerGhost(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || this.positions.isEmpty()) {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;
        PositionData first = this.positions.peekFirst();
        if (first == null) {
            return;
        }

        PositionData second = this.positions.size() > 1
                ? (PositionData) this.positions.toArray()[1]
                : first;

        double renderPosX = ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
        double renderPosY = ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
        double renderPosZ = ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
        float alpha = this.modelOpacity.getValue() / 100.0F;

        double oldPosX = player.posX;
        double oldPosY = player.posY;
        double oldPosZ = player.posZ;
        double oldPrevPosX = player.prevPosX;
        double oldPrevPosY = player.prevPosY;
        double oldPrevPosZ = player.prevPosZ;
        double oldLastTickPosX = player.lastTickPosX;
        double oldLastTickPosY = player.lastTickPosY;
        double oldLastTickPosZ = player.lastTickPosZ;
        boolean oldSneaking = player.isSneaking();

        try {
            player.posX = this.renderData.x;
            player.posY = this.renderData.y;
            player.posZ = this.renderData.z;
            player.prevPosX = this.renderData.x;
            player.prevPosY = this.renderData.y;
            player.prevPosZ = this.renderData.z;
            player.lastTickPosX = this.renderData.x;
            player.lastTickPosY = this.renderData.y;
            player.lastTickPosZ = this.renderData.z;
            player.setSneaking(this.renderData.sneaking);

            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);

            runWithModifiedRotation(
                    player,
                    first.yaw,
                    this.renderData.yaw,
                    first.pitch,
                    this.renderData.pitch,
                    first.bodyYaw,
                    second.bodyYaw,
                    () -> {
                        mc.getRenderManager().doRenderEntity(
                                player,
                                this.renderData.x - renderPosX,
                                this.renderData.y - renderPosY,
                                this.renderData.z - renderPosZ,
                                this.renderData.yaw,
                                event.getPartialTicks(),
                                true
                        );
                        return null;
                    }
            );
        } finally {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();

            player.posX = oldPosX;
            player.posY = oldPosY;
            player.posZ = oldPosZ;
            player.prevPosX = oldPrevPosX;
            player.prevPosY = oldPrevPosY;
            player.prevPosZ = oldPrevPosZ;
            player.lastTickPosX = oldLastTickPosX;
            player.lastTickPosY = oldLastTickPosY;
            player.lastTickPosZ = oldLastTickPosZ;
            player.setSneaking(oldSneaking);
        }
    }

    @Override
    public void onEnabled() {
        this.packetQueue.clear();
        this.positions.clear();
        this.wasNearEnemy = false;
        this.recoilTimer.setTime();
        this.renderData.reset(mc.thePlayer);
    }

    @Override
    public void onDisabled() {
        if (mc.thePlayer != null) {
            this.flush(true);
        }
        this.wasNearEnemy = false;
        this.renderData.reset(mc.thePlayer);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.valueOf(this.packetQueue.size())};
    }

    private static class QueuedPacket {
        private final Packet<?> packet;
        private final long time;

        private QueuedPacket(Packet<?> packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }

    private static class PositionData {
        private final Vec3 position;
        private final long time;
        private final float bodyYaw;
        private final float yaw;
        private final float pitch;
        private final float headYaw;
        private final boolean sneaking;

        private PositionData(Vec3 position, long time, float bodyYaw, float yaw, float pitch, float headYaw, boolean sneaking) {
            this.position = position;
            this.time = time;
            this.bodyYaw = bodyYaw;
            this.yaw = yaw;
            this.pitch = pitch;
            this.headYaw = headYaw;
            this.sneaking = sneaking;
        }
    }

    private static class ModelRenderData {
        private double x;
        private double y;
        private double z;
        private float bodyYaw;
        private float yaw;
        private float pitch;
        private float headYaw;
        private boolean sneaking;

        private void reset(EntityPlayerSP player) {
            if (player == null) {
                this.x = 0.0;
                this.y = 0.0;
                this.z = 0.0;
                this.bodyYaw = 0.0F;
                this.yaw = 0.0F;
                this.pitch = 0.0F;
                this.headYaw = 0.0F;
                this.sneaking = false;
                return;
            }

            this.x = player.posX;
            this.y = player.posY;
            this.z = player.posZ;
            this.bodyYaw = player.renderYawOffset;
            this.yaw = player.rotationYaw;
            this.pitch = player.rotationPitch;
            this.headYaw = player.rotationYawHead;
            this.sneaking = player.isSneaking();
        }

        private void update(PositionData target) {
            this.x = lerp(this.x, target.position.xCoord, 0.35);
            this.y = lerp(this.y, target.position.yCoord, 0.35);
            this.z = lerp(this.z, target.position.zCoord, 0.35);
            this.bodyYaw = lerpAngle(this.bodyYaw, target.bodyYaw, 0.35F);
            this.yaw = lerpAngle(this.yaw, target.yaw, 0.35F);
            this.pitch = lerpAngle(this.pitch, target.pitch, 0.35F);
            this.headYaw = lerpAngle(this.headYaw, target.headYaw, 0.35F);
            this.sneaking = target.sneaking;
        }

        private static double lerp(double current, double target, double factor) {
            return current + (target - current) * factor;
        }

        private static float lerpAngle(float current, float target, float factor) {
            return current + net.minecraft.util.MathHelper.wrapAngleTo180_float(target - current) * factor;
        }
    }
}
