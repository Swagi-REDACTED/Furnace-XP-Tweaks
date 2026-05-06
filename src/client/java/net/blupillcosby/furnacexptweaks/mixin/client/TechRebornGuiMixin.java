package net.blupillcosby.furnacexptweaks.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "techreborn.client.gui.GuiIronFurnace", remap = false)
public abstract class TechRebornGuiMixin extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<net.minecraft.world.inventory.AbstractContainerMenu> {

    public TechRebornGuiMixin() {
        super(null, null, null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void furnaceXpTweaks$hideTRButton(CallbackInfo ci) {
        if (!net.blupillcosby.furnacexptweaks.XpUtils.isModSupportEnabled()) return;
        // Find and remove TR's native XpButtonWidget from all lists
        java.util.function.Predicate<Object> isTRButton = w -> {
            String name = w.getClass().getName();
            return name.contains("XpButtonWidget") || name.contains("ExperienceButton");
        };

        ScreenAccessor sa = (ScreenAccessor) this;
        sa.furnaceXpTweaks$getRenderables().removeIf(isTRButton);
        sa.furnaceXpTweaks$getChildren().removeIf(isTRButton);
        sa.furnaceXpTweaks$getNarratables().removeIf(isTRButton);
    }
}
