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

public class GuiEditOfflineAccount extends GuiScreen {

    private final GuiScreen previousScreen;
    private final Account account;

    private GuiTextField usernameField;
    private GuiButton saveButton;
    private String status = "";

    public GuiEditOfflineAccount(GuiScreen previousScreen, Account account) {
        this.previousScreen = previousScreen;
        this.account = account;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        int centerX = width / 2;

        usernameField = new GuiTextField(
                0,
                fontRendererObj,
                centerX - 100,
                height / 2 - 20,
                200,
                20
        );
        usernameField.setMaxStringLength(16);
        usernameField.setText(account.getUsername());
        usernameField.setFocused(true);

        saveButton = new GuiButton(
                1,
                centerX - 100,
                height / 2 + 10,
                200,
                20,
                "Save"
        );

        buttonList.add(saveButton);
        buttonList.add(new GuiButton(
                0,
                centerX - 100,
                height / 2 + 35,
                200,
                20,
                "Back"
        ));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                mc.displayGuiScreen(previousScreen);
                break;

            case 1:
                saveUsername();
                break;
        }
    }

    private void saveUsername() {
        String newName = usernameField.getText().trim();

        if (newName.length() < 3) {
            status = "\u00A7cUsername must be at least 3 characters.";
            return;
        }

        account.setUsername(newName);
        AccountManager.save();

        mc.displayGuiScreen(
                new GuiAccountManager(
                        previousScreen,
                        new Notification("\u00A7aUsername updated to \u00A7f" + newName, 3000)
                )
        );
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(previousScreen);
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN && saveButton.enabled) {
            actionPerformed(saveButton);
            return;
        }

        usernameField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        drawCenteredString(
                fontRendererObj,
                "Edit Offline Account",
                width / 2,
                height / 2 - 50,
                0xFFFFFF
        );

        usernameField.drawTextBox();

        if (!status.isEmpty()) {
            drawCenteredString(
                    fontRendererObj,
                    status,
                    width / 2,
                    height / 2 + 60,
                    0xFFFFFF
            );
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
}
