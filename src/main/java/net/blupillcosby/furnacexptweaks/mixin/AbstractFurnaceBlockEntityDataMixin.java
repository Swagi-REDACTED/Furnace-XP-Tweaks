package net.blupillcosby.furnacexptweaks.mixin;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.blupillcosby.furnacexptweaks.access.AbstractFurnaceBlockEntityAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity$1")
public class AbstractFurnaceBlockEntityDataMixin {
    @Shadow @Final AbstractFurnaceBlockEntity field_14912; // This is the 'this$0' field. Need to verify name.

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void furnaceXpTweaks$getStoredData(int index, CallbackInfoReturnable<Integer> cir) {
        if (index == 4) {
            cir.setReturnValue(furnaceXpTweaks$calculateTotalXpPoints());
        } else if (index == 5) {
            cir.setReturnValue(field_14912.getBlockPos().getX());
        } else if (index == 6) {
            cir.setReturnValue(field_14912.getBlockPos().getY());
        } else if (index == 7) {
            cir.setReturnValue(field_14912.getBlockPos().getZ());
        }
    }

    @Inject(method = "getCount", at = @At("HEAD"), cancellable = true)
    private void furnaceXpTweaks$increaseDataCount(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(8);
    }

    private int furnaceXpTweaks$calculateTotalXpPoints() {
        AbstractFurnaceBlockEntity furnace = field_14912;
        if (furnace.getLevel() instanceof ServerLevel serverLevel) {
            // Get recipesUsed via accessor or shadow
            Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed = ((AbstractFurnaceBlockEntityAccessor) furnace).furnaceXpTweaks$getRecipesUsed();
            float totalXp = 0;
            var recipeRegistry = serverLevel.registryAccess().lookupOrThrow(Registries.RECIPE);
            for (Reference2IntMap.Entry<ResourceKey<Recipe<?>>> entry : recipesUsed.reference2IntEntrySet()) {
                int count = entry.getIntValue();
                totalXp += (float) recipeRegistry.get(entry.getKey())
                        .map(recipe -> ((AbstractCookingRecipe) recipe.value()).experience() * count)
                        .orElse(0.0f);
            }
            return (int) totalXp;
        }
        return 0;
    }
}
