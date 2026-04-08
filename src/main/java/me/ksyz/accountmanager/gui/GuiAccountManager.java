/*
 * Myau Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/suffpodiapi287/Myau-Beta
 */

package me.ksyz.accountmanager.gui;

import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.MicrosoftAuth;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.util.Session;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class GuiAccountManager extends GuiScreen {
    private final GuiScreen previousScreen;

    private GuiButton loginButton = null;
    private GuiButton deleteButton = null;
    private GuiButton cancelButton = null;
    private GuiButton editButton = null;
    private GuiAccountList guiAccountList = null;
    private Notification notification = null;
    private int selectedAccount = -1;
    private ExecutorService executor = null;
    private CompletableFuture<Void> task = null;

    public GuiAccountManager(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    public GuiAccountManager(GuiScreen previousScreen, Notification notification) {
        this.previousScreen = previousScreen;
        this.notification = notification;
    }

    @Override
    public void initGui() {
        AccountManager.load();
        Keyboard.enableRepeatEvents(true);

        buttonList.add(loginButton = new GuiButton(
                0, width / 2 - 200, height - 52, 90, 20, "Login"
        ));
        buttonList.add(new GuiButton(
                1, width / 2 - 100, height - 52, 90, 20, "Add Offline"
        ));
        buttonList.add(new GuiButton(
                6, width / 2, height - 52, 120, 20, "Add Microsoft"
        ));
        buttonList.add(new GuiButton(
                4, width / 2 + 125, height - 52, 90, 20, "Session"
        ));
        buttonList.add(deleteButton = new GuiButton(
                2, width / 2 - 100, height - 28, 90, 20, "Delete"
        ));
        buttonList.add(cancelButton = new GuiButton(
                3, width / 2 + 125, height - 28, 90, 20, "Cancel"
        ));
        buttonList.add(new GuiButton(
                5, width / 2, height - 28, 120, 20, "Add Token"
        ));
        buttonList.add(editButton = new GuiButton(
                7, width / 2 - 200, height - 28, 90, 20, "Edit"
        ));

        guiAccountList = new GuiAccountList(mc);
        guiAccountList.registerScrollButtons(11, 12);
        updateScreen();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);

        if (task != null && !task.isDone()) {
            task.cancel(true);
            executor.shutdownNow();
        }
    }

    @Override
    public void updateScreen() {
        if (loginButton != null && deleteButton != null && editButton != null) {
            if (selectedAccount >= 0) {
                Account acc = AccountManager.accounts.get(selectedAccount);

                loginButton.enabled = true;
                deleteButton.enabled = true;

                if (StringUtils.isBlank(acc.getAccessToken())) {
                    // OFFLINE
                    loginButton.displayString = "Use Offline";
                    editButton.enabled = true;
                } else {
                    // MICROSOFT
                    loginButton.displayString = "Login";
                    editButton.enabled = false;
                }
            } else {
                loginButton.enabled = false;
                deleteButton.enabled = false;
                editButton.enabled = false;
            }

            if (task != null && !task.isDone()) {
                loginButton.enabled = false;
                editButton.enabled = false;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks) {
        if (guiAccountList != null) {
            guiAccountList.drawScreen(mouseX, mouseY, renderPartialTicks);
        }
        super.drawScreen(mouseX, mouseY, renderPartialTicks);

        drawCenteredString(
                fontRendererObj,
                TextFormatting.translate(String.format(
                        "&rAccount Manager &8(&7%s&8)&r", AccountManager.accounts.size()
                )),
                width / 2, 20, -1
        );

        String text = TextFormatting.translate(String.format(
                "&7Username: &3%s&r", SessionManager.get().getUsername()
        ));
        mc.currentScreen.drawString(mc.fontRendererObj, text, 3, 3, -1);

        if (notification != null && !notification.isExpired()) {
            String notificationText = notification.getMessage();
            Gui.drawRect(
                    mc.currentScreen.width / 2 - mc.fontRendererObj.getStringWidth(notificationText) / 2 - 3, 4,
                    mc.currentScreen.width / 2 + mc.fontRendererObj.getStringWidth(notificationText) / 2 + 3, 4 + 3 + mc.fontRendererObj.FONT_HEIGHT + 2,
                    0x64000000
            );
            mc.currentScreen.drawCenteredString(
                    mc.fontRendererObj, notification.getMessage(),
                    mc.currentScreen.width / 2, 4 + 3, -1
            );
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        if (guiAccountList != null) {
            guiAccountList.handleMouseInput();
        }
        super.handleMouseInput();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        switch (keyCode) {
            case Keyboard.KEY_UP: {
                if (selectedAccount > 0) {
                    --selectedAccount;
                    if (isCtrlKeyDown()) {
                        Collections.swap(AccountManager.accounts, selectedAccount, selectedAccount + 1);
                        AccountManager.save();
                    }
                }
            }
            break;
            case Keyboard.KEY_DOWN: {
                if (selectedAccount < AccountManager.accounts.size() - 1) {
                    ++selectedAccount;
                    if (isCtrlKeyDown()) {
                        Collections.swap(AccountManager.accounts, selectedAccount, selectedAccount - 1);
                        AccountManager.save();
                    }
                }
            }
            break;
            case Keyboard.KEY_RETURN: {
                actionPerformed(loginButton);
            }
            break;
            case Keyboard.KEY_DELETE: {
                actionPerformed(deleteButton);
            }
            break;
            case Keyboard.KEY_ESCAPE: {
                actionPerformed(cancelButton);
            }
            break;
        }

        if (isKeyComboCtrlC(keyCode) && selectedAccount >= 0) {
            setClipboardString(AccountManager.accounts.get(selectedAccount).getUsername());
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == null) {
            return;
        }

        if (button.enabled) {
            switch (button.id) {
                case 0: { // Login
                    if (task == null || task.isDone()) {
                        if (executor == null) {
                            executor = Executors.newSingleThreadExecutor();
                        }
                        Account account = AccountManager.accounts.get(selectedAccount);

                        // ===============================
                        // OFFLINE LOGIN
                        // ===============================
                        if (StringUtils.isBlank(account.getAccessToken())) {
                            Session offlineSession = new Session(
                                    account.getUsername(),
                                    "0",
                                    "0",
                                    "legacy"
                            );

                            SessionManager.set(offlineSession);

                            notification = new Notification(
                                    TextFormatting.translate("&aUsing offline account: &f" + account.getUsername()),
                                    3000L
                            ); 
                            return;
                        }

                        // ===============================
                        // MICROSOFT LOGIN
                        // ===============================
                        String username = StringUtils.isBlank(account.getUsername()) ? "???" : account.getUsername();
                        AtomicReference<String> refreshToken = new AtomicReference<>("");
                        AtomicReference<String> accessToken = new AtomicReference<>("");
                        notification = new Notification(TextFormatting.translate(String.format(
                                "&7Fetching your Minecraft profile... (%s)&r", username
                        )), -1L);
                        MicrosoftAuth.CLIENT_ID = account.getClientId();
                        MicrosoftAuth.SCOPE = account.getScope();
                        task = MicrosoftAuth.login(account.getAccessToken(), executor)
                                .handle((session, error) -> {
                                    if (session != null) {
                                        account.setUsername(session.getUsername());
                                        AccountManager.save();
                                        SessionManager.set(session);
                                        notification = new Notification(TextFormatting.translate(String.format(
                                                "&aSuccessful login! (%s)&r", account.getUsername()
                                        )), 5000L);
                                        return true;
                                    }
                                    return false;
                                })
                                .thenComposeAsync(completed -> {
                                    if (completed) {
                                        throw new NoSuchElementException();
                                    }
                                    notification = new Notification(TextFormatting.translate(String.format(
                                            "&7Refreshing Microsoft access tokens... (%s)&r", username
                                    )), -1L);
                                    return MicrosoftAuth.refreshMSAccessTokens(account.getRefreshToken(), executor);
                                })
                                .thenComposeAsync(msAccessTokens -> {
                                    notification = new Notification(TextFormatting.translate(String.format(
                                            "&7Acquiring Xbox access token... (%s)&r", username
                                    )), -1L);
                                    refreshToken.set(msAccessTokens.get("refresh_token"));
                                    return MicrosoftAuth.acquireXboxAccessToken(msAccessTokens.get("access_token"), executor);
                                })
                                .thenComposeAsync(xboxAccessToken -> {
                                    notification = new Notification(TextFormatting.translate(String.format(
                                            "&7Acquiring Xbox XSTS token... (%s)&r", username
                                    )), -1L);
                                    return MicrosoftAuth.acquireXboxXstsToken(xboxAccessToken, executor);
                                })
                                .thenComposeAsync(xboxXstsData -> {
                                    notification = new Notification(TextFormatting.translate(String.format(
                                            "&7Acquiring Minecraft access token... (%s)&r", username
                                    )), -1L);
                                    return MicrosoftAuth.acquireMCAccessToken(
                                            xboxXstsData.get("Token"), xboxXstsData.get("uhs"), executor
                                    );
                                })
                                .thenComposeAsync(mcToken -> {
                                    notification = new Notification(TextFormatting.translate(String.format(
                                            "&7Fetching your Minecraft profile... (%s)&r", username
                                    )), -1L);
                                    accessToken.set(mcToken);
                                    return MicrosoftAuth.login(mcToken, executor);
                                })
                                .thenAccept(session -> {
                                    account.setRefreshToken(refreshToken.get());
                                    account.setAccessToken(accessToken.get());
                                    account.setUsername(session.getUsername());
                                    AccountManager.save();
                                    SessionManager.set(session);
                                    notification = new Notification(TextFormatting.translate(String.format(
                                            "&aSuccessful login! (%s)&r", account.getUsername()
                                    )), 5000L);
                                })
                                .exceptionally(error -> {
                                    if (!(error.getCause() instanceof NoSuchElementException)) {
                                        notification = new Notification(TextFormatting.translate(String.format(
                                                "&c%s (%s)&r", error.getMessage(), username
                                        )), 5000L);
                                    }
                                    return null;
                                });
                    }
                }
                break;
                case 1: { // Add Offline
                    mc.displayGuiScreen(new GuiAddOfflineAccount(this));
                }
                break;
                case 2: { // Delete
                    if (selectedAccount > -1 && selectedAccount < AccountManager.accounts.size()) {
                        AccountManager.accounts.remove(selectedAccount);
                        AccountManager.save();
                        selectedAccount = -1;
                        updateScreen();
                    }
                }
                break;
                case 3: { // Cancel
                    mc.displayGuiScreen(previousScreen);
                }
                break;
                case 4:{
                    mc.displayGuiScreen(new GuiSessionLogin(this));
                }
                break;
                case 5:{
                    mc.displayGuiScreen(new GuiAddToken(this));
                }
                break;
                case 6: { // Add Microsoft Account
                    mc.displayGuiScreen(new GuiMicrosoftAuth(this));
                }
                break;
                case 7: { // Edit Offline Account
                    Account acc = AccountManager.accounts.get(selectedAccount);

                    // Safety: only allow editing offline accounts.
                    if (!StringUtils.isBlank(acc.getAccessToken())) {
                        return;
                    }

                    mc.displayGuiScreen(new GuiEditOfflineAccount(this, acc));
                }
                break;
                default: {
                    guiAccountList.actionPerformed(button);
                }
            }
        }
    }

    class GuiAccountList extends GuiSlot {
        public GuiAccountList(Minecraft mc) {
            super(
                    mc, GuiAccountManager.this.width, GuiAccountManager.this.height,
                    32, GuiAccountManager.this.height - 64, 16
            );
        }

        @Override
        protected int getSize() {
            return AccountManager.accounts.size();
        }

        @Override
        protected boolean isSelected(int slotIndex) {
            return slotIndex == GuiAccountManager.this.selectedAccount;
        }

        @Override
        protected int getScrollBarX() {
            return (this.width + getListWidth()) / 2 + 2;
        }

        @Override
        public int getListWidth() {
            return (150 + 4) * 2;
        }

        @Override
        protected int getContentHeight() {
            return AccountManager.accounts.size() * 16;
        }

        @Override
        protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
            GuiAccountManager.this.selectedAccount = slotIndex;
            GuiAccountManager.this.updateScreen();
            if (isDoubleClick) {
                GuiAccountManager.this.actionPerformed(loginButton);
            }
        }

        @Override
        protected void drawBackground() {
            GuiAccountManager.this.drawDefaultBackground();
        }

        @Override
        protected void drawSlot(int entryID, int x, int y, int k, int mouseXIn, int mouseYIn) {
            FontRenderer fr = GuiAccountManager.this.fontRendererObj;
            Account account = AccountManager.accounts.get(entryID);

            boolean isOffline = StringUtils.isBlank(account.getAccessToken());

            String username = account.getUsername();
            if (StringUtils.isBlank(username)) {
                username = "&7&l?";
            } else {
                if (isOffline) {
                    // OFFLINE account
                    username = String.format("&7[OFFLINE] %s", username);
                } else {
                    // Microsoft account
                    username = String.format("&b[MS] %s", username);
                }

                // Highlight currently logged-in account
                if (!isOffline && account.getAccessToken().equals(SessionManager.get().getToken())) {
                    username = String.format("&a&l%s", username);
                } else if (username.contains(SessionManager.get().getUsername())) {
                    username = String.format("&a%s", username);
                }
            }

            username = TextFormatting.translate("&r" + username + "&r");

            GuiAccountManager.this.drawString(
                    fr, username, x + 2, y + 2, -1
            );

            // Unban status display.
            long currentTime = System.currentTimeMillis();
            long unbanTime = account.getUnban();
            String unban;

            if (unbanTime < 0L) {
                unban = "&4&lX";
            } else if (unbanTime <= currentTime) {
                unban = "&2&lOK";
            } else {
                long diff = unbanTime - currentTime;
                long s = (diff / 1000L) % 60L;
                long m = (diff / 60000L) % 60L;
                long h = (diff / 3600000L) % 24L;
                long d = (diff / 86400000L);
                unban = String.format(
                        "%s%s%s%s",
                        d > 0L ? String.format("%dd", d) : "",
                        h > 0L ? String.format(" %dh", h) : "",
                        m > 0L ? String.format(" %dm", m) : "",
                        s > 0L ? String.format(" %ds", s) : ""
                );
                unban = unban.trim();
                unban = String.format("%s &c&l!", unban);
            }

            unban = TextFormatting.translate("&r" + unban + "&r");

            GuiAccountManager.this.drawString(
                    fr, unban,
                    x + getListWidth() - 5 - fr.getStringWidth(unban),
                    y + 2,
                    -1
            );
        }
    }
}
