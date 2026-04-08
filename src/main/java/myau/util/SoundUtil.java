package myau.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SoundUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Map<String, String> SOUND_ALIASES;

    static {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("myau:magic_squash", "myau:magicsquash");
        SOUND_ALIASES = Collections.unmodifiableMap(aliases);
    }

    public static void playSound(String soundName) {
        if (soundName == null || soundName.isEmpty()) {
            return;
        }

        Runnable playTask = () -> {
            SoundHandler soundHandler = mc.getSoundHandler();
            if (soundHandler == null) {
                return;
            }

            String resolvedSoundName = resolveSoundName(soundHandler, soundName);
            if (resolvedSoundName == null || resolvedSoundName.isEmpty()) {
                return;
            }

            PositionedSoundRecord positionedSoundRecord = PositionedSoundRecord.create(new ResourceLocation(resolvedSoundName));
            soundHandler.playSound(positionedSoundRecord);
        };

        if (mc.isCallingFromMinecraftThread()) {
            playTask.run();
        } else {
            mc.addScheduledTask(playTask);
        }
    }

    public static String normalizeSoundName(String soundName) {
        if (soundName == null || soundName.isEmpty()) {
            return soundName;
        }

        SoundHandler soundHandler = mc.getSoundHandler();
        if (soundHandler == null) {
            return applyAlias(soundName);
        }

        return resolveSoundName(soundHandler, soundName);
    }

    private static String resolveSoundName(SoundHandler soundHandler, String soundName) {
        String resolvedName = applyAlias(soundName);
        if (hasSoundEvent(soundHandler, resolvedName)) {
            return resolvedName;
        }

        soundHandler.onResourceManagerReload(mc.getResourceManager());
        if (hasSoundEvent(soundHandler, resolvedName)) {
            return resolvedName;
        }

        String fallbackName = SOUND_ALIASES.get(soundName);
        if (fallbackName != null && hasSoundEvent(soundHandler, fallbackName)) {
            return fallbackName;
        }

        return resolvedName;
    }

    private static boolean hasSoundEvent(SoundHandler soundHandler, String soundName) {
        SoundEventAccessorComposite soundEvent = soundHandler.getSound(new ResourceLocation(soundName));
        return soundEvent != null;
    }

    private static String applyAlias(String soundName) {
        String alias = SOUND_ALIASES.get(soundName);
        return alias != null ? alias : soundName;
    }
}
