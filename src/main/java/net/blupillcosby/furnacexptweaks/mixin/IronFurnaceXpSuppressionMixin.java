package net.blupillcosby.furnacexptweaks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Universal suppression for Iron Furnaces native XP release.
 * This ensures that when an item is taken from an Iron Furnace, 
 * it does NOT release XP natively, forcing the player to use our button.
 */
@Pseudo
@Mixin(targets = "ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase", remap = false)
public abstract class IronFurnaceXpSuppressionMixin {

    @Inject(method = "grantStoredRecipeExperience", at = @At("HEAD"), cancellable = true)
    private void furnaceXpTweaks$cancelNativeXp(CallbackInfoReturnable<List<?>> cir) {
        if (!net.blupillcosby.furnacexptweaks.XpUtils.isModSupportEnabled()) return;
        // Return an empty list of recipes so no XP is awarded natively
        cir.setReturnValue(new ArrayList<>());
    }
}
