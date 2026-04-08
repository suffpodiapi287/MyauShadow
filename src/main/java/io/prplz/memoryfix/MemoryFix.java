package io.prplz.memoryfix;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import static net.minecraftforge.fml.common.Mod.EventHandler;

@Mod(modid = "memoryfix", name = "MemoryFix", version = "embedded", acceptableRemoteVersions = "*")
public class MemoryFix {
    @EventHandler
    public void init(FMLInitializationEvent event) {
        // Transformer-only integration; the original update checker is intentionally disabled.
    }
}
