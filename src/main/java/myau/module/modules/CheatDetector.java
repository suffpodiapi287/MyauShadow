package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.notification.NotificationManager;
import myau.notification.NotificationType;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.ChatUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

import java.util.*;
import java.util.stream.Collectors;

public class CheatDetector extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final BooleanProperty aim = new BooleanProperty("aim", true);
    public final BooleanProperty invalidInteract = new BooleanProperty("invalid-interact", true);
    public final BooleanProperty motion = new BooleanProperty("motion", true);
    public final BooleanProperty noFall = new BooleanProperty("no-fall", true);
    public final BooleanProperty noSlow = new BooleanProperty("no-slow", true);
    public final BooleanProperty omniSprint = new BooleanProperty("omni-sprint", true);
    public final BooleanProperty scaffold = new BooleanProperty("scaffold", true);
    public final BooleanProperty legitScaffold = new BooleanProperty("legit-scaffold", true);
    public final BooleanProperty velocity = new BooleanProperty("velocity", true);
    public final BooleanProperty checkSelf = new BooleanProperty("check-self", false);
    public final BooleanProperty chatAlerts = new BooleanProperty("chat-alerts", true);
    public final BooleanProperty notifications = new BooleanProperty("notifications", true);
    public final IntProperty alertCooldown = new IntProperty("alert-cooldown", 1000, 0, 5000);

    private final Set<UUID> cheaters = new HashSet<>();
    private final List<Check> checks = Arrays.asList(
            new AimCheck(),
            new InvalidInteractCheck(),
            new MotionCheck(),
            new NoFallCheck(),
            new NoSlowCheck(),
            new OmniSprintCheck(),
            new ScaffoldCheck(),
            new VelocityCheck(),
            new LegitScaffoldCheck()
    );

    public CheatDetector() {
        super("CheatDetector", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null || event.getType() != EventType.PRE) {
            return;
        }

        if ((mc.thePlayer.ticksExisted & 31) == 0) {
            this.cleanup();
        }

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!this.shouldCheck(player)) {
                continue;
            }

            for (Check check : this.checks) {
                if (this.isCheckEnabled(check)) {
                    check.onUpdate(player);
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()
                || mc.thePlayer == null
                || mc.theWorld == null
                || event.isCancelled()
                || event.getType() != EventType.RECEIVE) {
            return;
        }

        Packet<?> packet = event.getPacket();
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!this.shouldCheck(player)) {
                continue;
            }

            for (Check check : this.checks) {
                if (this.isCheckEnabled(check)) {
                    check.onPacket(packet, player);
                }
            }
        }
    }

    @EventTarget
    public void onWorld(LoadWorldEvent event) {
        this.resetState();
    }

    @Override
    public void onDisabled() {
        this.resetState();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.valueOf(this.cheaters.size())};
    }

    public boolean isCheater(EntityPlayer player) {
        return player != null && this.cheaters.contains(player.getUniqueID());
    }

    public void mark(EntityPlayer player) {
        if (player != null) {
            this.cheaters.add(player.getUniqueID());
        }
    }

    private void resetState() {
        this.cheaters.clear();
        for (Check check : this.checks) {
            check.clear();
        }
    }

    private void cleanup() {
        if (mc.theWorld == null) {
            this.resetState();
            return;
        }

        Set<UUID> online = mc.theWorld.playerEntities.stream()
                .map(EntityPlayer::getUniqueID)
                .collect(Collectors.toSet());
        this.cheaters.removeIf(uuid -> !online.contains(uuid));
        for (Check check : this.checks) {
            check.cleanup(online);
        }
    }

    private boolean shouldCheck(EntityPlayer player) {
        if (player == null || player.isDead || player.ticksExisted < 10) {
            return false;
        }

        if (!this.checkSelf.getValue() && player == mc.thePlayer) {
            return false;
        }

        if (TeamUtil.isFriend(player)) {
            return false;
        }

        AntiBot antiBot = (AntiBot) Myau.moduleManager.getModule(AntiBot.class);
        if (antiBot != null && antiBot.isEnabled() && antiBot.isBot(player)) {
            return false;
        }

        double maxDistance = mc.gameSettings.renderDistanceChunks * 16.0 + 8.0;
        return mc.thePlayer.getDistanceToEntity(player) <= maxDistance;
    }

    private boolean isCheckEnabled(Check check) {
        String key = check.getGroup();
        if ("aim".equals(key)) return this.aim.getValue();
        if ("invalid-interact".equals(key)) return this.invalidInteract.getValue();
        if ("motion".equals(key)) return this.motion.getValue();
        if ("no-fall".equals(key)) return this.noFall.getValue();
        if ("no-slow".equals(key)) return this.noSlow.getValue();
        if ("omni-sprint".equals(key)) return this.omniSprint.getValue();
        if ("scaffold".equals(key)) return this.scaffold.getValue();
        if ("legit-scaffold".equals(key)) return this.legitScaffold.getValue();
        if ("velocity".equals(key)) return this.velocity.getValue();
        return false;
    }

    private void alert(EntityPlayer player, String checkName, String verbose) {
        this.mark(player);
        if (this.chatAlerts.getValue()) {
            ChatUtil.sendFormatted(
                    String.format(
                            "%s&f%s &7failed &c%s&7 - &f%s",
                            Myau.clientName,
                            player.getName(),
                            checkName,
                            verbose
                    )
            );
        }
        if (this.notifications.getValue()) {
            NotificationManager.addNotification("CheatDetector", player.getName(), checkName + " | " + verbose, NotificationType.WARNING);
        }
    }

    private abstract class Check {
        private final String group;
        private final String name;
        private final Map<UUID, Long> lastFlags = new HashMap<>();

        protected Check(String group, String name) {
            this.group = group;
            this.name = name;
        }

        public String getGroup() {
            return this.group;
        }

        public void onUpdate(EntityPlayer player) {
        }

        public void onPacket(Packet<?> packet, EntityPlayer player) {
        }

        public void cleanup(Set<UUID> online) {
            this.lastFlags.keySet().removeIf(uuid -> !online.contains(uuid));
        }

        public void clear() {
            this.lastFlags.clear();
        }

        protected void flag(EntityPlayer player, String verbose) {
            long now = System.currentTimeMillis();
            long last = this.lastFlags.getOrDefault(player.getUniqueID(), 0L);
            if (now - last < CheatDetector.this.alertCooldown.getValue()) {
                return;
            }
            this.lastFlags.put(player.getUniqueID(), now);
            CheatDetector.this.alert(player, this.name, verbose);
        }

        protected boolean isMoving(EntityPlayer player) {
            return player != null && (
                    Math.abs(player.motionX) > 0.08D
                            || Math.abs(player.motionZ) > 0.08D
                            || player.moveForward != 0.0F
                            || player.moveStrafing != 0.0F
            );
        }
    }

    private final class AimCheck extends Check {
        private final Map<UUID, Float> lastYaw = new HashMap<>();

        private AimCheck() {
            super("aim", "Aim");
        }

        @Override
        public void onUpdate(EntityPlayer player) {
            UUID uuid = player.getUniqueID();
            float previousYaw = this.lastYaw.getOrDefault(uuid, player.rotationYawHead);
            float yawDelta = Math.abs(MathHelper.wrapAngleTo180_float(player.rotationYawHead - previousYaw));

            if (player.swingProgress > 0.0F && yawDelta > 175.0F) {
                this.flag(player, "Impossible yaw change");
            }

            if (player.rotationPitch > 90.0F || player.rotationPitch < -90.0F) {
                this.flag(player, "Invalid pitch");
            }

            this.lastYaw.put(uuid, player.rotationYawHead);
        }

        @Override
        public void cleanup(Set<UUID> online) {
            super.cleanup(online);
            this.lastYaw.keySet().removeIf(uuid -> !online.contains(uuid));
        }

        @Override
        public void clear() {
            super.clear();
            this.lastYaw.clear();
        }
    }

    private final class InvalidInteractCheck extends Check {
        private final Map<UUID, Integer> useTicks = new HashMap<>();

        private InvalidInteractCheck() {
            super("invalid-interact", "Invalid interact");
        }

        @Override
        public void onPacket(Packet<?> packet, EntityPlayer player) {
            UUID uuid = player.getUniqueID();
            int ticks = this.useTicks.getOrDefault(uuid, 0);

            if (player.isUsingItem()) {
                ticks++;
            } else {
                ticks = 0;
            }

            this.useTicks.put(uuid, ticks);

            if (ticks > 2 && packet instanceof S0BPacketAnimation) {
                S0BPacketAnimation animation = (S0BPacketAnimation) packet;
                if (animation.getEntityID() == player.getEntityId()) {
                    this.flag(player, "Swinging while using an item");
                }
            }
        }

        @Override
        public void cleanup(Set<UUID> online) {
            super.cleanup(online);
            this.useTicks.keySet().removeIf(uuid -> !online.contains(uuid));
        }

        @Override
        public void clear() {
            super.clear();
            this.useTicks.clear();
        }
    }

    private final class MotionCheck extends Check {
        private final Map<UUID, Integer> airTicks = new HashMap<>();

        private MotionCheck() {
            super("motion", "Motion");
        }

        @Override
        public void onUpdate(EntityPlayer player) {
            if (player.capabilities.isFlying || player.capabilities.allowFlying) {
                this.airTicks.put(player.getUniqueID(), 0);
                return;
            }

            UUID uuid = player.getUniqueID();
            int ticks = this.airTicks.getOrDefault(uuid, 0);
            if (!player.onGround) {
                ticks++;
            } else {
                ticks = 0;
            }
            this.airTicks.put(uuid, ticks);

            if (ticks > 5 && player.motionY == 0.0D && this.isMoving(player)) {
                this.flag(player, "Ignoring gravity");
            }
        }

        @Override
        public void cleanup(Set<UUID> online) {
            super.cleanup(online);
            this.airTicks.keySet().removeIf(uuid -> !online.contains(uuid));
        }

        @Override
        public void clear() {
            super.clear();
            this.airTicks.clear();
        }
    }

    private final class NoFallCheck extends Check {
        private final Map<UUID, Boolean> falling = new HashMap<>();

        private NoFallCheck() {
            super("no-fall", "No fall");
        }

        @Override
        public void onUpdate(EntityPlayer player) {
            UUID uuid = player.getUniqueID();
            boolean value = this.falling.getOrDefault(uuid, false);

            if (player.fallDistance > player.getMaxFallHeight()
                    && player.ticksExisted > 20
                    && !player.capabilities.disableDamage
                    && !player.capabilities.allowFlying) {
                value = true;
            }

            if (value && player.fallDistance == 0.0F && player.hurtTime == 0 && !player.isInWater() && player.onGround) {
                this.flag(player, "Not taking fall damage");
                value = false;
            }

            this.falling.put(uuid, value);
        }

        @Override
        public void cleanup(Set<UUID> online) {
            super.cleanup(online);
            this.falling.keySet().removeIf(uuid -> !online.contains(uuid));
        }

        @Override
        public void clear() {
            super.clear();
            this.falling.clear();
        }
    }

    private final class NoSlowCheck extends Check {
        private final Map<UUID, Integer> sprintBuffer = new HashMap<>();
        private final Map<UUID, Integer> speedBuffer = new HashMap<>();

        private NoSlowCheck() {
            super("no-slow", "No slow");
        }

        @Override
        public void onUpdate(EntityPlayer player) {
            UUID uuid = player.getUniqueID();
            int sprint = this.sprintBuffer.getOrDefault(uuid, 0);
            int speed = this.speedBuffer.getOrDefault(uuid, 0);

            if (player.isUsingItem() && player.hurtTime == 0) {
                if (player.isSprinting()) {
                    sprint++;
                    if (sprint > 5) {
                        this.flag(player, "Sprinting while using an item");
                        sprint = 0;
                    }
                } else {
                    sprint = Math.max(0, sprint - 1);
                }

                double horizontal = Math.hypot(player.motionX, player.motionZ);
                if ((player.onGround && horizontal > 0.15D) || (!player.onGround && horizontal > 0.30D)) {
                    speed++;
                    if (speed > 5) {
                        this.flag(player, "Moving too fast while using an item");
                        speed = 0;
                    }
                } else {
                    speed = Math.max(0, speed - 1);
                }
            } else {
                sprint = 0;
                speed = 0;
            }

            this.sprintBuffer.put(uuid, sprint);
            this.speedBuffer.put(uuid, speed);
        }

        @Override
        public void cleanup(Set<UUID> online) {
            super.cleanup(online);
            this.sprintBuffer.keySet().removeIf(uuid -> !online.contains(uuid));
            this.speedBuffer.keySet().removeIf(uuid -> !online.contains(uuid));
        }

        @Override
        public void clear() {
            super.clear();
            this.sprintBuffer.clear();
            this.speedBuffer.clear();
        }
    }

    private final class OmniSprintCheck extends Check {
        private final Map<UUID, Integer> sprintTicks = new HashMap<>();

        private OmniSprintCheck() {
            super("omni-sprint", "Omni sprint");
        }

        @Override
        public void onUpdate(EntityPlayer player) {
            UUID uuid = player.getUniqueID();
            int ticks = this.sprintTicks.getOrDefault(uuid, 0);

            if ((player.moveForward < 0.0F || (player.moveForward == 0.0F && player.moveStrafing != 0.0F)) && player.isSprinting()) {
                ticks++;
            } else {
                ticks = 0;
            }

            if (ticks > 2) {
                this.flag(player, "Sprinting while moving backwards");
            }

            this.sprintTicks.put(uuid, ticks);
        }

        @Override
        public void cleanup(Set<UUID> online) {
            super.cleanup(online);
            this.sprintTicks.keySet().removeIf(uuid -> !online.contains(uuid));
        }

        @Override
        public void clear() {
            super.clear();
            this.sprintTicks.clear();
        }
    }

    private final class VelocityCheck extends Check {
        private VelocityCheck() {
            super("velocity", "Velocity");
        }

        @Override
        public void onUpdate(EntityPlayer player) {
            double speed = Math.hypot(player.motionX, player.motionZ);
            if (speed == 0.0D
                    && player.hurtTime < 6
                    && player.hurtTime > 2
                    && !mc.theWorld.checkBlockCollision(player.getEntityBoundingBox().expand(0.05D, 0.0D, 0.05D))) {
                this.flag(player, "Invalid velocity");
            }
        }
    }

    private final class ScaffoldCheck extends Check {
        private final Map<UUID, Integer> blocksPlaced = new HashMap<>();
        private final Map<UUID, Boolean> bridging = new HashMap<>();
        private final Map<UUID, Float> lastYaw = new HashMap<>();
        private final Map<UUID, Integer> yawSnaps = new HashMap<>();
        private final Map<UUID, Float> lastPitch = new HashMap<>();
        private final Map<UUID, Double> pitchBuffer = new HashMap<>();
        private final Map<UUID, Long> bridgeTime = new HashMap<>();
        private long lastPlacementFlag;

        private ScaffoldCheck() {
            super("scaffold", "Scaffold");
        }

        @Override
        public void onUpdate(EntityPlayer player) {
            UUID uuid = player.getUniqueID();
            boolean bridgingNow = this.isBridging(player);
            int placed = this.blocksPlaced.getOrDefault(uuid, 0);
            boolean lastBridging = this.bridging.getOrDefault(uuid, false);

            if (lastBridging && player.swingProgressInt == 0 && player.isSwingInProgress) {
                placed++;
            }

            if (bridgingNow) {
                this.bridgeTime.put(uuid, System.currentTimeMillis());
            } else if (System.currentTimeMillis() - this.bridgeTime.getOrDefault(uuid, 0L) > 1000L) {
                placed = 0;
            }

            if (!this.isStableBridgeMovement(player)) {
                placed = 0;
            }

            if (placed > 7) {
                placed = 0;
            }

            this.blocksPlaced.put(uuid, placed);
            this.bridging.put(uuid, bridgingNow);

            if (bridgingNow && player.isSwingInProgress && this.isStableBridgeMovement(player)) {
                float previousYaw = this.lastYaw.getOrDefault(uuid, player.rotationYawHead);
                int snapCount = this.yawSnaps.getOrDefault(uuid, 0);
                if (Math.abs(MathHelper.wrapAngleTo180_float(player.rotationYawHead - previousYaw)) > 45.0F && !player.isSneaking()) {
                    snapCount++;
                    if (snapCount > 2) {
                        this.flag(player, "Suspicious yaw change");
                        snapCount = 0;
                    }
                }
                this.yawSnaps.put(uuid, snapCount);

                float previousPitch = this.lastPitch.getOrDefault(uuid, player.rotationPitch);
                double pitchBuf = this.pitchBuffer.getOrDefault(uuid, 0.0D);
                double pitchDelta = Math.abs(player.rotationPitch - previousPitch);
                if (pitchDelta > 2.0D) {
                    pitchBuf += 1.4D;
                    if (pitchBuf > 4.0D) {
                        this.flag(player, "Suspicious pitch change");
                        pitchBuf = 0.0D;
                    }
                } else {
                    pitchBuf = Math.max(0.0D, pitchBuf - 0.8D);
                }
                this.pitchBuffer.put(uuid, pitchBuf);
            }

            if (placed > 6 && System.currentTimeMillis() - this.lastPlacementFlag > 500L) {
                this.flag(player, "Suspicious block placement");
                this.lastPlacementFlag = System.currentTimeMillis();
            }

            this.lastYaw.put(uuid, player.rotationYawHead);
            this.lastPitch.put(uuid, player.rotationPitch);
        }

        @Override
        public void onPacket(Packet<?> packet, EntityPlayer player) {
            if (packet instanceof S25PacketBlockBreakAnim) {
                S25PacketBlockBreakAnim blockBreakAnim = (S25PacketBlockBreakAnim) packet;
                if (blockBreakAnim.getBreakerId() == player.getEntityId()) {
                    this.bridging.put(player.getUniqueID(), false);
                }
            }
        }

        private boolean isBridging(EntityPlayer player) {
            return player.rotationPitch > 70.0F
                    && player.getHeldItem() != null
                    && player.getHeldItem().getItem() instanceof ItemBlock;
        }

        private boolean isStableBridgeMovement(EntityPlayer player) {
            return this.isMoving(player) && player.onGround && !player.isSneaking();
        }

        @Override
        public void cleanup(Set<UUID> online) {
            super.cleanup(online);
            this.blocksPlaced.keySet().removeIf(uuid -> !online.contains(uuid));
            this.bridging.keySet().removeIf(uuid -> !online.contains(uuid));
            this.lastYaw.keySet().removeIf(uuid -> !online.contains(uuid));
            this.yawSnaps.keySet().removeIf(uuid -> !online.contains(uuid));
            this.lastPitch.keySet().removeIf(uuid -> !online.contains(uuid));
            this.pitchBuffer.keySet().removeIf(uuid -> !online.contains(uuid));
            this.bridgeTime.keySet().removeIf(uuid -> !online.contains(uuid));
        }

        @Override
        public void clear() {
            super.clear();
            this.blocksPlaced.clear();
            this.bridging.clear();
            this.lastYaw.clear();
            this.yawSnaps.clear();
            this.lastPitch.clear();
            this.pitchBuffer.clear();
            this.bridgeTime.clear();
            this.lastPlacementFlag = 0L;
        }
    }

    private final class LegitScaffoldCheck extends Check {
        private final Map<UUID, Boolean> shouldSneak = new HashMap<>();
        private final Map<UUID, Integer> sneakTicks = new HashMap<>();
        private final Map<UUID, Integer> buffer = new HashMap<>();

        private LegitScaffoldCheck() {
            super("legit-scaffold", "Legit scaffold");
        }

        @Override
        public void onPacket(Packet<?> packet, EntityPlayer player) {
            UUID uuid = player.getUniqueID();
            boolean needsSneak = this.shouldSneak.getOrDefault(uuid, false);
            int ticks = this.sneakTicks.getOrDefault(uuid, 0);
            int buf = this.buffer.getOrDefault(uuid, 0);

            if (packet instanceof S14PacketEntity) {
                S14PacketEntity movement = (S14PacketEntity) packet;
                if (movement.getEntity(mc.theWorld) == player) {
                    if (movement.getOnGround()) {
                        BlockPos under = new BlockPos(player.posX, player.posY - 1.0D, player.posZ);
                        Block block = mc.theWorld.getBlockState(under).getBlock();
                        if (block instanceof BlockAir) {
                            needsSneak = true;
                            ticks++;
                        } else {
                            needsSneak = false;
                            ticks = 0;
                        }
                    } else {
                        needsSneak = false;
                        ticks = 0;
                    }
                }
            }

            if (packet instanceof S1CPacketEntityMetadata) {
                S1CPacketEntityMetadata metadata = (S1CPacketEntityMetadata) packet;
                if (metadata.getEntityId() == player.getEntityId() && metadata.func_149376_c() != null) {
                    for (DataWatcher.WatchableObject object : metadata.func_149376_c()) {
                        if (object.getObject() instanceof Byte && ((Byte) object.getObject()) == 2) {
                            if (needsSneak && ticks <= 2) {
                                buf = Math.min(10000, buf + 1);
                                if (buf > 10) {
                                    this.flag(player, "Sneaking too fast");
                                    needsSneak = false;
                                    ticks = 0;
                                    buf = 0;
                                }
                            } else {
                                buf = Math.max(0, buf - 5);
                            }
                        }
                    }
                }
            }

            this.shouldSneak.put(uuid, needsSneak);
            this.sneakTicks.put(uuid, ticks);
            this.buffer.put(uuid, buf);
        }

        @Override
        public void cleanup(Set<UUID> online) {
            super.cleanup(online);
            this.shouldSneak.keySet().removeIf(uuid -> !online.contains(uuid));
            this.sneakTicks.keySet().removeIf(uuid -> !online.contains(uuid));
            this.buffer.keySet().removeIf(uuid -> !online.contains(uuid));
        }

        @Override
        public void clear() {
            super.clear();
            this.shouldSneak.clear();
            this.sneakTicks.clear();
            this.buffer.clear();
        }
    }

    private boolean isMoving(EntityPlayer player) {
        return Math.hypot(player.motionX, player.motionZ) > 0.1D || Math.abs(player.moveForward) > 0.0F || Math.abs(player.moveStrafing) > 0.0F;
    }
}
