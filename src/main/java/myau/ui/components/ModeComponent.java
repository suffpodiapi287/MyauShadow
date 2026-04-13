package myau.ui.components;

import myau.property.properties.ModeProperty;
import myau.ui.BlackStyle;
import myau.ui.ClickGuiFont;

import java.util.concurrent.atomic.AtomicInteger;

public class ModeComponent extends BlackSettingComponent {
    private final ModeProperty property;

    public ModeComponent(ModeProperty property, ModuleComponent parentModule, int offsetY) {
        super(parentModule, offsetY);
        this.property = property;
    }

    @Override
    public void draw(AtomicInteger offset) {
        String value = BlackStyle.prettify(this.property.getModeString());
        String text = displayName(this.property.getName()) + ": " + value;
        requestWidth((int) Math.ceil(ClickGuiFont.getWidth(text)) + 8);
        ClickGuiFont.drawStringWithShadow(text, left(), this.y + 2.0F, BlackStyle.TEXT);
    }

    @Override
    public int getHeight() {
        return Math.max(12, (int) Math.ceil(ClickGuiFont.getHeight()) + 4);
    }

    @Override
    public void mouseDown(int x, int y, int button) {
        if (!isHovered(x, y)) {
            return;
        }

        if (button == 0) {
            this.property.nextMode();
        } else if (button == 1) {
            this.property.previousMode();
        }
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }
}
