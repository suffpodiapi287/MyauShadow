package myau.config;

import myau.Myau;
import myau.module.modules.Animations;

public class AnimationConfig {
    private static AnimationMode mode = AnimationMode.VANILLA;
    private static int scale = 100;
    private static int swingSpeed = 0;
    private static int handX = 0;
    private static int handY = 0;
    private static boolean enabled = false;

    public static void sync() {
        try {
            Animations animations = Myau.moduleManager == null ? null : (Animations) Myau.moduleManager.modules.get(Animations.class);
            if (animations != null && animations.isEnabled()) {
                enabled = true;
                AnimationMode[] modes = AnimationMode.values();
                if (animations.mode.getValue() >= 0 && animations.mode.getValue() < modes.length) {
                    mode = modes[animations.mode.getValue()];
                }
                scale = animations.scale.getValue();
                swingSpeed = animations.swingSpeed.getValue();
                handX = animations.handX.getValue();
                handY = animations.handY.getValue();
            } else {
                enabled = false;
            }
        } catch (Exception ignored) {
            enabled = false;
        }
    }

    public static AnimationMode getMode() {
        return mode;
    }

    public static void setMode(AnimationMode mode) {
        AnimationConfig.mode = mode;
    }

    public static int getScale() {
        return scale;
    }

    public static void setScale(int scale) {
        AnimationConfig.scale = scale;
    }

    public static int getSwingSpeed() {
        return swingSpeed;
    }

    public static void setSwingSpeed(int swingSpeed) {
        AnimationConfig.swingSpeed = swingSpeed;
    }

    public static int getHandX() {
        return handX;
    }

    public static void setHandX(int handX) {
        AnimationConfig.handX = handX;
    }

    public static int getHandY() {
        return handY;
    }

    public static void setHandY(int handY) {
        AnimationConfig.handY = handY;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        AnimationConfig.enabled = enabled;
    }
}
