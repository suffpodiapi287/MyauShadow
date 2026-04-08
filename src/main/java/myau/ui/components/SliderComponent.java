package myau.ui.components;

import myau.ui.BlackStyle;
import myau.ui.ClickGui;
import myau.ui.callback.GuiInput;
import myau.ui.dataset.Slider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;

public class SliderComponent extends BlackSettingComponent {
    private final Slider slider;
    private boolean dragging;

    public SliderComponent(Slider slider, ModuleComponent parentModule, int offsetY) {
        super(parentModule, offsetY);
        this.slider = slider;
    }

    @Override
    public void draw(AtomicInteger offset) {
        Minecraft minecraft = Minecraft.getMinecraft();
        String text = displayName(this.slider.getName()) + ": " + this.slider.getValueString();
        requestWidth(Math.max(128, minecraft.fontRendererObj.getStringWidth(text) + 8));

        int trackLeft = left();
        int trackRight = right();
        int trackY = this.y + 14;
        int trackWidth = Math.max(1, trackRight - trackLeft);
        float percentage = (float) ((this.slider.getInput() - this.slider.getMin()) / Math.max(0.0001D, this.slider.getMax() - this.slider.getMin()));
        int sliderEnd = trackLeft + Math.round(trackWidth * percentage);

        minecraft.fontRendererObj.drawStringWithShadow(text, trackLeft, this.y + 2, BlackStyle.TEXT);
        Gui.drawRect(trackLeft, trackY, trackRight, trackY + 2, BlackStyle.TRACK);
        Gui.drawRect(trackLeft, trackY, sliderEnd, trackY + 2, BlackStyle.TRACK_FILL);
        BlackStyle.drawCircle(sliderEnd, trackY + 1, 3.0F, BlackStyle.TEXT);
    }

    @Override
    public int getHeight() {
        return 20;
    }

    @Override
    public void update(int mousePosX, int mousePosY) {
        super.update(mousePosX, mousePosY);

        double trackWidth = Math.max(1, right() - left());
        double clampedMouse = Math.min(trackWidth, Math.max(0, mousePosX - left()));

        if (this.dragging) {
            if (clampedMouse == 0.0D) {
                this.slider.setValue(this.slider.getMin());
            } else {
                double rawValue = clampedMouse / trackWidth * (this.slider.getMax() - this.slider.getMin()) + this.slider.getMin();
                double increment = this.slider.getIncrement();
                if (increment > 0) {
                    rawValue = Math.round(rawValue / increment) * increment;
                }
                double value = roundToPrecision(rawValue, 2);
                value = Math.max(this.slider.getMin(), Math.min(this.slider.getMax(), value));
                this.slider.setValue(value);
            }
        }
    }

    private static double roundToPrecision(double value, int precision) {
        if (precision < 0) {
            return 0.0D;
        }

        BigDecimal decimal = new BigDecimal(value);
        decimal = decimal.setScale(precision, RoundingMode.HALF_UP);
        return decimal.doubleValue();
    }

    @Override
    public void mouseDown(int x, int y, int button) {
        if (this.isTextHovered(x, y) && button == 0 && this.parentModule.panelExpand) {
            GuiInput.prompt(slider.getName().replace("-", " "), slider.getValueString(), slider::setValueString, ClickGui.getInstance());
            return;
        }

        if (this.isTrackHovered(x, y) && this.parentModule.panelExpand) {
            if (button == 0) {
                this.dragging = true;
                update(x, y);
            } else if (button == 1) {
                if (x <= left() + innerWidth() / 2) {
                    this.slider.stepping(false);
                } else {
                    this.slider.stepping(true);
                }
            }
        }
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
        this.dragging = false;
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
    }

    public boolean isTextHovered(int x, int y) {
        return x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + 10;
    }

    public boolean isTrackHovered(int x, int y) {
        return x >= left() && x <= right() && y >= this.y + 11 && y <= this.y + 17;
    }

    @Override
    public boolean isVisible() {
        return slider.isVisible();
    }
}
