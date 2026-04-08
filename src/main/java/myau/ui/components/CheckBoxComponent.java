package myau.ui.components;

import myau.property.properties.BooleanProperty;
import myau.ui.BlackStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import java.util.concurrent.atomic.AtomicInteger;

public class CheckBoxComponent extends BlackSettingComponent {
    private final BooleanProperty property;

    public CheckBoxComponent(BooleanProperty property, ModuleComponent parentModule, int offsetY) {
        super(parentModule, offsetY);
        this.property = property;
    }

    @Override
    public void draw(AtomicInteger offset) {
        Minecraft minecraft = Minecraft.getMinecraft();
        String text = displayName(this.property.getName());
        requestWidth(minecraft.fontRendererObj.getStringWidth(text) + 18);

        int textColor = this.property.getValue() ? BlackStyle.TEXT : BlackStyle.TEXT_DISABLED;
        minecraft.fontRendererObj.drawStringWithShadow(text, left(), this.y + 2, textColor);

        int indicatorLeft = right() - 8;
        Gui.drawRect(indicatorLeft, this.y + 2, indicatorLeft + 6, this.y + 8, this.property.getValue() ? BlackStyle.TEXT : BlackStyle.TEXT_DISABLED);
    }

    @Override
    public int getHeight() {
        return 12;
    }

    @Override
    public void mouseDown(int x, int y, int button) {
        if (this.isHovered(x, y) && button == 0 && this.parentModule.panelExpand) {
            this.property.setValue(!this.property.getValue());
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
