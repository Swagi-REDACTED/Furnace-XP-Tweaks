package net.blupillcosby.furnacexptweaks.access;

import net.minecraft.core.BlockPos;

/**
 * Duck-typing interface implemented by AbstractFurnaceMenuMixin.
 * Allows client code to safely query synced XP data from AbstractFurnaceMenu.
 */
public interface AbstractFurnaceMenuAccessorExtra {
    double furnaceXpTweaks$getStoredXpPoints();
    BlockPos furnaceXpTweaks$getBlockPos();
}
