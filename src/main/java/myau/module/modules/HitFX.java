package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.ModeProperty;
import myau.util.SoundUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.server.S19PacketEntityStatus;

import java.util.concurrent.ThreadLocalRandom;

public class HitFX extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final long TARGET_SOUND_WINDOW_MS = 1500L;
    private static final String[] SOUND_MODES = new String[]{
            "HIT", "ORB", "BONK", "BOYKISSER", "BRING", "GLASS", "CLICK", "MEOW",
            "MOAN", "MAGICSQUASH", "NYA", "POP", "SOFT", "SQUASH", "TUNG", "UWU"
    };
    private static final String[] HIT_SOUNDS = new String[]{"random.successful_hit"};
    private static final String[] ORB_SOUNDS = new String[]{"random.orb"};
    private static final String[] BONK_SOUNDS = new String[]{"myau:bonk"};
    private static final String[] BOYKISSER_SOUNDS = new String[]{
            "myau:boykisser-1", "myau:boykisser-2", "myau:boykisser-3",
            "myau:boykisser-4", "myau:boykisser-5", "myau:boykisser-6"
    };
    private static final String[] BRING_SOUNDS = new String[]{"myau:bring"};
    private static final String[] GLASS_SOUNDS = new String[]{"myau:glass-1", "myau:glass-2", "myau:glass-3"};
    private static final String[] CLICK_SOUNDS = new String[]{"myau:click-1", "myau:click-2", "myau:click-3"};
    private static final String[] MEOW_SOUNDS = new String[]{"myau:meow"};
    private static final String[] MOAN_SOUNDS = new String[]{"myau:moan-1", "myau:moan-2", "myau:moan-3", "myau:moan-4"};
    private static final String[] MAGIC_SQUASH_SOUNDS = new String[]{"myau:magic_squash"};
    private static final String[] NYA_SOUNDS = new String[]{"myau:nya"};
    private static final String[] POP_SOUNDS = new String[]{"myau:pop"};
    private static final String[] SOFT_SOUNDS = new String[]{"myau:soft"};
    private static final String[] SQUASH_SOUNDS = new String[]{"myau:squash"};
    private static final String[] TUNG_SOUNDS = new String[]{"myau:tung"};
    private static final String[] UWU_SOUNDS = new String[]{"myau:uwu"};

    public final ModeProperty otherSound = new ModeProperty("other-sound", 11, SOUND_MODES);
    public final ModeProperty selfSound = new ModeProperty("self-sound", 3, SOUND_MODES);

    private int lastTargetId = -1;
    private long lastAttackTime = 0L;

    public HitFX() {
        super("HitFX", false);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled() || mc.theWorld == null || !(event.getTarget() instanceof EntityLivingBase)) {
            return;
        }

        EntityLivingBase target = (EntityLivingBase) event.getTarget();
        if (target.isDead || target.deathTime > 0) {
            return;
        }

        this.lastTargetId = target.getEntityId();
        this.lastAttackTime = System.currentTimeMillis();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST) {
            return;
        }

        if (System.currentTimeMillis() - this.lastAttackTime > TARGET_SOUND_WINDOW_MS) {
            this.clearAttackPending();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()
                || event.isCancelled()
                || event.getType() != EventType.RECEIVE
                || mc.theWorld == null
                || !(event.getPacket() instanceof S19PacketEntityStatus)) {
            return;
        }

        S19PacketEntityStatus packet = (S19PacketEntityStatus) event.getPacket();
        if (packet.getOpCode() != 2 || this.lastTargetId == -1) {
            return;
        }

        Entity entity = packet.getEntity(mc.theWorld);
        if (!(entity instanceof EntityLivingBase) || entity.getEntityId() != this.lastTargetId) {
            return;
        }

        if (System.currentTimeMillis() - this.lastAttackTime > TARGET_SOUND_WINDOW_MS) {
            this.clearAttackPending();
            return;
        }

        SoundUtil.playSound(this.resolveSound(this.otherSound.getValue()));
        this.clearAttackPending();
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.clearState();
    }

    @Override
    public void onDisabled() {
        this.clearState();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.otherSound.getModeString(), this.selfSound.getModeString()};
    }

    public String getReplacementHurtSound(EntityLivingBase entity, String originalSound) {
        if (!this.isEnabled() || mc.thePlayer == null || entity == null || originalSound == null) {
            return originalSound;
        }

        if (entity == mc.thePlayer) {
            return SoundUtil.normalizeSoundName(this.resolveSound(this.selfSound.getValue()));
        }

        return originalSound;
    }

    private String resolveSound(int mode) {
        switch (mode) {
            case 0:
                return this.pickSound(HIT_SOUNDS);
            case 1:
                return this.pickSound(ORB_SOUNDS);
            case 2:
                return this.pickSound(BONK_SOUNDS);
            case 3:
                return this.pickSound(BOYKISSER_SOUNDS);
            case 4:
                return this.pickSound(BRING_SOUNDS);
            case 5:
                return this.pickSound(GLASS_SOUNDS);
            case 6:
                return this.pickSound(CLICK_SOUNDS);
            case 7:
                return this.pickSound(MEOW_SOUNDS);
            case 8:
                return this.pickSound(MOAN_SOUNDS);
            case 9:
                return this.pickSound(MAGIC_SQUASH_SOUNDS);
            case 10:
                return this.pickSound(NYA_SOUNDS);
            case 11:
                return this.pickSound(POP_SOUNDS);
            case 12:
                return this.pickSound(SOFT_SOUNDS);
            case 13:
                return this.pickSound(SQUASH_SOUNDS);
            case 14:
                return this.pickSound(TUNG_SOUNDS);
            case 15:
                return this.pickSound(UWU_SOUNDS);
            default:
                return this.pickSound(POP_SOUNDS);
        }
    }

    private String pickSound(String[] sounds) {
        return sounds[ThreadLocalRandom.current().nextInt(sounds.length)];
    }

    private void clearAttackPending() {
        this.lastTargetId = -1;
        this.lastAttackTime = 0L;
    }

    private void clearState() {
        this.clearAttackPending();
    }
}
