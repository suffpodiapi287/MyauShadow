package myau.ui.components;

import myau.module.Module;
import myau.ui.BlackStyle;
import myau.ui.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CategoryComponent {
    private static final int MAX_HEIGHT = 240;
    private static final int HEADER_HEIGHT = 18;
    private static final int SCREEN_PADDING = 4;

    private final ArrayList<Component> modulesInCategory = new ArrayList<>();
    private final String categoryName;
    private boolean categoryOpened;
    private int width;
    private int y;
    private int x;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private int scroll;
    private double animScroll;
    private int height;

    public CategoryComponent(String category, List<Module> modules) {
        this.categoryName = category;
        this.width = 100;
        this.x = 96;
        this.y = 20;
        this.categoryOpened = false;

        int tY = HEADER_HEIGHT + 3;
        for (Module mod : modules) {
            ModuleComponent component = new ModuleComponent(mod, this, tY);
            this.modulesInCategory.add(component);
            tY += component.getHeight() + 1;
        }
    }

    public ArrayList<Component> getModules() {
        return modulesInCategory;
    }

    public void update(int mouseX, int mouseY) {
        this.width = 100;
        this.height = 0;
        for (Component component : modulesInCategory) {
            this.height += component.getHeight() + 1;
        }
        if (this.height > 0) {
            this.height -= 1;
        }

        int maxScroll = Math.max(0, this.height - MAX_HEIGHT);
        this.scroll = BlackStyle.clamp(this.scroll, 0, maxScroll);
        this.animScroll += (this.scroll - this.animScroll) * 0.25D;
        if (Math.abs(this.animScroll - this.scroll) < 0.35D) {
            this.animScroll = this.scroll;
        }

        clampToScreen();

        int renderHeight = 0;
        for (Component component : modulesInCategory) {
            int componentHeight = component.getHeight();
            int drawY = (int) Math.round(renderHeight - this.animScroll);
            component.setComponentStartAt(HEADER_HEIGHT + 3 + drawY);

            boolean visible = renderHeight + componentHeight >= this.animScroll - 3 && renderHeight <= this.animScroll + MAX_HEIGHT + 3;
            if (component instanceof ModuleComponent) {
                ((ModuleComponent) component).setVisibleInList(visible);
            }

            if (visible || component instanceof ModuleComponent && ((ModuleComponent) component).isSettingsVisible()) {
                component.update(mouseX, mouseY);
            }

            renderHeight += componentHeight + 1;
        }
    }

    public void render() {
        BlackStyle.drawPanelHeader(this.x, this.y, this.width, HEADER_HEIGHT, this.categoryName);

        if (!this.categoryOpened || this.modulesInCategory.isEmpty()) {
            return;
        }

        int displayHeight = Math.min(this.height, MAX_HEIGHT);
        BlackStyle.drawBorderedRect(this.x, this.y + HEADER_HEIGHT - 1, this.x + this.width, this.y + HEADER_HEIGHT + displayHeight + 5, BlackStyle.BORDER, BlackStyle.BODY);
        Gui.drawRect(this.x, this.y + HEADER_HEIGHT + displayHeight + 4, this.x + this.width, this.y + HEADER_HEIGHT + displayHeight + 5, BlackStyle.HEADER);

        if (displayHeight > 0) {
            ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
            double scale = resolution.getScaleFactor();
            int scissorTop = this.y + HEADER_HEIGHT;
            int scissorBottom = scissorTop + displayHeight + 4;

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(
                    (int) (this.x * scale),
                    (int) ((resolution.getScaledHeight() - scissorBottom) * scale),
                    (int) (this.width * scale),
                    (int) ((displayHeight + 4) * scale)
            );

            AtomicInteger offset = new AtomicInteger();
            for (Component component : modulesInCategory) {
                if (component instanceof ModuleComponent) {
                    ModuleComponent moduleComponent = (ModuleComponent) component;
                    if (moduleComponent.isVisibleInList()) {
                        moduleComponent.draw(offset);
                        offset.incrementAndGet();
                    }
                }
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        for (Component component : modulesInCategory) {
            if (component instanceof ModuleComponent) {
                ((ModuleComponent) component).drawSettings();
            }
        }

        if (this.height > MAX_HEIGHT) {
            float barHeight = (float) MAX_HEIGHT * MAX_HEIGHT / this.height;
            float barTravel = MAX_HEIGHT - barHeight;
            float barY = (float) this.y + HEADER_HEIGHT + 2 + (float) (this.animScroll / (this.height - MAX_HEIGHT)) * barTravel;
            Gui.drawRect(this.x + this.width - 3, (int) barY, this.x + this.width - 1, (int) (barY + barHeight), new Color(255, 255, 255, 80).getRGB());
        }
    }

    public boolean handleClick(int mouseX, int mouseY, int mouseButton) {
        if (this.categoryOpened) {
            for (int i = this.modulesInCategory.size() - 1; i >= 0; i--) {
                Component component = this.modulesInCategory.get(i);
                if (component instanceof ModuleComponent && ((ModuleComponent) component).handleClick(mouseX, mouseY, mouseButton)) {
                    return true;
                }
            }
        }

        if (!isHeaderHovered(mouseX, mouseY)) {
            return false;
        }

        if (mouseButton == 0) {
            this.dragging = true;
            this.dragOffsetX = mouseX - this.x;
            this.dragOffsetY = mouseY - this.y;
            return true;
        }

        if (mouseButton == 1) {
            this.categoryOpened = !this.categoryOpened;
            return true;
        }

        return false;
    }

    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        this.dragging = false;

        for (Component component : this.modulesInCategory) {
            component.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (!this.categoryOpened) {
            return;
        }

        for (Component component : this.modulesInCategory) {
            component.keyTyped(typedChar, keyCode);
        }
    }

    public void handleDrag(int mouseX, int mouseY) {
        if (!this.dragging) {
            return;
        }

        this.setX(mouseX - this.dragOffsetX);
        this.setY(mouseY - this.dragOffsetY);
    }

    public void onScroll(int mouseX, int mouseY, int scrollAmount) {
        if (!this.categoryOpened || this.height <= MAX_HEIGHT) {
            return;
        }

        int areaTop = this.y + HEADER_HEIGHT;
        int areaBottom = areaTop + MAX_HEIGHT;
        if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= areaTop && mouseY <= areaBottom) {
            this.scroll = BlackStyle.clamp(this.scroll - scrollAmount * 14, 0, this.height - MAX_HEIGHT);
        }
    }

    public boolean isHeaderHovered(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + HEADER_HEIGHT;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isOpened() {
        return this.categoryOpened;
    }

    public void setOpened(boolean open) {
        this.categoryOpened = open;
    }

    public String getName() {
        return categoryName;
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void clampToScreen() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        int reservedRightSpace = SCREEN_PADDING;
        for (Component component : this.modulesInCategory) {
            if (component instanceof ModuleComponent) {
                reservedRightSpace = Math.max(reservedRightSpace, ((ModuleComponent) component).getReservedRightSpace());
            }
        }

        int maxX = resolution.getScaledWidth() - this.width - reservedRightSpace;
        this.x = BlackStyle.clamp(this.x, SCREEN_PADDING, Math.max(SCREEN_PADDING, maxX));

        int maxY = resolution.getScaledHeight() - getRenderedHeight() - SCREEN_PADDING;
        this.y = BlackStyle.clamp(this.y, SCREEN_PADDING, Math.max(SCREEN_PADDING, maxY));
    }

    private int getRenderedHeight() {
        if (!this.categoryOpened || this.modulesInCategory.isEmpty()) {
            return HEADER_HEIGHT;
        }

        return HEADER_HEIGHT + Math.min(this.height, MAX_HEIGHT) + 5;
    }

    public boolean isBindingActive() {
        for (Component component : this.modulesInCategory) {
            if (component instanceof ModuleComponent && ((ModuleComponent) component).isBindingActive()) {
                return true;
            }
        }

        return false;
    }
}
