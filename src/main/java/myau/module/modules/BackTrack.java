package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BackTrack extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final List<Packet<?>> queuedPackets = new ArrayList<>();
    private EntityLivingBase target;
    private boolean hasTrackedPosition;
    private double trackedServerX;
    private double trackedServerY;
    private double trackedServerZ;
    private long lastFlushAt;
    private long nextFlushDelay;
    public final IntProperty minDelay = new IntProperty("min-delay", 100, 0, 1000);
    public final IntProperty maxDelay = new IntProperty("max-delay", 200, 0, 1000);
    public final FloatProperty range = new FloatProperty("range", 3.0F, 0.0F, 10.0F);
    public final BooleanProperty zoneEsp = new BooleanProperty("zone-esp", true);
    public final ModeProperty showPosition = new ModeProperty("show-position", 1, new String[]{"NONE", "DEFAULT", "HUD"});

    public BackTrack() {
        super("BackTrack", false);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!(event.getTarget() instanceof EntityLivingBase) || mc.theWorld == null) {
            return;
        }

        EntityLivingBase attackedEntity = (EntityLivingBase) event.getTarget();
        if (this.target != attackedEntity) {
            this.flushPackets();
        }

        this.target = attackedEntity;
        this.trackedServerX = attackedEntity.serverPosX;
        this.trackedServerY = attackedEntity.serverPosY;
        this.trackedServerZ = attackedEntity.serverPosZ;
        this.hasTrackedPosition = true;
        this.scheduleNextFlush();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE || mc.thePlayer == null || mc.theWorld == null || event.isCancelled()) {
            return;
        }

        EntityLivingBase trackedTarget = this.target;
        if (trackedTarget == null) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (packet instanceof S06PacketUpdateHealth && ((S06PacketUpdateHealth) packet).getHealth() <= 0.0F) {
            this.flushPackets();
            return;
        }

        if (packet instanceof S08PacketPlayerPosLook || packet instanceof S40PacketDisconnect) {
            this.flushPackets();
            return;
        }

        if (packet instanceof S13PacketDestroyEntities) {
            for (int entityId : ((S13PacketDestroyEntities) packet).getEntityIDs()) {
                if (entityId == trackedTarget.getEntityId()) {
                    this.flushPackets();
                    return;
                }
            }
        }

        if (!this.shouldQueuePacket(packet, trackedTarget)) {
            return;
        }

        if (packet instanceof S14PacketEntity) {
            S14PacketEntity movementPacket = (S14PacketEntity) packet;
            this.trackedServerX += movementPacket.func_149062_c();
            this.trackedServerY += movementPacket.func_149061_d();
            this.trackedServerZ += movementPacket.func_149064_e();
            this.hasTrackedPosition = true;
        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport teleportPacket = (S18PacketEntityTeleport) packet;
            this.trackedServerX = teleportPacket.getX();
            this.trackedServerY = teleportPacket.getY();
            this.trackedServerZ = teleportPacket.getZ();
            this.hasTrackedPosition = true;
        }

        synchronized (this.queuedPackets) {
            this.queuedPackets.add(packet);
        }
        event.setCancelled(true);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (this.target == null || !mc.theWorld.loadedEntityList.contains(this.target) || this.target.isDead || this.target.deathTime > 0) {
            this.flushPackets();
            return;
        }

        if (!this.hasTrackedPosition) {
            return;
        }

        Vec3 trackedPosition = this.getTrackedPosition();
        double realDistance = mc.thePlayer.getDistance(trackedPosition.xCoord, trackedPosition.yCoord, trackedPosition.zCoord);
        double renderDistance = mc.thePlayer.getDistance(this.target.posX, this.target.posY, this.target.posZ);

        if (renderDistance >= realDistance || realDistance > this.range.getValue()) {
            this.flushPackets();
            return;
        }

        if (System.currentTimeMillis() - this.lastFlushAt >= this.nextFlushDelay) {
            this.flushPackets();
            if (this.target != null) {
                this.scheduleNextFlush();
            }
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (!this.isEnabled() || !this.zoneEsp.getValue() || this.showPosition.getValue() == 0 || this.target == null || !this.hasTrackedPosition) {
            return;
        }

        AxisAlignedBB trackedBox = this.getNearestTrackedBox(this.target);
        if (trackedBox == null) {
            return;
        }

        Color color = this.getRenderColor();
        AxisAlignedBB renderBox = trackedBox.offset(
                -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()
        );

        RenderUtil.enableRenderState();
        RenderUtil.drawFilledBox(renderBox, color.getRed(), color.getGreen(), color.getBlue());
        RenderUtil.drawBoundingBox(renderBox, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 1.5F);
        RenderUtil.disableRenderState();
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        this.reset(false);
    }

    public AxisAlignedBB getNearestTrackedBox(EntityLivingBase entity) {
        if (!this.isEnabled() || entity == null || entity != this.target || !this.hasTrackedPosition) {
            return null;
        }

        Vec3 trackedPosition = this.getTrackedPosition();
        float borderSize = entity.getCollisionBorderSize();
        return entity.getEntityBoundingBox()
                .expand(borderSize, borderSize, borderSize)
                .offset(trackedPosition.xCoord - entity.posX, trackedPosition.yCoord - entity.posY, trackedPosition.zCoord - entity.posZ);
    }

    public Vec3 getNearestTrackedPosition(EntityLivingBase entity) {
        return this.getNearestTrackedBox(entity) == null ? null : this.getTrackedPosition();
    }

    public double getNearestTrackedDistance(EntityLivingBase entity) {
        AxisAlignedBB trackedBox = this.getNearestTrackedBox(entity);
        return trackedBox == null ? Double.MAX_VALUE : myau.util.RotationUtil.distanceToBox(trackedBox);
    }

    private Vec3 getTrackedPosition() {
        return new Vec3(this.trackedServerX / 32.0, this.trackedServerY / 32.0, this.trackedServerZ / 32.0);
    }

    private boolean shouldQueuePacket(Packet<?> packet, EntityLivingBase trackedTarget) {
        if (packet instanceof S14PacketEntity) {
            return ((S14PacketEntity) packet).getEntity(mc.theWorld) == trackedTarget;
        }

        if (packet instanceof S18PacketEntityTeleport) {
            return ((S18PacketEntityTeleport) packet).getEntityId() == trackedTarget.getEntityId();
        }

        return false;
    }

    private void flushPackets() {
        if (mc.getNetHandler() == null) {
            this.reset(false);
            return;
        }

        List<Packet<?>> packetsToFlush;
        synchronized (this.queuedPackets) {
            packetsToFlush = new ArrayList<>(this.queuedPackets);
            this.queuedPackets.clear();
        }

        for (Packet<?> packet : packetsToFlush) {
            ((Packet) packet).processPacket(mc.getNetHandler());
        }

        this.lastFlushAt = System.currentTimeMillis();
        this.hasTrackedPosition = this.target != null;
        if (this.target != null) {
            this.trackedServerX = this.target.serverPosX;
            this.trackedServerY = this.target.serverPosY;
            this.trackedServerZ = this.target.serverPosZ;
        }
    }

    private void scheduleNextFlush() {
        int minimum = this.minDelay.getValue();
        int maximum = this.maxDelay.getValue();
        if (minimum > maximum) {
            int temp = minimum;
            minimum = maximum;
            maximum = temp;
        }
        this.nextFlushDelay = ThreadLocalRandom.current().nextLong(minimum, maximum + 1L);
        this.lastFlushAt = System.currentTimeMillis();
    }

    private void reset(boolean flushPackets) {
        if (flushPackets) {
            this.flushPackets();
        } else {
            synchronized (this.queuedPackets) {
                this.queuedPackets.clear();
            }
        }
        this.target = null;
        this.hasTrackedPosition = false;
        this.trackedServerX = 0.0;
        this.trackedServerY = 0.0;
        this.trackedServerZ = 0.0;
        this.lastFlushAt = 0L;
        this.nextFlushDelay = 0L;
    }

    private Color getRenderColor() {
        if (this.showPosition.getValue() == 2) {
            return ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
        }
        return new Color(72, 125, 227, 160);
    }

    @Override
    public void onDisabled() {
        this.reset(true);
    }

    @Override
    public void verifyValue(String value) {
        if (this.minDelay.getName().equals(value) && this.minDelay.getValue() > this.maxDelay.getValue()) {
            this.maxDelay.setValue(this.minDelay.getValue());
        } else if (this.maxDelay.getName().equals(value) && this.maxDelay.getValue() < this.minDelay.getValue()) {
            this.minDelay.setValue(this.maxDelay.getValue());
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.format("%dms", this.getDisplayedDelayMs())};
    }

    private int getDisplayedDelayMs() {
        if (this.nextFlushDelay > 0L) {
            return (int) this.nextFlushDelay;
        }

        int minimum = this.minDelay.getValue();
        int maximum = this.maxDelay.getValue();
        return minimum == maximum ? minimum : (minimum + maximum) / 2;
    }
}
