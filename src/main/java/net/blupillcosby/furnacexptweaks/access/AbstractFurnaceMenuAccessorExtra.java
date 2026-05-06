package net.blupillcosby.furnacexptweaks.access;

import net.minecraft.core.BlockPos;

public interface AbstractFurnaceMenuAccessorExtra {
    /** Gets the position of the block associated with this menu. */
    BlockPos furnaceXpTweaks$getBlockPos();
    
    /** Gets stored XP directly from synced slots (if available). */
    double furnaceXpTweaks$getStoredXpPoints();
}
