package myau.ui.components;

import myau.module.modules.GuiModule;
import myau.ui.BlackStyle;
import myau.ui.ClickGuiFont;
import myau.ui.dataset.BindStage;
import myau.util.KeyBindUtil;

import java.util.concurrent.atomic.AtomicInteger;

public class BindComponent extends BlackSettingComponent {
    private boolean isBinding;

    public BindComponent(ModuleComponent parentModule, int offsetY) {
        super(parentModule, offsetY);
    }

    @Override
    public void draw(AtomicInteger offset) {
        String displayText = this.isBinding ? BindStage.binding : BindStage.bind + ": " + KeyBindUtil.getKeyName(this.parentModule.mod.getKey());
        requestWidth((int) Math.ceil(ClickGuiFont.getWidth(displayText)) + 8);
        renderText(displayText, this.isBinding ? BlackStyle.TEXT : BlackStyle.TEXT_MUTED);
    }

    @Override
    public void mouseDown(int x, int y, int button) {
        if (this.isHovered(x, y) && button == 0 && this.parentModule.panelExpand) {
            this.isBinding = !this.isBinding;
        } else if (this.isBinding && this.parentModule.panelExpand) {
            int keyIndex = button - 100;

            if (button == 0) {
                this.isBinding = false;
                return;
            }

            this.parentModule.mod.setKey(keyIndex);
            this.isBinding = false;
        }
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
    }

    @Override
    public void keyTyped(char chatTyped, int keyCode) {
        if (!this.isBinding) {
            return;
        }

        if (keyCode == 1) {
            clearBind();
            this.isBinding = false;
            return;
        }

        if (keyCode == 11) {
            clearBind();
        } else {
            this.parentModule.mod.setKey(keyCode);
        }

        this.isBinding = false;
    }

    @Override
    public int getHeight() {
        return Math.max(12, (int) Math.ceil(ClickGuiFont.getHeight()) + 4);
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    private void renderText(String text, int color) {
        ClickGuiFont.drawStringWithShadow(text, left(), this.y + 2.0F, color);
    }

    private void clearBind() {
        if (this.parentModule.mod instanceof GuiModule) {
            this.parentModule.mod.setKey(54);
        } else {
            this.parentModule.mod.setKey(0);
        }
    }

    public boolean isBindingActive() {
        return this.isBinding;
    }
}
