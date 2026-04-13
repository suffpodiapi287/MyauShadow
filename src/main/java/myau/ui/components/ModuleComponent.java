package myau.ui.components;

import myau.Myau;
import myau.module.Module;
import myau.property.Property;
import myau.property.properties.*;
import myau.ui.BlackStyle;
import myau.ui.ClickGuiFont;
import myau.ui.Component;
import myau.ui.dataset.impl.FloatSlider;
import myau.ui.dataset.impl.IntSlider;
import myau.ui.dataset.impl.PercentageSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ModuleComponent implements Component {
    private static final int ROW_HEIGHT = 15;
    private static final int SETTINGS_WIDTH_MIN = 114;
    private static final int SETTINGS_WIDTH_MAX = 204;
    private static final int SETTINGS_PANEL_GAP = 4;
    private static final int SCREEN_PADDING = 4;
    private static final AtomicInteger UNUSED_OFFSET = new AtomicInteger();

    public final Module mod;
    public final CategoryComponent category;
    public int offsetY;
    private final ArrayList<Component> settings;
    public boolean panelExpand;
    private boolean visibleInList;
    private int hoverFade;
    private int enabledFade;
    private int settingsWidth = SETTINGS_WIDTH_MIN;
    private int measuredSettingsWidth = SETTINGS_WIDTH_MIN;

    public ModuleComponent(Module mod, CategoryComponent category, int offsetY) {
        this.mod = mod;
        this.category = category;
        this.offsetY = offsetY;
        this.settings = new ArrayList<>();
        this.panelExpand = false;
        int y = offsetY + 12;

        if (!Myau.propertyManager.properties.get(mod.getClass()).isEmpty()) {
            for (Property<?> baseProperty : Myau.propertyManager.properties.get(mod.getClass())) {
                if (baseProperty instanceof BooleanProperty) {
                    CheckBoxComponent component = new CheckBoxComponent((BooleanProperty) baseProperty, this, y);
                    this.settings.add(component);
                    y += component.getHeight();
                } else if (baseProperty instanceof FloatProperty) {
                    SliderComponent component = new SliderComponent(new FloatSlider((FloatProperty) baseProperty), this, y);
                    this.settings.add(component);
                    y += component.getHeight();
                } else if (baseProperty instanceof IntProperty) {
                    SliderComponent component = new SliderComponent(new IntSlider((IntProperty) baseProperty), this, y);
                    this.settings.add(component);
                    y += component.getHeight();
                } else if (baseProperty instanceof PercentProperty) {
                    SliderComponent component = new SliderComponent(new PercentageSlider((PercentProperty) baseProperty), this, y);
                    this.settings.add(component);
                    y += component.getHeight();
                } else if (baseProperty instanceof ModeProperty) {
                    ModeComponent component = new ModeComponent((ModeProperty) baseProperty, this, y);
                    this.settings.add(component);
                    y += component.getHeight();
                } else if (baseProperty instanceof ColorProperty) {
                    ColorSliderComponent component = new ColorSliderComponent((ColorProperty) baseProperty, this, y);
                    this.settings.add(component);
                    y += component.getHeight();
                } else if (baseProperty instanceof TextProperty) {
                    TextComponent component = new TextComponent((TextProperty) baseProperty, this, y);
                    this.settings.add(component);
                    y += component.getHeight();
                }
            }
        }

        this.settings.add(new BindComponent(this, y));
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
        int y = 6;

        for (Component component : this.settings) {
            component.setComponentStartAt(y);
            if (component.isVisible()) {
                y += component.getHeight();
            }
        }
    }

    @Override
    public void draw(AtomicInteger offset) {
        if (!this.visibleInList) {
            return;
        }

        int x = this.category.getX();
        int y = this.category.getY() + this.offsetY;
        int width = this.category.getWidth();
        int baseColor = BlackStyle.blend(BlackStyle.MODULE, BlackStyle.MODULE_HOVER, this.hoverFade / 100.0F);

        BlackStyle.drawBorderedRect(x, y, x + width, y + ROW_HEIGHT, BlackStyle.BORDER, baseColor);
        if (this.enabledFade > 0) {
            Gui.drawRect(x, y, x + width, y + ROW_HEIGHT, new Color(20, 20, 20, this.enabledFade).getRGB());
        }

        int textColor = this.mod.isEnabled() ? BlackStyle.TEXT : BlackStyle.TEXT_MUTED;
        ClickGuiFont.drawStringWithShadow(this.mod.getName(), x + 6.0F, y + (ROW_HEIGHT - ClickGuiFont.getHeight()) / 2.0F, textColor);

        if (!this.settings.isEmpty()) {
            String arrow = this.panelExpand ? "<" : ">";
            ClickGuiFont.drawStringWithShadow(arrow, x + width - 6.0F - ClickGuiFont.getWidth(arrow), y + (ROW_HEIGHT - ClickGuiFont.getHeight()) / 2.0F, BlackStyle.TEXT);
        }
    }

    @Override
    public int getHeight() {
        return ROW_HEIGHT;
    }

    @Override
    public void update(int mousePosX, int mousePosY) {
        this.hoverFade = BlackStyle.clamp(this.hoverFade + (this.isHovered(mousePosX, mousePosY) ? 18 : -18), 0, 100);
        this.enabledFade = BlackStyle.clamp(this.enabledFade + (this.mod.isEnabled() ? 18 : -18), 0, 185);

        if (!this.panelExpand) {
            return;
        }

        for (Component component : this.settings) {
            if (component.isVisible()) {
                component.update(mousePosX, mousePosY);
            }
        }
    }

    @Override
    public void mouseDown(int x, int y, int button) {
        this.handleClick(x, y, button);
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
        for (Component component : this.settings) {
            if (component.isVisible()) {
                component.mouseReleased(x, y, button);
            }
        }
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
        if (!this.panelExpand) {
            return;
        }

        for (Component component : this.settings) {
            if (component.isVisible()) {
                component.keyTyped(chatTyped, keyCode);
            }
        }
    }

    public boolean isHovered(int x, int y) {
        return x >= this.category.getX() && x <= this.category.getX() + this.category.getWidth()
                && y >= this.category.getY() + this.offsetY && y <= this.category.getY() + this.offsetY + ROW_HEIGHT;
    }

    public boolean handleClick(int mouseX, int mouseY, int button) {
        if (isSettingsVisible()) {
            int settingsX = getSettingsX();
            int settingsY = getSettingsY();
            int settingsHeight = getSettingsHeight();

            if (mouseX >= settingsX && mouseX <= settingsX + this.settingsWidth
                    && mouseY >= settingsY && mouseY <= settingsY + settingsHeight) {
                for (Component component : this.settings) {
                    if (component.isVisible() && component instanceof BlackSettingComponent) {
                        BlackSettingComponent settingComponent = (BlackSettingComponent) component;
                        if (settingComponent.isHovered(mouseX, mouseY)) {
                            settingComponent.mouseDown(mouseX, mouseY, button);
                            return true;
                        }
                    }
                }
                return true;
            }
        }

        if (!this.visibleInList || !this.isHovered(mouseX, mouseY)) {
            return false;
        }

        if (button == 0) {
            this.mod.toggle();
            return true;
        }

        if (button == 1 && !this.settings.isEmpty()) {
            this.panelExpand = !this.panelExpand;
            return true;
        }

        return false;
    }

    public void drawSettings() {
        if (!isSettingsVisible()) {
            return;
        }

        this.measuredSettingsWidth = SETTINGS_WIDTH_MIN;

        int panelX = getSettingsX();
        int panelY = getSettingsY();
        int panelHeight = getSettingsHeight();
        BlackStyle.drawBorderedRect(panelX, panelY, panelX + this.settingsWidth, panelY + panelHeight, BlackStyle.BORDER, BlackStyle.BODY);

        for (Component component : this.settings) {
            if (component.isVisible()) {
                component.draw(UNUSED_OFFSET);
            }
        }

        this.settingsWidth = BlackStyle.clamp(this.measuredSettingsWidth, SETTINGS_WIDTH_MIN, SETTINGS_WIDTH_MAX);
    }

    public int getSettingsX() {
        return this.category.getX() + this.category.getWidth() + SETTINGS_PANEL_GAP;
    }

    public int getSettingsY() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        int desired = this.category.getY() + this.offsetY;
        int maxY = resolution.getScaledHeight() - getSettingsHeight() - SCREEN_PADDING;
        return Math.max(SCREEN_PADDING, Math.min(desired, maxY));
    }

    public int getSettingsWidth() {
        return this.settingsWidth;
    }

    public void requestSettingsWidth(int requestedWidth) {
        this.measuredSettingsWidth = Math.max(this.measuredSettingsWidth, requestedWidth);
    }

    public int getSettingsHeight() {
        int height = 6;
        for (Component component : this.settings) {
            if (component.isVisible()) {
                height += component.getHeight();
            }
        }
        return Math.max(12, height);
    }

    public boolean isSettingsVisible() {
        return this.panelExpand && hasVisibleSettings() && this.visibleInList;
    }

    public boolean isVisibleInList() {
        return this.visibleInList;
    }

    public void setVisibleInList(boolean visibleInList) {
        this.visibleInList = visibleInList;
    }

    private boolean hasVisibleSettings() {
        for (Component component : this.settings) {
            if (component.isVisible()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    public int getReservedRightSpace() {
        if (!isSettingsVisible()) {
            return SCREEN_PADDING;
        }

        return SETTINGS_PANEL_GAP + this.settingsWidth + SCREEN_PADDING;
    }

    public boolean isBindingActive() {
        for (Component component : this.settings) {
            if (component instanceof BindComponent && ((BindComponent) component).isBindingActive()) {
                return true;
            }
        }

        return false;
    }
}
