package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.TextProperty;
import myau.util.ChatUtil;
import myau.util.RandomUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.network.play.server.S45PacketTitle;

import java.util.Locale;
import java.util.regex.Pattern;

public class AutoLogin extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String DEFAULT_PASSWORD = "axolotlaxolotl";
    private static final Pattern REGISTER_PROMPT = Pattern.compile("(^|\\s)/(?:register|reg)(?:\\s|$)");
    private static final Pattern LOGIN_PROMPT = Pattern.compile("(^|\\s)/(?:login|log|l)(?:\\s|$)");

    public final BooleanProperty autoRegister = new BooleanProperty("auto-register", true);
    public final BooleanProperty autoLogin = new BooleanProperty("auto-login", true);
    public final TextProperty password = new TextProperty("password", DEFAULT_PASSWORD, () -> this.autoRegister.getValue() || this.autoLogin.getValue());
    public final IntProperty minDelay = new IntProperty("min-delay", 150, 0, 1000, () -> this.autoRegister.getValue() || this.autoLogin.getValue());
    public final IntProperty maxDelay = new IntProperty("max-delay", 300, 0, 1000, () -> this.autoRegister.getValue() || this.autoLogin.getValue());

    private Status status = Status.WAITING;
    private AuthAction queuedAction = AuthAction.NONE;
    private AuthAction lastAction = AuthAction.NONE;
    private long queuedAt = -1L;

    public AutoLogin() {
        super("AutoLogin", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST || this.queuedAction == AuthAction.NONE || this.queuedAt < 0L) {
            return;
        }

        if (mc.thePlayer == null || mc.theWorld == null || System.currentTimeMillis() < this.queuedAt) {
            return;
        }

        String password = this.password.getValue();
        if (password == null || password.trim().isEmpty() || password.contains(" ")) {
            ChatUtil.sendFormatted(String.format("%s%s: &cPassword is invalid. Use a password without spaces.&r", Myau.clientName, this.getName()));
            this.status = Status.STOPPED;
            this.queuedAction = AuthAction.NONE;
            this.queuedAt = -1L;
            return;
        }

        String command;
        if (this.queuedAction == AuthAction.REGISTER) {
            command = String.format("/register %s %s", password, password);
        } else {
            command = String.format("/login %s", password);
        }

        this.lastAction = this.queuedAction;
        this.queuedAction = AuthAction.NONE;
        this.queuedAt = -1L;
        this.status = Status.SENT_COMMAND;
        ChatUtil.sendMessage(command);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE || event.isCancelled()) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (packet instanceof S40PacketDisconnect) {
            this.resetState();
            return;
        }

        String message = this.readMessage(packet);
        if (message == null || message.isEmpty()) {
            return;
        }

        if (this.status == Status.WAITING) {
            if (this.handlePrompt(message)) {
                return;
            }
        } else if (this.status == Status.QUEUED || this.status == Status.SENT_COMMAND) {
            if (this.isSuccessMessage(message)) {
                ChatUtil.sendFormatted(String.format("%s%s: &a%s successful.&r", Myau.clientName, this.getName(), this.lastAction.getDisplay()));
                this.status = Status.STOPPED;
                this.queuedAction = AuthAction.NONE;
                this.queuedAt = -1L;
            } else if (this.isFailureMessage(message)) {
                ChatUtil.sendFormatted(String.format("%s%s: &c%s failed. Check your password.&r", Myau.clientName, this.getName(), this.lastAction.getDisplay()));
                this.status = Status.STOPPED;
                this.queuedAction = AuthAction.NONE;
                this.queuedAt = -1L;
            } else if (this.isStopMessage(message)) {
                this.status = Status.STOPPED;
                this.queuedAction = AuthAction.NONE;
                this.queuedAt = -1L;
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.resetState();
    }

    @Override
    public void onDisabled() {
        this.resetState();
    }

    private boolean handlePrompt(String message) {
        if (this.autoRegister.getValue() && REGISTER_PROMPT.matcher(message).find()) {
            this.queue(AuthAction.REGISTER);
            return true;
        }

        if (this.autoLogin.getValue() && LOGIN_PROMPT.matcher(message).find()) {
            this.queue(AuthAction.LOGIN);
            return true;
        }

        return false;
    }

    private void queue(AuthAction action) {
        if (action == AuthAction.NONE || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        int min = Math.min(this.minDelay.getValue(), this.maxDelay.getValue());
        int max = Math.max(this.minDelay.getValue(), this.maxDelay.getValue());
        long delay = RandomUtil.nextLong(min, max);
        this.queuedAction = action;
        this.queuedAt = System.currentTimeMillis() + delay;
        this.status = Status.QUEUED;
        ChatUtil.sendFormatted(String.format("%s%s: &fQueueing %s in %dms&r", Myau.clientName, this.getName(), action.getDisplay().toLowerCase(Locale.ROOT), delay));
    }

    private String readMessage(Packet<?> packet) {
        if (packet instanceof S02PacketChat) {
            S02PacketChat chatPacket = (S02PacketChat) packet;
            if (chatPacket.getChatComponent() != null) {
                return this.normalize(chatPacket.getChatComponent().getUnformattedText());
            }
        }

        if (packet instanceof S45PacketTitle) {
            S45PacketTitle titlePacket = (S45PacketTitle) packet;
            if (titlePacket.getMessage() != null) {
                return this.normalize(titlePacket.getMessage().getUnformattedText());
            }
        }

        return null;
    }

    private String normalize(String message) {
        if (message == null) {
            return "";
        }

        return message.toLowerCase(Locale.ROOT).replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    private boolean isSuccessMessage(String message) {
        return message.contains("registered")
                || message.contains("register successful")
                || message.contains("login successful")
                || message.contains("logged in")
                || message.contains("successfully logged")
                || message.contains("success");
    }

    private boolean isFailureMessage(String message) {
        return message.contains("incorrect")
                || message.contains("wrong")
                || message.contains("invalid")
                || message.contains("failed")
                || message.contains("error")
                || message.contains("try again");
    }

    private boolean isStopMessage(String message) {
        return message.contains("already logged")
                || message.contains("already registered")
                || message.contains("unknown command")
                || message.contains("you are already");
    }

    private void resetState() {
        this.status = Status.WAITING;
        this.queuedAction = AuthAction.NONE;
        this.lastAction = AuthAction.NONE;
        this.queuedAt = -1L;
    }

    private enum Status {
        WAITING,
        QUEUED,
        SENT_COMMAND,
        STOPPED
    }

    private enum AuthAction {
        NONE("Action"),
        REGISTER("Register"),
        LOGIN("Login");

        private final String display;

        AuthAction(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return this.display;
        }
    }
}
