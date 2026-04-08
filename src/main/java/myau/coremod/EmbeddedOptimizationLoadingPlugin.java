package myau.coremod;

import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import pl.asie.foamfix.coremod.FoamFixCoreContainer;
import pl.asie.foamfix.coremod.FoamFixTransformer;
import pl.asie.foamfix.shared.FoamFixShared;

import java.io.File;
import java.util.Map;

@IFMLLoadingPlugin.Name("MyauEmbeddedOptimizations")
@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions({
        "myau.coremod",
        "io.prplz.memoryfix",
        "pl.asie.foamfix"
})
public class EmbeddedOptimizationLoadingPlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                "io.prplz.memoryfix.ClassTransformer",
                "pl.asie.foamfix.coremod.FoamFixTransformer"
        };
    }

    @Override
    public String getModContainerClass() {
        return FoamFixCoreContainer.class.getName();
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        FoamFixShared.coremodEnabled = true;
        FoamFixShared.config.init(new File(new File("config"), "foamfix.cfg"), true);
        FoamFixShared.config.geSmallPropertyStorage = false;
        FoamFixShared.config.clDynamicItemModels = false;

        if (FoamFixShared.config.geBlacklistLibraryTransformers) {
            LaunchClassLoader classLoader = (LaunchClassLoader) getClass().getClassLoader();
            classLoader.addTransformerExclusion("com.ibm.icu.");
            classLoader.addTransformerExclusion("com.sun.");
            classLoader.addTransformerExclusion("gnu.trove.");
            classLoader.addTransformerExclusion("io.netty.");
            classLoader.addTransformerExclusion("it.unimi.dsi.fastutil.");
            classLoader.addTransformerExclusion("joptsimple.");
            classLoader.addTransformerExclusion("org.apache.");
            classLoader.addTransformerExclusion("oshi.");
            classLoader.addTransformerExclusion("scala.");
        }

        FoamFixTransformer.init();
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
