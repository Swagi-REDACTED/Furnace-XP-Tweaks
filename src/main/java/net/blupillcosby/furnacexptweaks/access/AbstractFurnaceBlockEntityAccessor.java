package net.blupillcosby.furnacexptweaks.access;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;

public interface AbstractFurnaceBlockEntityAccessor {
    void furnaceXpTweaks$claimXp(ServerPlayer player);
    Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> furnaceXpTweaks$getRecipesUsed();
}
