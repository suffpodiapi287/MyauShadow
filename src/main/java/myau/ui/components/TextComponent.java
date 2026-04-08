package myau.ui.components;

import myau.property.properties.TextProperty;
import myau.ui.BlackStyle;
import myau.ui.ClickGui;
import myau.ui.callback.GuiInput;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicInteger;

public class TextComponent extends BlackSettingComponent {
    private final TextProperty property;

    public TextComponent(TextProperty property, ModuleComponent parentModule, int offsetY) {
        super(parentModule, offsetY);
        this.property = property;
    }

    @Override
    public void draw(AtomicInteger offset) {
        Minecraft minecraft = Minecraft.getMinecraft();
        String text = displayName(this.property.getName()) + ": " + this.property.getValue();
        requestWidth(Math.max(128, minecraft.fontRendererObj.getStringWidth(text) + 8));
        minecraft.fontRendererObj.drawStringWithShadow(text, left(), this.y + 2, BlackStyle.TEXT);
    }

    @Override
    public int getHeight() {
        return 12;
    }

    @Override
    public void mouseDown(int x, int y, int button) {
        if (this.isHovered(x, y) && button == 0 && this.parentModule.panelExpand) {
            GuiInput.prompt(property.getName().replace("-", " "), property.getValue(), property::setValue, ClickGui.getInstance());
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
