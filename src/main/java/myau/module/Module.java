package myau.module;

import myau.Myau;
import myau.module.modules.HUD;
import myau.property.Property;
import myau.property.properties.BooleanProperty;
import myau.util.KeyBindUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    protected final String name;
    protected final boolean defaultEnabled;
    protected final int defaultKey;
    protected final boolean defaultHidden;
    private final BooleanProperty hideProperty;
    protected boolean enabled;
    protected int key;
    protected boolean hidden;

    public Module(String name, boolean enabled) {
        this(name, enabled, false);
    }

    public Module(String name, boolean enabled, boolean hidden) {
        this.name = name;
        this.enabled = this.defaultEnabled = enabled;
        this.key = this.defaultKey = 0;
        this.hidden = this.defaultHidden = hidden;
        this.hideProperty = new BooleanProperty("hide", hidden) {
            @Override
            public boolean setValue(Object object) {
                boolean changed = super.setValue(object);
                if (changed) {
                    Module.this.hidden = this.getValue();
                }
                return changed;
            }
        };
    }

    public String getName() {
        return this.name;
    }

    public String formatModule() {
        return String.format(
                "%s%s &r(%s&r)",
                this.key == 0 ? "" : String.format("&l[%s] &r", KeyBindUtil.getKeyName(this.key)),
                this.name,
                this.enabled ? "&a&lON" : "&c&lOFF"
        );
    }

    public String[] getSuffix() {
        return new String[0];
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                this.onEnabled();
            } else {
                this.onDisabled();
            }
        }
    }

    public boolean toggle() {
        boolean enabled = !this.enabled;
        this.setEnabled(enabled);
        if (this.enabled == enabled) {
            if (((HUD) Myau.moduleManager.modules.get(HUD.class)).toggleSound.getValue()) {
                Myau.moduleManager.playSound();
            }
            return true;
        } else {
            return false;
        }
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int integer) {
        this.key = integer;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(boolean boolean1) {
        this.hidden = boolean1;
        if (this.hideProperty.getValue() != boolean1) {
            this.hideProperty.setValue(boolean1);
        }
    }

    public List<Property<?>> getBaseProperties() {
        ArrayList<Property<?>> properties = new ArrayList<>();
        properties.add(this.hideProperty);
        return Collections.unmodifiableList(properties);
    }

    public void onEnabled() {
    }

    public void onDisabled() {
    }

    public void verifyValue(String string) {
    }
}
