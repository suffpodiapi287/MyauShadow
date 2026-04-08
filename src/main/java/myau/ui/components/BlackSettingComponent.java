package myau.ui.components;

import myau.ui.BlackStyle;
import myau.ui.Component;

public abstract class BlackSettingComponent implements Component {
    protected final ModuleComponent parentModule;
    protected int offsetY;
    protected int x;
    protected int y;
    protected int width;

    protected BlackSettingComponent(ModuleComponent parentModule, int offsetY) {
        this.parentModule = parentModule;
        this.offsetY = offsetY;
    }

    @Override
    public void update(int mousePosX, int mousePosY) {
        this.x = parentModule.getSettingsX();
        this.y = parentModule.getSettingsY() + offsetY;
        this.width = parentModule.getSettingsWidth();
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    protected String displayName(String rawName) {
        return BlackStyle.prettify(rawName);
    }

    protected void requestWidth(int contentWidth) {
        parentModule.requestSettingsWidth(contentWidth + 12);
    }

    protected int left() {
        return x + 6;
    }

    protected int right() {
        return x + width - 6;
    }

    protected int innerWidth() {
        return Math.max(8, width - 12);
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + getHeight();
    }
}
