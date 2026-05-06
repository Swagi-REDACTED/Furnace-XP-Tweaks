package net.blupillcosby.furnacexptweaks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Pseudo
@Mixin(targets = "techreborn.blockentity.machine.iron.IronFurnaceBlockEntity", remap = false)
public abstract class TechRebornIronFurnaceMixin {

    @Shadow public float experience;
    @Shadow(remap = false) private net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.SmeltingRecipe> lastRecipe;
    
    @Unique
    private float furnaceXpTweaks$lastExp = 0;

    @Inject(method = "smelt", at = @At("HEAD"))
    private void furnaceXpTweaks$beforeSmelt(CallbackInfo ci) {
        this.furnaceXpTweaks$lastExp = experience;
    }

    @Inject(method = "smelt", at = @At("TAIL"))
    private void furnaceXpTweaks$afterSmelt(CallbackInfo ci) {
        if (!net.blupillcosby.furnacexptweaks.XpUtils.isModSupportEnabled()) return;

        net.blupillcosby.furnacexptweaks.access.TechRebornExperienceAccessor acc = 
            (net.blupillcosby.furnacexptweaks.access.TechRebornExperienceAccessor) this;
        
        float customXp = acc.furnaceXpTweaks$getExperience();
        float gainedTrXp = experience - furnaceXpTweaks$lastExp;

        if (gainedTrXp > 0) {
            customXp += gainedTrXp;
        } else if (lastRecipe == null) {
            // Fallback for custom TR recipes that might not be detected as smelting
            customXp += 0.7f;
        }

        double max = net.blupillcosby.furnacexptweaks.XpUtils.getExperienceToLevelDouble(net.blupillcosby.furnacexptweaks.FurnaceXPTweaks.CONFIG.maxStoredLevels.get());
        if (customXp > max) {
            customXp = (float) max;
        }

        acc.furnaceXpTweaks$setExperience(customXp);
        this.experience = 0; // Disable native TR XP storage to prevent double-claiming
        net.blupillcosby.furnacexptweaks.XpUtils.syncXpToClients((net.minecraft.world.level.block.entity.BlockEntity)(Object)this);
    }

    @Inject(method = "handleGuiInputFromClient", at = @At("HEAD"), cancellable = true, remap = false)
    private void furnaceXpTweaks$disableNativeClaim(net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        if (!net.blupillcosby.furnacexptweaks.XpUtils.isModSupportEnabled()) return;
        ci.cancel();
    }
}
