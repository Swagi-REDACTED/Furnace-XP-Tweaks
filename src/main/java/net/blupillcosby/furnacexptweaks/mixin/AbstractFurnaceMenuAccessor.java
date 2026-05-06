package net.blupillcosby.furnacexptweaks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor to expose the BlockPos from AbstractFurnaceMenu.
 * The XP getter is on AbstractFurnaceMenuMixin via @Unique.
 * We use this accessor purely for the 'container' field to derive BlockPos.
 */
@Mixin(AbstractFurnaceMenu.class)
public interface AbstractFurnaceMenuAccessor {
    @Accessor("container")
    net.minecraft.world.Container furnaceXpTweaks$getContainer();
}
