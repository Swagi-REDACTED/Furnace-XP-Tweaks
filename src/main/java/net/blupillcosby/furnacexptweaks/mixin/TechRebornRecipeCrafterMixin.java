package net.blupillcosby.furnacexptweaks.mixin;

import net.blupillcosby.furnacexptweaks.access.TechRebornExperienceAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Collection;

@Pseudo
@Mixin(targets = "reborncore.common.recipes.RecipeCrafter", remap = false)
public abstract class TechRebornRecipeCrafterMixin {

    @Shadow public BlockEntity blockEntity;

    @Unique
    private Object furnaceXpTweaks$getCurrentRecipe() {
        try {
            Field field = this.getClass().getField("currentRecipe");
            return field.get(this);
        } catch (Exception e) {
            return null;
        }
    }

    @Inject(method = "completeCraft", at = @At("TAIL"))
    private void furnaceXpTweaks$onCompleteCraft(CallbackInfo ci) {
        if (!net.blupillcosby.furnacexptweaks.XpUtils.isModSupportEnabled()) return;
        
        Object currentRecipe = furnaceXpTweaks$getCurrentRecipe();
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide() && currentRecipe != null) {
            if (blockEntity instanceof TechRebornExperienceAccessor accessor) {
                float xp = 0.0f;
                
                try {
                    // 1. Attempt to get XP directly from RebornRecipe if it has it
                    try {
                        Method expMethod = currentRecipe.getClass().getMethod("experience");
                        xp = (float) expMethod.invoke(currentRecipe);
                    } catch (Exception e) {
                        // 2. Fallback: Search for a vanilla cooking recipe with the same output
                        Method outputsMethod = currentRecipe.getClass().getMethod("outputs");
                        List<?> outputs = (List<?>) outputsMethod.invoke(currentRecipe);
                        
                        if (outputs != null && !outputs.isEmpty()) {
                            Object firstOutputTemplate = outputs.get(0);
                            Method createMethod = firstOutputTemplate.getClass().getMethod("create");
                            ItemStack outputStack = (ItemStack) createMethod.invoke(firstOutputTemplate);
                            
                            if (outputStack != null && !outputStack.isEmpty()) {
                                var server = blockEntity.getLevel().getServer();
                                if (server != null) {
                                    for (var holder : server.getRecipeManager().getRecipes()) {
                                        if (holder.value() instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe cook) {
                                            if (ItemStack.isSameItem(cook.assemble(null), outputStack)) {
                                                xp = cook.experience();
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 3. Last resort fallback
                        if (xp == 0.0f) {
                            xp = 0.7f;
                        }
                    }
                } catch (Exception ignored) {}
                
                float current = accessor.furnaceXpTweaks$getExperience();
                double max = net.blupillcosby.furnacexptweaks.XpUtils.getExperienceToLevelDouble(net.blupillcosby.furnacexptweaks.FurnaceXPTweaks.CONFIG.maxStoredLevels.get());
                accessor.furnaceXpTweaks$setExperience((float) Math.min(current + xp, max));
                blockEntity.setChanged();
                net.blupillcosby.furnacexptweaks.XpUtils.syncXpToClients(blockEntity);
            }
        }
    }
}
