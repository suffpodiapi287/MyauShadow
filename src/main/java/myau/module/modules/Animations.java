package myau.module.modules;

import myau.config.AnimationConfig;
import myau.config.AnimationMode;
import myau.module.Module;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;

public class Animations extends Module {
    public final ModeProperty mode = new ModeProperty(
            "mode",
            0,
            new String[]{"VANILLA", "EXHIBITION", "ETB", "SIGMA", "DORTWARE", "PLAIN", "SPIN", "AVATAR", "SWONG", "SWANG", "SWANK", "STYLES", "NUDGE", "PUNCH", "JIGSAW", "SLIDE"}
    );
    public final IntProperty scale = new IntProperty("scale", 100, 50, 150);
    public final IntProperty swingSpeed = new IntProperty("swing-speed", 0, 0, 100);
    public final IntProperty handX = new IntProperty("hand-x", 0, -100, 100);
    public final IntProperty handY = new IntProperty("hand-y", 0, -100, 100);

    public Animations() {
        super("Animations", true);
    }

    @Override
    public void onEnabled() {
        this.syncConfig();
    }

    @Override
    public void onDisabled() {
        AnimationConfig.setEnabled(false);
    }

    private void syncConfig() {
        AnimationConfig.setEnabled(true);
        AnimationMode[] modes = AnimationMode.values();
        if (this.mode.getValue() >= 0 && this.mode.getValue() < modes.length) {
            AnimationConfig.setMode(modes[this.mode.getValue()]);
        }
        AnimationConfig.setScale(this.scale.getValue());
        AnimationConfig.setSwingSpeed(this.swingSpeed.getValue());
        AnimationConfig.setHandX(this.handX.getValue());
        AnimationConfig.setHandY(this.handY.getValue());
    }

    @Override
    public void verifyValue(String value) {
        this.syncConfig();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
