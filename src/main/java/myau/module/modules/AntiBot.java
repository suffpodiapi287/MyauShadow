package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S20PacketEntityProperties;
import net.minecraft.potion.Potion;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AntiBot extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty tab = new BooleanProperty("tab", true);
    public final ModeProperty tabMode = new ModeProperty("tab-mode", 1, new String[]{"EQUALS", "CONTAINS"}, this.tab::getValue);
    public final BooleanProperty entityId = new BooleanProperty("entity-id", true);
    public final BooleanProperty invalidUuid = new BooleanProperty("invalid-uuid", true);
    public final BooleanProperty color = new BooleanProperty("color", false);
    public final BooleanProperty livingTime = new BooleanProperty("living-time", false);
    public final IntProperty livingTimeTicks = new IntProperty("living-time-ticks", 40, 1, 200, this.livingTime::getValue);
    public final BooleanProperty weaponCheck = new BooleanProperty("weapon-check", false);
    public final BooleanProperty hvhCheck = new BooleanProperty("HvH-Check", false);
    public final BooleanProperty onlyWoodenSword = new BooleanProperty("Only-Wooden-Sword", false, this.hvhCheck::getValue);
    public final BooleanProperty armorCheck = new BooleanProperty("Armor-Check", false, this.hvhCheck::getValue);
    public final BooleanProperty capabilities = new BooleanProperty("capabilities", true);
    public final BooleanProperty ground = new BooleanProperty("ground", true);
    public final BooleanProperty air = new BooleanProperty("air", false);
    public final BooleanProperty invalidGround = new BooleanProperty("invalid-ground", true);
    public final BooleanProperty invalidSpeed = new BooleanProperty("invalid-speed", false);
    public final BooleanProperty swing = new BooleanProperty("swing", false);
    public final BooleanProperty health = new BooleanProperty("health", false);
    public final BooleanProperty derp = new BooleanProperty("derp", true);
    public final BooleanProperty wasInvisible = new BooleanProperty("was-invisible", false);
    public final BooleanProperty armor = new BooleanProperty("armor", false);
    public final BooleanProperty ping = new BooleanProperty("ping", false);
    public final BooleanProperty needHit = new BooleanProperty("need-hit", false);
    public final BooleanProperty duplicateInWorld = new BooleanProperty("duplicate-in-world", false);
    public final BooleanProperty duplicateInTab = new BooleanProperty("duplicate-in-tab", false);
    public final BooleanProperty duplicateProfile = new BooleanProperty("duplicate-profile", false);
    public final BooleanProperty properties = new BooleanProperty("properties", false);
    public final BooleanProperty alwaysInRadius = new BooleanProperty("always-in-radius", false);
    public final FloatProperty alwaysRadius = new FloatProperty("always-radius", 20.0F, 3.0F, 30.0F, this.alwaysInRadius::getValue);
    public final IntProperty alwaysRadiusTick = new IntProperty("always-radius-tick", 50, 1, 100, this.alwaysInRadius::getValue);
    public final BooleanProperty alwaysBehind = new BooleanProperty("always-behind", false);
    public final FloatProperty alwaysBehindRadius = new FloatProperty("always-behind-radius", 10.0F, 3.0F, 30.0F, this.alwaysBehind::getValue);
    public final FloatProperty behindRotDiffToIgnore = new FloatProperty("behind-rot-diff", 90.0F, 1.0F, 180.0F, this.alwaysBehind::getValue);

    private final Set<Integer> groundList = new HashSet<>();
    private final Set<Integer> airList = new HashSet<>();
    private final Map<Integer, Integer> invalidGroundList = new HashMap<>();
    private final Set<Integer> invalidSpeedList = new HashSet<>();
    private final Set<Integer> swingList = new HashSet<>();
    private final Set<Integer> invisibleList = new HashSet<>();
    private final Set<Integer> propertiesList = new HashSet<>();
    private final Set<Integer> hitList = new HashSet<>();
    private final Set<Integer> alwaysInRadiusList = new HashSet<>();
    private final Set<Integer> alwaysBehindList = new HashSet<>();
    private final Map<Integer, Integer> entityTickMap = new HashMap<>();
    private long lastAttackTime;

    public AntiBot() {
        super("AntiBot", false);
    }

    public boolean isBot(EntityLivingBase entityLivingBase) {
        if (!(entityLivingBase instanceof EntityPlayer) || !this.isEnabled() || mc.thePlayer == null || mc.theWorld == null || mc.getNetHandler() == null) {
            return false;
        }

        EntityPlayer entity = (EntityPlayer) entityLivingBase;
        if (entity == mc.thePlayer) {
            return false;
        }

        if (this.hvhCheck.getValue()) {
            if (this.onlyWoodenSword.getValue()) {
                ItemStack heldItem = entity.getHeldItem();
                if (heldItem == null || heldItem.getItem() != Items.wooden_sword) {
                    return true;
                }
            }

            if (this.armorCheck.getValue()) {
                ItemStack helmet = entity.inventory.armorInventory[3];
                ItemStack chestplate = entity.inventory.armorInventory[2];

                if (!isColoredLeather(helmet) || !isColoredLeather(chestplate)) {
                    return true;
                }
            }
        }

        if (this.color.getValue()) {
            String formattedName = entity.getDisplayName().getFormattedText().replace("\u00A7r", "");
            if (!formattedName.contains("\u00A7")) {
                return true;
            }
        }

        if (this.livingTime.getValue() && entity.ticksExisted < this.livingTimeTicks.getValue()) {
            return true;
        }

        if (this.ground.getValue() && !this.groundList.contains(entity.getEntityId())) {
            return true;
        }

        if (this.air.getValue() && !this.airList.contains(entity.getEntityId())) {
            return true;
        }

        if (this.swing.getValue() && !this.swingList.contains(entity.getEntityId())) {
            return true;
        }

        if (this.health.getValue() && (entity.getHealth() > 20.0F || entity.getHealth() < 0.0F)) {
            return true;
        }

        if (this.entityId.getValue() && (entity.getEntityId() >= 1000000000 || entity.getEntityId() <= 0)) {
            return true;
        }

        if (this.derp.getValue() && (entity.rotationPitch > 90.0F || entity.rotationPitch < -90.0F)) {
            return true;
        }

        if (this.wasInvisible.getValue() && this.invisibleList.contains(entity.getEntityId())) {
            return true;
        }

        if (this.properties.getValue() && !this.propertiesList.contains(entity.getEntityId())) {
            return true;
        }

        if (this.armor.getValue()
                && entity.inventory.armorInventory[0] == null
                && entity.inventory.armorInventory[1] == null
                && entity.inventory.armorInventory[2] == null
                && entity.inventory.armorInventory[3] == null) {
            return true;
        }

        if (this.ping.getValue() && this.getPing(entity) == 0) {
            return true;
        }

        if (this.invalidUuid.getValue() && mc.getNetHandler().getPlayerInfo(entity.getUniqueID()) == null) {
            return true;
        }

        if (this.capabilities.getValue()
                && (entity.isSpectator()
                || entity.capabilities.isFlying
                || entity.capabilities.allowFlying
                || entity.capabilities.disableDamage
                || entity.capabilities.isCreativeMode)) {
            return true;
        }

        if (this.invalidSpeed.getValue() && this.invalidSpeedList.contains(entity.getEntityId())) {
            return true;
        }

        if (this.needHit.getValue() && !this.hitList.contains(entity.getEntityId())) {
            return true;
        }

        if (this.invalidGround.getValue() && this.invalidGroundList.getOrDefault(entity.getEntityId(), 0) >= 10) {
            return true;
        }

        if (this.alwaysInRadius.getValue() && this.alwaysInRadiusList.contains(entity.getEntityId())) {
            return true;
        }

        if (this.alwaysBehind.getValue() && this.alwaysBehindList.contains(entity.getEntityId())) {
            return true;
        }

        if (this.duplicateProfile.getValue()) {
            int duplicates = 0;
            for (NetworkPlayerInfo playerInfo : mc.getNetHandler().getPlayerInfoMap()) {
                if (playerInfo.getGameProfile() != null
                        && entity.getGameProfile() != null
                        && playerInfo.getGameProfile().getName().equals(entity.getGameProfile().getName())
                        && !playerInfo.getGameProfile().getId().equals(entity.getGameProfile().getId())) {
                    duplicates++;
                }
            }
            if (duplicates > 0) {
                return true;
            }
        }

        if (this.duplicateInWorld.getValue()) {
            Set<String> seenNames = new HashSet<>();
            Set<String> duplicateNames = new HashSet<>();
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                String playerName = player.getName();
                if (!seenNames.add(playerName)) {
                    duplicateNames.add(playerName);
                }
            }
            if (duplicateNames.contains(entity.getName())) {
                return true;
            }
        }

        if (this.duplicateInTab.getValue()) {
            Set<String> seenNames = new HashSet<>();
            Set<String> duplicateNames = new HashSet<>();
            for (NetworkPlayerInfo playerInfo : mc.getNetHandler().getPlayerInfoMap()) {
                String playerName = stripColor(this.getFullName(playerInfo));
                if (!seenNames.add(playerName)) {
                    duplicateNames.add(playerName);
                }
            }
            if (duplicateNames.contains(stripColor(entity.getDisplayName().getFormattedText()))) {
                return true;
            }
        }

        if (this.tab.getValue()) {
            boolean equals = this.tabMode.getValue() == 0;
            String targetName = stripColor(entity.getDisplayName().getFormattedText());
            boolean found = false;
            for (NetworkPlayerInfo playerInfo : mc.getNetHandler().getPlayerInfoMap()) {
                String networkName = stripColor(this.getFullName(playerInfo));
                if (equals ? targetName.equals(networkName) : targetName.contains(networkName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return true;
            }
        }

        return entity.getName().isEmpty() || entity.getName().equals(mc.thePlayer.getName());
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        Packet<?> packet = event.getPacket();

        if (packet instanceof S14PacketEntity) {
            Entity entity = ((S14PacketEntity) packet).getEntity(mc.theWorld);
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                int entityId = player.getEntityId();

                if (player.onGround) {
                    this.groundList.add(entityId);
                } else {
                    this.airList.add(entityId);
                }

                if (player.onGround) {
                    if (player.fallDistance > 0.0F || player.posY == player.prevPosY || !player.isCollidedVertically) {
                        this.invalidGroundList.put(entityId, this.invalidGroundList.getOrDefault(entityId, 0) + 1);
                    }
                } else {
                    int currentVl = this.invalidGroundList.getOrDefault(entityId, 0);
                    if (currentVl > 0) {
                        this.invalidGroundList.put(entityId, currentVl - 1);
                    } else {
                        this.invalidGroundList.remove(entityId);
                    }
                }

                if ((player.isInvisible() || player.isInvisibleToPlayer(mc.thePlayer))) {
                    this.invisibleList.add(entityId);
                }

                if (this.alwaysInRadius.getValue()) {
                    double distance = mc.thePlayer.getDistanceToEntity(player);
                    int currentTicks = this.entityTickMap.getOrDefault(entityId, 0);
                    if (distance < this.alwaysRadius.getValue()) {
                        currentTicks++;
                    } else {
                        currentTicks = 0;
                    }
                    this.entityTickMap.put(entityId, currentTicks);
                    if (currentTicks >= this.alwaysRadiusTick.getValue()) {
                        this.alwaysInRadiusList.add(entityId);
                    } else {
                        this.alwaysInRadiusList.remove(entityId);
                    }
                }

                if (this.alwaysBehind.getValue()) {
                    double distance = mc.thePlayer.getDistanceToEntity(player);
                    float angleDifference = Math.abs(MathHelper.wrapAngleTo180_float(
                            RotationUtil.getYawBetween(mc.thePlayer.posX, mc.thePlayer.posZ, player.posX, player.posZ)
                    ));
                    if (distance < this.alwaysBehindRadius.getValue() && angleDifference > this.behindRotDiffToIgnore.getValue()) {
                        this.alwaysBehindList.add(entityId);
                    } else {
                        this.alwaysBehindList.remove(entityId);
                    }
                }

                if (this.invalidSpeed.getValue()) {
                    double deltaX = player.posX - player.prevPosX;
                    double deltaZ = player.posZ - player.prevPosZ;
                    double speed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                    boolean moving = Math.abs(deltaX) > 1.0E-3 || Math.abs(deltaZ) > 1.0E-3;
                    if (speed >= 0.45 && speed <= 0.46
                            && (!player.isSprinting() || !moving || player.getActivePotionEffect(Potion.moveSpeed) == null)) {
                        this.invalidSpeedList.add(entityId);
                    }
                }
            }
        }

        if (packet instanceof S0BPacketAnimation) {
            S0BPacketAnimation animationPacket = (S0BPacketAnimation) packet;
            Entity entity = mc.theWorld.getEntityByID(animationPacket.getEntityID());
            if (entity instanceof EntityLivingBase && animationPacket.getAnimationType() == 0) {
                this.swingList.add(entity.getEntityId());
            }
        }

        if (packet instanceof S20PacketEntityProperties) {
            this.propertiesList.add(((S20PacketEntityProperties) packet).getEntityId());
        }

        if (packet instanceof S13PacketDestroyEntities) {
            for (int entityId : ((S13PacketDestroyEntities) packet).getEntityIDs()) {
                this.removeTracking(entityId);
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled()) {
            return;
        }

        this.lastAttackTime = System.currentTimeMillis();
        if (event.getTarget() instanceof EntityLivingBase) {
            this.hitList.add(event.getTarget().getEntityId());
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        this.clearAll();
    }

    private void removeTracking(int entityId) {
        this.groundList.remove(entityId);
        this.airList.remove(entityId);
        this.invalidGroundList.remove(entityId);
        this.invalidSpeedList.remove(entityId);
        this.swingList.remove(entityId);
        this.invisibleList.remove(entityId);
        this.propertiesList.remove(entityId);
        this.hitList.remove(entityId);
        this.alwaysInRadiusList.remove(entityId);
        this.alwaysBehindList.remove(entityId);
        this.entityTickMap.remove(entityId);
    }

    private void clearAll() {
        this.groundList.clear();
        this.airList.clear();
        this.invalidGroundList.clear();
        this.invalidSpeedList.clear();
        this.swingList.clear();
        this.invisibleList.clear();
        this.propertiesList.clear();
        this.hitList.clear();
        this.alwaysInRadiusList.clear();
        this.alwaysBehindList.clear();
        this.entityTickMap.clear();
        this.lastAttackTime = 0L;
    }

    private int getPing(EntityPlayer player) {
        NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
        return playerInfo == null ? -1 : playerInfo.getResponseTime();
    }

    private String getFullName(NetworkPlayerInfo playerInfo) {
        if (playerInfo.getDisplayName() != null) {
            return playerInfo.getDisplayName().getFormattedText();
        }

        ScorePlayerTeam team = playerInfo.getPlayerTeam();
        String profileName = playerInfo.getGameProfile() == null ? "" : playerInfo.getGameProfile().getName();
        return ScorePlayerTeam.formatPlayerName(team, profileName);
    }

    private static String stripColor(String text) {
        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(text);
        return stripped == null ? "" : stripped.trim();
    }

    private boolean isColoredLeather(ItemStack stack) {
        if (stack == null) return false;
        Item item = stack.getItem();

        // Kiểm tra xem có phải đồ da không (Leather Armor)
        if (!(item instanceof net.minecraft.item.ItemArmor)) return false;
        net.minecraft.item.ItemArmor armor = (net.minecraft.item.ItemArmor) item;

        if (armor.getArmorMaterial() != net.minecraft.item.ItemArmor.ArmorMaterial.LEATHER) {
            return false;
        }

        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("display", 10)) {
            if (stack.getTagCompound().getCompoundTag("display").hasKey("color", 3)) {
                int color = stack.getTagCompound().getCompoundTag("display").getInteger("color");
                return color != 10511680;
            }
        }
        return false;
    }

    @Override
    public void onEnabled() {
        this.clearAll();
    }

    @Override
    public void onDisabled() {
        this.clearAll();
    }
}
