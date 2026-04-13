package myau.module.modules;

import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.ui.ClickGui;
import myau.util.font.FontManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class GuiModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private ClickGui clickGui;
    public final ModeProperty fontMode = new ModeProperty("font-mode", FontManager.indexOfManagedFont("Minecraft"), FontManager.MANAGED_FONT_MODES);
    public final FloatProperty fontSize = new FloatProperty("font-size", 12.0F, 8.0F, 24.0F);

    public GuiModule() {
        super("ClickGui", false);
        setKey(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        if(clickGui == null){
            clickGui = new ClickGui();
        }
        mc.displayGuiScreen(clickGui);
    }
}
