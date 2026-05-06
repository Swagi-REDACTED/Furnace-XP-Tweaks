package net.blupillcosby.furnacexptweaks.mixin;

import net.blupillcosby.furnacexptweaks.XpUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Block.class)
public abstract class BlockMixin {

    /**
     * Universal XP Persistence: 
     * Injects into getDrops to ensure that when a block with experience is broken, 
     * the experience is stored inside the dropped ItemStack's Block Entity Data.
     */
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;", at = @At("RETURN"))
    private static void furnaceXpTweaks$persistXpOnDrop(
            BlockState state, 
            net.minecraft.server.level.ServerLevel level, 
            net.minecraft.core.BlockPos pos, 
            BlockEntity blockEntity, 
            net.minecraft.world.entity.Entity breaker, 
            net.minecraft.world.item.ItemInstance tool, 
            CallbackInfoReturnable<List<ItemStack>> cir) {
        
        if (blockEntity == null) return;

        double xp = XpUtils.getStoredXpFromBlockEntity(blockEntity);
        if (xp <= 0) return;

        List<ItemStack> drops = cir.getReturnValue();
        if (drops == null || drops.isEmpty()) return;

        for (ItemStack stack : drops) {
            // Check if this item is the block itself
            if (stack.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() == state.getBlock()) {
                // Get or create the block entity data component
                TypedEntityData<?> existingData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
                net.minecraft.nbt.CompoundTag tag = existingData != null ? existingData.copyTagWithoutId() : new net.minecraft.nbt.CompoundTag();
                
                // Store our custom XP
                tag.putFloat("furnacexptweaks:experience", (float) xp);
                
                // Re-apply to the stack
                stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(blockEntity.getType(), tag));
                break; // Only apply to the first matching block item
            }
        }
    }
}
