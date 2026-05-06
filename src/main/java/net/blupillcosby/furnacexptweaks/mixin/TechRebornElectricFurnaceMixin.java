package net.blupillcosby.furnacexptweaks.mixin;

import net.blupillcosby.furnacexptweaks.access.TechRebornExperienceAccessor;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "techreborn.blockentity.machine.tier1.ElectricFurnaceBlockEntity", remap = false)
public abstract class TechRebornElectricFurnaceMixin {

    @Inject(method = "craftRecipe", at = @At("HEAD"))
    private void furnaceXpTweaks$onCraftRecipe(SmeltingRecipe recipe, CallbackInfo ci) {
        if (!net.blupillcosby.furnacexptweaks.XpUtils.isModSupportEnabled()) return;
        
        if (recipe != null && this instanceof TechRebornExperienceAccessor accessor) {
            float recipeXp = recipe.experience();
            if (recipeXp <= 0) recipeXp = 0.7f; // Default furnace XP if unspecified
            
            float current = accessor.furnaceXpTweaks$getExperience();
            double max = net.blupillcosby.furnacexptweaks.XpUtils.getExperienceToLevelDouble(net.blupillcosby.furnacexptweaks.FurnaceXPTweaks.CONFIG.maxStoredLevels.get());
            accessor.furnaceXpTweaks$setExperience((float) Math.min(current + recipeXp, max));
        }
    }
}
