/*
 * Myau Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/suffpodiapi287/Myau-Beta
 */

package me.ksyz.accountmanager.gui;

import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.utils.Notification;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiAddOfflineAccount extends GuiScreen {

    private final GuiScreen previousScreen;
    private GuiTextField usernameField;
    private GuiButton addButton;
    private String status = "";

    public GuiAddOfflineAccount(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        int centerX = width / 2;

        usernameField = new GuiTextField(
                0, fontRendererObj,
                centerX - 100, height / 2 - 30,
                200, 20
        );
        usernameField.setMaxStringLength(16);
        usernameField.setFocused(true);

        addButton = new GuiButton(
                1, centerX - 100, height / 2,
                200, 20, "Add Offline Account"
        );

        GuiButton randomButton = new GuiButton(
                2,
                centerX + 105,
                height / 2 - 30,
                60,
                20,
                "Random"
        );
        buttonList.add(randomButton);
        buttonList.add(addButton);
        buttonList.add(new GuiButton(
                0, centerX - 100, height / 2 + 25,
                200, 20, "Back"
        ));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                mc.displayGuiScreen(previousScreen);
                break;

            case 1:
                addOfflineAccount();
                break;
            case 2: { // Random Username
                usernameField.setText("Player" + (int) (Math.random() * 10000));
                break;
            }
        }
    }

    private void addOfflineAccount() {
        String username = usernameField.getText().trim();

        if (username.length() < 3) {
            status = "\u00A7cUsername must be at least 3 characters.";
            return;
        }

        for (Account acc : AccountManager.accounts) {
            if (username.equalsIgnoreCase(acc.getUsername())) {
                status = "\u00A7cThis account already exists.";
                return;
            }
        }

        Account account = new Account(
                "",
                "",
                username,
                0L,
                "",
                ""
        );

        AccountManager.accounts.add(account);
        AccountManager.save();

        mc.displayGuiScreen(
                new GuiAccountManager(previousScreen,
                        new Notification("\u00A7aOffline account added: \u00A7f" + username, 3000))
        );
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(previousScreen);
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN) {
            actionPerformed(addButton);
            return;
        }

        usernameField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        usernameField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        drawCenteredString(
                fontRendererObj,
                "Add Offline Account",
                width / 2, height / 2 - 60,
                0xFFFFFF
        );

        drawCenteredString(
                fontRendererObj,
                "Offline / Singleplayer only",
                width / 2, height / 2 - 45,
                0xAAAAAA
        );

        usernameField.drawTextBox();

        if (!status.isEmpty()) {
            drawCenteredString(
                    fontRendererObj,
                    status,
                    width / 2, height / 2 + 55,
                    0xFFFFFF
            );
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        usernameField.updateCursorCounter();

        addButton.enabled = !usernameField.getText().trim().isEmpty();
    }
}
