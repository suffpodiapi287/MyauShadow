package myau.module.modules;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.PacketUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.world.WorldSettings;

public class LegitReach extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int LEGIT_REACH_FAKE_PLAYER_ID = -1337;

    public final ModeProperty mode = new ModeProperty("mode", 1, new String[]{"INTAVE", "FAKE_PLAYER"});
    public final BooleanProperty aura = new BooleanProperty("aura", false);
    public final IntProperty pulseDelay = new IntProperty(
            "pulse-delay", 200, 50, 500, this::isPulseMode
    );
    public final IntProperty intavePackets = new IntProperty(
            "intave-packets", 5, 0, 30, this::isIntaveMode
    );

    private final TimerUtil pulseTimer = new TimerUtil();
    private final TimerUtil combatTimer = new TimerUtil();

    private EntityOtherPlayerMP fakePlayer;
    private EntityLivingBase currentTarget;
    private boolean shown;

    public LegitReach() {
        super("LegitReach", false);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        Entity entity = event.getTarget();
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }

        EntityLivingBase target = (EntityLivingBase) entity;

        if (target == this.fakePlayer) {
            if (this.currentTarget != null) {
                this.attackEntity(this.currentTarget);
                this.combatTimer.reset();
            }
            return;
        }

        this.currentTarget = target;
        this.combatTimer.reset();

        this.removeFakePlayerOnly();
        this.createFakePlayer(target);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.SEND || mc.theWorld == null) {
            return;
        }

        if (!(event.getPacket() instanceof C02PacketUseEntity)) {
            return;
        }

        C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
        if (packet.getAction() != Action.ATTACK) {
            return;
        }

        Entity entity = packet.getEntityFromWorld(mc.theWorld);
        if (entity != null && entity == this.fakePlayer) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST) {
            return;
        }

        if (mc.thePlayer == null || mc.theWorld == null || this.currentTarget == null || !this.isInCombat()) {
            this.removeFakePlayer();
            return;
        }

        if (this.aura.getValue()) {
            KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
            if (killAura == null || !killAura.isEnabled()) {
                this.removeFakePlayer();
                return;
            }
        }

        if (!mc.theWorld.loadedEntityList.contains(this.currentTarget)) {
            this.removeFakePlayer();
            return;
        }

        if (!this.currentTarget.isEntityAlive() || this.currentTarget.isDead) {
            this.removeFakePlayer();
            return;
        }

        if (!this.shown) {
            this.createFakePlayer(this.currentTarget);
        }

        if (this.fakePlayer == null) {
            return;
        }

        if (this.fakePlayer != null && !mc.theWorld.loadedEntityList.contains(this.fakePlayer)) {
            this.fakePlayer = null;
            this.shown = false;
            return;
        }

        if (!this.fakePlayer.isEntityAlive()) {
            this.removeFakePlayer();
            return;
        }

        this.fakePlayer.setHealth(this.currentTarget.getHealth());
        for (int slot = 0; slot <= 4; slot++) {
            this.fakePlayer.setCurrentItemOrArmor(slot, this.currentTarget.getEquipmentInSlot(slot));
        }

        if (this.isIntaveMode()) {
            int interval = Math.max(1, this.intavePackets.getValue());
            if (mc.thePlayer.ticksExisted % interval == 0) {
                this.syncFakePlayerPosition();
            }
            return;
        }

        if (this.pulseTimer.hasTimeElapsed(this.pulseDelay.getValue())) {
            this.syncFakePlayerPosition();
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        this.removeFakePlayer();
    }

    @Override
    public void onEnabled() {
        this.shown = false;
        this.currentTarget = null;
        this.fakePlayer = null;
        this.pulseTimer.reset();
        this.combatTimer.setTime();
    }

    @Override
    public void onDisabled() {
        this.removeFakePlayer();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }

    private boolean isPulseMode() {
        int modeValue = this.mode.getValue();
        return modeValue == 0 || modeValue == 1;
    }

    private boolean isIntaveMode() {
        return this.mode.getValue() == 0;
    }

    private boolean isInCombat() {
        if (!this.combatTimer.hasTimeElapsed(500L)) {
            return true;
        }

        return this.currentTarget != null
                && mc.thePlayer != null
                && !this.currentTarget.isDead
                && this.currentTarget.isEntityAlive()
                && mc.thePlayer.getDistanceToEntity(this.currentTarget) <= 7.0F;
    }

    private void attackEntity(EntityLivingBase target) {
        if (mc.thePlayer == null) {
            return;
        }

        mc.thePlayer.swingItem();
        PacketUtil.sendPacket(new C02PacketUseEntity(target, Action.ATTACK));
        if (mc.playerController != null
                && mc.playerController.getCurrentGameType() != WorldSettings.GameType.SPECTATOR) {
            mc.thePlayer.attackTargetEntityWithCurrentItem(target);
        }
    }

    private void createFakePlayer(EntityLivingBase target) {
        if (target == null || mc.theWorld == null || mc.getNetHandler() == null) {
            return;
        }

        net.minecraft.client.network.NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(target.getUniqueID());
        if (info == null || info.getGameProfile() == null) {
            return;
        }

        this.removeFakePlayerOnly();

        EntityOtherPlayerMP faker = new EntityOtherPlayerMP(mc.theWorld, info.getGameProfile());
        faker.rotationYawHead = target.rotationYawHead;
        faker.renderYawOffset = target.renderYawOffset;
        faker.copyLocationAndAnglesFrom(target);
        faker.setHealth(target.getHealth());

        for (int slot = 0; slot <= 4; slot++) {
            faker.setCurrentItemOrArmor(slot, target.getEquipmentInSlot(slot));
        }

        mc.theWorld.removeEntityFromWorld(LEGIT_REACH_FAKE_PLAYER_ID);
        mc.theWorld.addEntityToWorld(LEGIT_REACH_FAKE_PLAYER_ID, faker);

        this.fakePlayer = faker;
        this.shown = true;
        this.pulseTimer.reset();
    }

    private void syncFakePlayerPosition() {
        if (this.fakePlayer == null || this.currentTarget == null) {
            return;
        }

        this.fakePlayer.rotationYawHead = this.currentTarget.rotationYawHead;
        this.fakePlayer.renderYawOffset = this.currentTarget.renderYawOffset;
        this.fakePlayer.copyLocationAndAnglesFrom(this.currentTarget);
        this.pulseTimer.reset();
    }

    private void removeFakePlayerOnly() {
        if (mc.theWorld != null) {
            mc.theWorld.removeEntityFromWorld(LEGIT_REACH_FAKE_PLAYER_ID);
        }
        this.fakePlayer = null;
        this.shown = false;
    }

    private void removeFakePlayer() {
        this.removeFakePlayerOnly();
        this.currentTarget = null;
    }
}