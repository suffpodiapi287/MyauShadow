package myau.management;

import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.PacketEvent;
import myau.events.TickEvent;
import myau.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.util.Vec3;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class BlinkManager {
    public enum BlinkDirection {
        SEND,
        RECEIVE,
        BOTH
    }

    public static Minecraft mc = Minecraft.getMinecraft();
    public BlinkModules blinkModule = BlinkModules.NONE;
    public boolean blinking = false;
    public BlinkDirection blinkDirection = BlinkDirection.SEND;
    public Deque<Packet<?>> blinkedPackets = new ConcurrentLinkedDeque<>();
    public Deque<Packet<?>> receivedPackets = new ConcurrentLinkedDeque<>();
    private boolean manualBlinking = false;
    private BlinkDirection manualBlinkDirection = BlinkDirection.SEND;
    private final Deque<Packet<?>> manualSentPackets = new ConcurrentLinkedDeque<>();
    private final Deque<Packet<?>> manualReceivedPackets = new ConcurrentLinkedDeque<>();
    private final Deque<Vec3> manualPositions = new ConcurrentLinkedDeque<>();
    private EntityOtherPlayerMP manualFakePlayer;
    private static final int MANUAL_FAKE_PLAYER_ID = -71337;

    public boolean offerPacket(Packet<?> packet) {
        return this.offerPacket(packet, EventType.SEND);
    }

    public boolean offerPacket(Packet<?> packet, EventType type) {
        if (this.manualBlinking) {
            if (type == EventType.SEND) {
                if (this.isManualSendBlinking() && !(packet instanceof C00PacketKeepAlive) && !(packet instanceof C01PacketChatMessage)) {
                    if (!(this.manualSentPackets.isEmpty() && packet instanceof C0FPacketConfirmTransaction)) {
                        this.manualSentPackets.offer(packet);
                        if (packet instanceof C03PacketPlayer) {
                            C03PacketPlayer playerPacket = (C03PacketPlayer) packet;
                            if (playerPacket.isMoving()) {
                                this.manualPositions.offer(new Vec3(playerPacket.getPositionX(), playerPacket.getPositionY(), playerPacket.getPositionZ()));
                            }
                        }
                        return true;
                    }
                }
            } else if (type == EventType.RECEIVE) {
                if (this.isManualReceiveBlinking() && !(packet instanceof S00PacketKeepAlive) && !(packet instanceof S02PacketChat)) {
                    this.manualReceivedPackets.offer(packet);
                    return true;
                }
            }
        }

        if (!this.blinking || this.blinkModule == BlinkModules.NONE) {
            return false;
        }

        if (type == EventType.SEND) {
            if (!this.isSendBlinking() || packet instanceof C00PacketKeepAlive || packet instanceof C01PacketChatMessage) {
                return false;
            }

            if (this.blinkedPackets.isEmpty() && packet instanceof C0FPacketConfirmTransaction) {
                return false;
            }

            this.blinkedPackets.offer(packet);
            return true;
        }

        if (type == EventType.RECEIVE) {
            if (!this.isReceiveBlinking() || packet instanceof S00PacketKeepAlive || packet instanceof S02PacketChat) {
                return false;
            }

            this.receivedPackets.offer(packet);
            return true;
        }

        return false;
    }

    public boolean setBlinkState(boolean state, BlinkModules module) {
        return this.setBlinkState(state, module, BlinkDirection.SEND);
    }

    public boolean setBlinkState(boolean state, BlinkModules module, BlinkDirection direction) {
        if (module == BlinkModules.NONE) {
            return false;
        }

        if (state) {
            this.blinkModule = module;
            this.blinking = true;
            this.blinkDirection = direction;
        } else {
            if (this.blinkModule != module) {
                return false;
            }

            this.blinking = false;

            if (Minecraft.getMinecraft().getNetHandler() != null) {
                while (!this.receivedPackets.isEmpty()) {
                    Packet<?> receivedPacket = this.receivedPackets.poll();
                    if (receivedPacket != null) {
                        ((Packet) receivedPacket).processPacket(Minecraft.getMinecraft().getNetHandler());
                    }
                }

                while (!this.blinkedPackets.isEmpty()) {
                    Packet<?> blinkedPacket = this.blinkedPackets.poll();
                    if (blinkedPacket != null) {
                        PacketUtil.sendPacketNoEvent(blinkedPacket);
                    }
                }
            } else {
                this.receivedPackets.clear();
                this.blinkedPackets.clear();
            }

            this.blinkModule = BlinkModules.NONE;
            this.blinkDirection = BlinkDirection.SEND;
        }
        return true;
    }

    public void setManualBlinkState(boolean state, BlinkDirection direction) {
        if (state) {
            this.manualBlinking = true;
            this.manualBlinkDirection = direction;
        } else {
            this.manualBlinking = false;
            this.unblinkManual();
            this.manualBlinkDirection = BlinkDirection.SEND;
        }
    }

    public boolean isManualBlinking() {
        return this.manualBlinking;
    }

    public long countManualMovement() {
        return this.manualSentPackets.stream().filter(packet -> packet instanceof C03PacketPlayer).count();
    }

    public int countManualQueuedPackets() {
        return this.manualSentPackets.size() + this.manualReceivedPackets.size();
    }

    public Deque<Vec3> getManualPositions() {
        return this.manualPositions;
    }

    public boolean isManualSendBlinking() {
        return this.manualBlinking && (this.manualBlinkDirection == BlinkDirection.SEND || this.manualBlinkDirection == BlinkDirection.BOTH);
    }

    public boolean isManualReceiveBlinking() {
        return this.manualBlinking && (this.manualBlinkDirection == BlinkDirection.RECEIVE || this.manualBlinkDirection == BlinkDirection.BOTH);
    }

    public void syncManualSent() {
        if (Minecraft.getMinecraft().getNetHandler() == null) {
            this.manualReceivedPackets.clear();
            return;
        }

        while (!this.manualReceivedPackets.isEmpty()) {
            Packet<?> receivedPacket = this.manualReceivedPackets.poll();
            if (receivedPacket != null) {
                ((Packet) receivedPacket).processPacket(Minecraft.getMinecraft().getNetHandler());
            }
        }
    }

    public void syncManualReceived() {
        while (!this.manualSentPackets.isEmpty()) {
            Packet<?> sentPacket = this.manualSentPackets.poll();
            if (sentPacket != null) {
                PacketUtil.sendPacketNoEvent(sentPacket);
            }
        }
    }

    public void unblinkManual() {
        this.syncManualSent();
        this.syncManualReceived();
        this.clearManual();
        this.removeManualFakePlayer();
    }

    public void clearManual() {
        this.manualReceivedPackets.clear();
        this.manualSentPackets.clear();
        this.manualPositions.clear();
    }

    public void addManualFakePlayer() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        this.removeManualFakePlayer();

        EntityOtherPlayerMP fakePlayer = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
        fakePlayer.copyLocationAndAnglesFrom(mc.thePlayer);
        fakePlayer.rotationYaw = mc.thePlayer.rotationYaw;
        fakePlayer.rotationPitch = mc.thePlayer.rotationPitch;
        fakePlayer.rotationYawHead = mc.thePlayer.rotationYawHead;
        fakePlayer.renderYawOffset = mc.thePlayer.renderYawOffset;
        fakePlayer.inventory.currentItem = mc.thePlayer.inventory.currentItem;
        fakePlayer.setCurrentItemOrArmor(0, mc.thePlayer.getHeldItem());
        for (int slot = 1; slot <= 4; slot++) {
            fakePlayer.setCurrentItemOrArmor(slot, mc.thePlayer.getCurrentArmor(slot - 1));
        }

        mc.theWorld.removeEntityFromWorld(MANUAL_FAKE_PLAYER_ID);
        mc.theWorld.addEntityToWorld(MANUAL_FAKE_PLAYER_ID, fakePlayer);
        this.manualFakePlayer = fakePlayer;
    }

    public void removeManualFakePlayer() {
        if (mc.theWorld != null) {
            mc.theWorld.removeEntityFromWorld(MANUAL_FAKE_PLAYER_ID);
        }
        this.manualFakePlayer = null;
    }

    public EntityOtherPlayerMP getManualFakePlayer() {
        return this.manualFakePlayer;
    }

    public BlinkModules getBlinkingModule() {
        return this.blinkModule;
    }

    public long countMovement() {
        return this.blinkedPackets.stream().filter(packet -> packet instanceof C03PacketPlayer).count();
    }

    public int countQueuedPackets() {
        return this.blinkedPackets.size() + this.receivedPackets.size();
    }

    public boolean isSendBlinking() {
        return this.isManualSendBlinking() || this.blinking && (this.blinkDirection == BlinkDirection.SEND || this.blinkDirection == BlinkDirection.BOTH);
    }

    public boolean isReceiveBlinking() {
        return this.isManualReceiveBlinking() || this.blinking && (this.blinkDirection == BlinkDirection.RECEIVE || this.blinkDirection == BlinkDirection.BOTH);
    }

    public boolean isBlinking() {
        return this.manualBlinking || this.blinking;
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE && !event.isCancelled() && this.offerPacket(event.getPacket(), event.getType())) {
            event.setCancelled(true);
            return;
        }

        if (event.getPacket() instanceof C00Handshake
                || event.getPacket() instanceof C00PacketLoginStart
                || event.getPacket() instanceof C00PacketServerQuery
                || event.getPacket() instanceof C01PacketPing
                || event.getPacket() instanceof C01PacketEncryptionResponse) {
            this.setBlinkState(false, this.blinkModule);
            this.setManualBlinkState(false, this.manualBlinkDirection);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.POST) {
            if (mc.thePlayer.isDead) {
                this.setBlinkState(false, this.blinkModule);
                this.setManualBlinkState(false, this.manualBlinkDirection);
            }
        }
    }

    private void flushManualPackets() {
        if (Minecraft.getMinecraft().getNetHandler() != null) {
            while (!this.manualReceivedPackets.isEmpty()) {
                Packet<?> receivedPacket = this.manualReceivedPackets.poll();
                if (receivedPacket != null) {
                    ((Packet) receivedPacket).processPacket(Minecraft.getMinecraft().getNetHandler());
                }
            }

            while (!this.manualSentPackets.isEmpty()) {
                Packet<?> sentPacket = this.manualSentPackets.poll();
                if (sentPacket != null) {
                    PacketUtil.sendPacketNoEvent(sentPacket);
                }
            }
        } else {
            this.manualReceivedPackets.clear();
            this.manualSentPackets.clear();
        }
    }
}
