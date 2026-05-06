package net.blupillcosby.furnacexptweaks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockNativeSuppressionMixin {

    /**
     * Redirects the call to getRecipesToAwardAndPopExperience during block removal.
     * This prevents vanilla furnaces from dropping XP orbs when broken, 
     * as our mod stores the XP in the dropped item instead.
     */
    @Redirect(method = "preRemoveSideEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getRecipesToAwardAndPopExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;"))
    private java.util.List<?> furnaceXpTweaks$cancelVanillaXpDrop(AbstractFurnaceBlockEntity instance, ServerLevel level, Vec3 pos) {
        if (!net.blupillcosby.furnacexptweaks.XpUtils.isModSupportEnabled()) {
             // If mod support is off, only suppress for native vanilla furnaces
             boolean isNative = instance instanceof net.minecraft.world.level.block.entity.FurnaceBlockEntity ||
                                instance instanceof net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity ||
                                instance instanceof net.minecraft.world.level.block.entity.SmokerBlockEntity;
             if (!isNative) {
                 // Restore original functionality for modded furnaces inheriting from vanilla
                 return instance.getRecipesToAwardAndPopExperience(level, pos);
             }
        }
        // Return an empty list to prevent XP from being popped (default mod behavior)
        return java.util.Collections.emptyList();
    }
}
