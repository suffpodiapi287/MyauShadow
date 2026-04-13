
package myau.ui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import myau.Myau;
import myau.module.Module;
import myau.module.modules.*;
import myau.ui.BlackStyle;
import myau.ui.components.CategoryComponent;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ClickGui extends GuiScreen {
    private static ClickGui instance;
    private final File configFile = new File("./config/Myau/", "clickgui.txt");
    private final ArrayList<CategoryComponent> categoryList;

    public ClickGui() {
        instance = this;

        List<Module> combatModules = new ArrayList<>();
        combatModules.add(Myau.moduleManager.getModule(AimAssist.class));
        combatModules.add(Myau.moduleManager.getModule(AutoClicker.class));
        combatModules.add(Myau.moduleManager.getModule(BackTrack.class));
        combatModules.add(Myau.moduleManager.getModule(KillAura.class));
        combatModules.add(Myau.moduleManager.getModule(FakeLag.class));
        combatModules.add(Myau.moduleManager.getModule(ForwardTrack.class));
        combatModules.add(Myau.moduleManager.getModule(Wtap.class));
        combatModules.add(Myau.moduleManager.getModule(Velocity.class));
        combatModules.add(Myau.moduleManager.getModule(Freeze.class));
        combatModules.add(Myau.moduleManager.getModule(Reach.class));
        combatModules.add(Myau.moduleManager.getModule(TargetStrafe.class));
        combatModules.add(Myau.moduleManager.getModule(NoHitDelay.class));
        combatModules.add(Myau.moduleManager.getModule(AntiFireball.class));
        combatModules.add(Myau.moduleManager.getModule(LagRange.class));
        combatModules.add(Myau.moduleManager.getModule(HitBox.class));
        combatModules.add(Myau.moduleManager.getModule(MoreKB.class));
        combatModules.add(Myau.moduleManager.getModule(NewTimerRange.class));
        combatModules.add(Myau.moduleManager.getModule(Refill.class));
        combatModules.add(Myau.moduleManager.getModule(HitSelect.class));

        List<Module> movementModules = new ArrayList<>();
        movementModules.add(Myau.moduleManager.getModule(AntiAFK.class));
        movementModules.add(Myau.moduleManager.getModule(Fly.class));
        movementModules.add(Myau.moduleManager.getModule(Speed.class));
        movementModules.add(Myau.moduleManager.getModule(LongJump.class));
        movementModules.add(Myau.moduleManager.getModule(Sprint.class));
        movementModules.add(Myau.moduleManager.getModule(SafeWalk.class));
        movementModules.add(Myau.moduleManager.getModule(Jesus.class));
        movementModules.add(Myau.moduleManager.getModule(Blink.class));
        movementModules.add(Myau.moduleManager.getModule(NoFall.class));
        movementModules.add(Myau.moduleManager.getModule(NoSlow.class));
        movementModules.add(Myau.moduleManager.getModule(KeepSprint.class));
        movementModules.add(Myau.moduleManager.getModule(Eagle.class));
        movementModules.add(Myau.moduleManager.getModule(NoJumpDelay.class));
        movementModules.add(Myau.moduleManager.getModule(AntiVoid.class));

        List<Module> renderModules = new ArrayList<>();
        renderModules.add(Myau.moduleManager.getModule(Animations.class));
        renderModules.add(Myau.moduleManager.getModule(ESP.class));
        renderModules.add(Myau.moduleManager.getModule(Chams.class));
        renderModules.add(Myau.moduleManager.getModule(FullBright.class));
        renderModules.add(Myau.moduleManager.getModule(Tracers.class));
        renderModules.add(Myau.moduleManager.getModule(NameTags.class));
        renderModules.add(Myau.moduleManager.getModule(Xray.class));
        renderModules.add(Myau.moduleManager.getModule(TargetHUD.class));
        renderModules.add(Myau.moduleManager.getModule(Indicators.class));
        renderModules.add(Myau.moduleManager.getModule(HitFX.class));
        renderModules.add(Myau.moduleManager.getModule(BedESP.class));
        renderModules.add(Myau.moduleManager.getModule(ItemESP.class));
        renderModules.add(Myau.moduleManager.getModule(WaterMark.class));
        renderModules.add(Myau.moduleManager.getModule(ViewClip.class));
        renderModules.add(Myau.moduleManager.getModule(NoHurtCam.class));
        renderModules.add(Myau.moduleManager.getModule(HUD.class));
        renderModules.add(Myau.moduleManager.getModule(GuiModule.class));
        renderModules.add(Myau.moduleManager.getModule(ChestESP.class));
        renderModules.add(Myau.moduleManager.getModule(Trajectories.class));
        renderModules.add(Myau.moduleManager.getModule(Radar.class));

        List<Module> playerModules = new ArrayList<>();
        playerModules.add(Myau.moduleManager.getModule(AutoHeal.class));
        playerModules.add(Myau.moduleManager.getModule(AutoTool.class));
        playerModules.add(Myau.moduleManager.getModule(ChestStealer.class));
        playerModules.add(Myau.moduleManager.getModule(InvManager.class));
        playerModules.add(Myau.moduleManager.getModule(InvWalk.class));
        playerModules.add(Myau.moduleManager.getModule(Scaffold.class));
        playerModules.add(Myau.moduleManager.getModule(AutoBlockIn.class));
        playerModules.add(Myau.moduleManager.getModule(SpeedMine.class));
        playerModules.add(Myau.moduleManager.getModule(FastPlace.class));
        playerModules.add(Myau.moduleManager.getModule(GhostHand.class));
        playerModules.add(Myau.moduleManager.getModule(MCF.class));
        playerModules.add(Myau.moduleManager.getModule(AntiDebuff.class));

        List<Module> miscModules = new ArrayList<>();
        miscModules.add(Myau.moduleManager.getModule(Spammer.class));
        miscModules.add(Myau.moduleManager.getModule(BedNuker.class));
        miscModules.add(Myau.moduleManager.getModule(BedTracker.class));
        miscModules.add(Myau.moduleManager.getModule(AntiBot.class));
        miscModules.add(Myau.moduleManager.getModule(Disabler.class));
        miscModules.add(Myau.moduleManager.getModule(LightningTracker.class));
        miscModules.add(Myau.moduleManager.getModule(NoRotate.class));
        miscModules.add(Myau.moduleManager.getModule(NickHider.class));
        miscModules.add(Myau.moduleManager.getModule(AntiObbyTrap.class));
        miscModules.add(Myau.moduleManager.getModule(AntiObfuscate.class));
        miscModules.add(Myau.moduleManager.getModule(AutoAnduril.class));
        miscModules.add(Myau.moduleManager.getModule(AutoLogin.class));
        miscModules.add(Myau.moduleManager.getModule(InventoryClicker.class));

        List<Module> alertModules = new ArrayList<>();
        alertModules.add(Myau.moduleManager.getModule(CriticalCheck.class));
        alertModules.add(Myau.moduleManager.getModule(FlagDetector.class));
        alertModules.add(Myau.moduleManager.getModule(HealthDebug.class));

        Comparator<Module> comparator = Comparator.comparing(m -> m.getName().toLowerCase());
        combatModules.sort(comparator);
        movementModules.sort(comparator);
        renderModules.sort(comparator);
        playerModules.sort(comparator);
        miscModules.sort(comparator);
        alertModules.sort(comparator);

        Set<Module> registered = new HashSet<>();
        registered.addAll(combatModules);
        registered.addAll(movementModules);
        registered.addAll(renderModules);
        registered.addAll(playerModules);
        registered.addAll(miscModules);
        registered.addAll(alertModules);

        for (Module module : Myau.moduleManager.modules.values()) {
            if (!registered.contains(module)) {
                throw new RuntimeException(module.getClass().getName() + " is unregistered to click gui.");
            }
        }

        this.categoryList = new ArrayList<>();
        int topOffset = 20;


        CategoryComponent combat = new CategoryComponent("Combat", combatModules);
        combat.setX(96);
        combat.setY(topOffset);
        categoryList.add(combat);
        topOffset += 22;

        CategoryComponent movement = new CategoryComponent("Movement", movementModules);
        movement.setX(96);
        movement.setY(topOffset);
        categoryList.add(movement);
        topOffset += 22;

        CategoryComponent render = new CategoryComponent("Render", renderModules);
        render.setX(96);
        render.setY(topOffset);
        categoryList.add(render);
        topOffset += 22;

        CategoryComponent player = new CategoryComponent("Player", playerModules);
        player.setX(96);
        player.setY(topOffset);
        categoryList.add(player);
        topOffset += 22;

        CategoryComponent misc = new CategoryComponent("Misc", miscModules);
        misc.setX(96);
        misc.setY(topOffset);
        categoryList.add(misc);
        topOffset += 22;

        CategoryComponent alerts = new CategoryComponent("Alerts", alertModules);
        alerts.setX(96);
        alerts.setY(topOffset);
        categoryList.add(alerts);

        loadPositions();
    }

    public static ClickGui getInstance() {
        return instance;
    }

    public void initGui() {
        super.initGui();
    }

    public void drawScreen(int x, int y, float p) {
        drawDefaultBackground();
        drawRect(0, 0, this.width, this.height, BlackStyle.BACKDROP);

        ClickGuiFont.drawStringWithShadow("MyauShadow " + Myau.version, 8.0F, this.height - 20.0F, new Color(225, 225, 225).getRGB());
        ClickGuiFont.drawStringWithShadow("Black ClickGUI", 8.0F, this.height - 10.0F, new Color(170, 170, 170).getRGB());

        for (CategoryComponent category : categoryList) {
            category.handleDrag(x, y);
            category.update(x, y);
            category.render();
        }

        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            int scrollDir = wheel > 0 ? 1 : -1;
            for (int i = categoryList.size() - 1; i >= 0; i--) {
                categoryList.get(i).onScroll(x, y, scrollDir);
            }
        }
    }

    public void mouseClicked(int x, int y, int mouseButton) {
        for (int i = categoryList.size() - 1; i >= 0; i--) {
            CategoryComponent category = categoryList.get(i);
            if (category.handleClick(x, y, mouseButton)) {
                categoryList.remove(i);
                categoryList.add(category);
                return;
            }
        }
    }

    public void mouseReleased(int x, int y, int mouseButton) {
        for (CategoryComponent categoryComponent : categoryList) {
            categoryComponent.mouseReleased(x, y, mouseButton);
        }
    }

    public void keyTyped(char typedChar, int key) {
        if (key == 1 && isBindingActive()) {
            for (CategoryComponent cat : categoryList) {
                cat.keyTyped(typedChar, key);
            }
            return;
        }

        if (key == 1) {
            this.mc.displayGuiScreen(null);
        } else {
            for (CategoryComponent cat : categoryList) {
                cat.keyTyped(typedChar, key);
            }
        }
    }

    public void onGuiClosed() {
        savePositions();
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    private void savePositions() {
        JsonObject json = new JsonObject();
        for (CategoryComponent cat : categoryList) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", cat.getX());
            pos.addProperty("y", cat.getY());
            pos.addProperty("open", cat.isOpened());
            json.add(cat.getName(), pos);
        }
        if (configFile.getParentFile() != null) {
            configFile.getParentFile().mkdirs();
        }
        try (FileWriter writer = new FileWriter(configFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPositions() {
        if (!configFile.exists()) return;
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            for (CategoryComponent cat : categoryList) {
                if (json.has(cat.getName())) {
                    JsonObject pos = json.getAsJsonObject(cat.getName());
                    cat.setX(pos.get("x").getAsInt());
                    cat.setY(pos.get("y").getAsInt());
                    cat.setOpened(pos.get("open").getAsBoolean());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isBindingActive() {
        for (CategoryComponent category : this.categoryList) {
            if (category.isBindingActive()) {
                return true;
            }
        }

        return false;
    }
}
