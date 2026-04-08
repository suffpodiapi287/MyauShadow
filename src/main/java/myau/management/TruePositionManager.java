package myau.management;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.Vec3;

public class TruePositionManager {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private long lastInterpolationUpdateNanos = 0L;

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE || mc.theWorld == null) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (packet instanceof S14PacketEntity) {
            this.handleRelativeMove((S14PacketEntity) packet);
        } else if (packet instanceof S18PacketEntityTeleport) {
            this.handleTeleport((S18PacketEntityTeleport) packet);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.POST || mc.theWorld == null) {
            return;
        }

        for (Object loadedEntity : mc.theWorld.loadedEntityList) {
            if (!(loadedEntity instanceof EntityLivingBase)) {
                continue;
            }

            EntityLivingBase entity = (EntityLivingBase) loadedEntity;
            if (!(entity instanceof ITruePositionEntity)) {
                continue;
            }

            ITruePositionEntity trueEntity = (ITruePositionEntity) entity;
            if (!trueEntity.hasTruePosition()) {
                snapToCurrent(entity, false);
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.theWorld == null) {
            this.lastInterpolationUpdateNanos = 0L;
            return;
        }

        double delta = this.getNormalizedDelta();
        for (Object loadedEntity : mc.theWorld.loadedEntityList) {
            if (!(loadedEntity instanceof EntityLivingBase)) {
                continue;
            }

            EntityLivingBase entity = (EntityLivingBase) loadedEntity;
            if (!(entity instanceof ITruePositionEntity)) {
                continue;
            }

            ITruePositionEntity trueEntity = (ITruePositionEntity) entity;
            if (!trueEntity.hasTruePosition()) {
                snapToCurrent(entity, false);
                continue;
            }

            trueEntity.setLerpX(approach(trueEntity.getLerpX(), trueEntity.getTrueX(), delta));
            trueEntity.setLerpY(approach(trueEntity.getLerpY(), trueEntity.getTrueY(), delta));
            trueEntity.setLerpZ(approach(trueEntity.getLerpZ(), trueEntity.getTrueZ(), delta));
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        this.lastInterpolationUpdateNanos = 0L;
    }

    public static boolean hasTruePosition(Entity entity) {
        return entity instanceof ITruePositionEntity && ((ITruePositionEntity) entity).hasTruePosition();
    }

    public static Vec3 getTruePosition(Entity entity) {
        if (!(entity instanceof ITruePositionEntity)) {
            return null;
        }

        ITruePositionEntity trueEntity = (ITruePositionEntity) entity;
        if (!trueEntity.hasTruePosition()) {
            return null;
        }

        return new Vec3(trueEntity.getTrueX(), trueEntity.getTrueY(), trueEntity.getTrueZ());
    }

    public static Vec3 getInterpolatedPosition(Entity entity) {
        if (!(entity instanceof ITruePositionEntity)) {
            return null;
        }

        ITruePositionEntity trueEntity = (ITruePositionEntity) entity;
        if (!trueEntity.hasTruePosition()) {
            return null;
        }

        return new Vec3(trueEntity.getLerpX(), trueEntity.getLerpY(), trueEntity.getLerpZ());
    }

    public static void snapToCurrent(Entity entity, boolean ignoreInterpolation) {
        if (!(entity instanceof ITruePositionEntity)) {
            return;
        }

        ITruePositionEntity trueEntity = (ITruePositionEntity) entity;
        trueEntity.setTrueX(entity.posX);
        trueEntity.setTrueY(entity.posY);
        trueEntity.setTrueZ(entity.posZ);
        if (!ignoreInterpolation) {
            trueEntity.setLerpX(entity.posX);
            trueEntity.setLerpY(entity.posY);
            trueEntity.setLerpZ(entity.posZ);
        }
        trueEntity.setTruePosition(true);
    }

    private void handleRelativeMove(S14PacketEntity packet) {
        Entity entity = packet.getEntity(mc.theWorld);
        if (!(entity instanceof EntityLivingBase) || !(entity instanceof ITruePositionEntity)) {
            return;
        }

        ITruePositionEntity trueEntity = (ITruePositionEntity) entity;
        if (!trueEntity.hasTruePosition()) {
            snapToCurrent(entity, false);
        }

        trueEntity.setTrueX(trueEntity.getTrueX() + packet.func_149062_c() / 32.0);
        trueEntity.setTrueY(trueEntity.getTrueY() + packet.func_149061_d() / 32.0);
        trueEntity.setTrueZ(trueEntity.getTrueZ() + packet.func_149064_e() / 32.0);
        trueEntity.setTruePosition(true);
    }

    private void handleTeleport(S18PacketEntityTeleport packet) {
        Entity entity = mc.theWorld.getEntityByID(packet.getEntityId());
        if (!(entity instanceof EntityLivingBase) || !(entity instanceof ITruePositionEntity)) {
            return;
        }

        ITruePositionEntity trueEntity = (ITruePositionEntity) entity;
        trueEntity.setTrueX(packet.getX() / 32.0);
        trueEntity.setTrueY(packet.getY() / 32.0);
        trueEntity.setTrueZ(packet.getZ() / 32.0);
        trueEntity.setTruePosition(true);
    }

    private double getNormalizedDelta() {
        long now = System.nanoTime();
        if (this.lastInterpolationUpdateNanos == 0L) {
            this.lastInterpolationUpdateNanos = now;
            return 1.0;
        }

        double deltaTime = (now - this.lastInterpolationUpdateNanos) / 1_000_000.0;
        this.lastInterpolationUpdateNanos = now;
        return Math.min(1.0, deltaTime / 50.0 * 3.0);
    }

    private static double approach(double current, double target, double delta) {
        return current + (target - current) * delta;
    }
}
