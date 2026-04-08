package myau.mixin;

import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderer.class)
public interface IAccessorItemRendererAnimations {
    @Accessor("equippedProgress")
    float getEquippedProgress();

    @Accessor("prevEquippedProgress")
    float getPrevEquippedProgress();
}
