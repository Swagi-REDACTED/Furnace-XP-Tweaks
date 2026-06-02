package net.blupillcosby.furnacexptweaks.mixin;

import net.blupillcosby.furnacexptweaks.XpUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Block.class)
public abstract class AbstractFurnaceBlockMixin {

    /** Key used to embed pre-calculated XP in the dropped item's NBT. */
    @Unique
    private static final String FURNACE_XP_TWEAKS$XP_KEY = "furnacexptweaks:stored_xp";

    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",
            at = @At("RETURN"))
    private static void furnaceXpTweaks$storeXpInDrops(BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker, ItemInstance tool, CallbackInfoReturnable<List<ItemStack>> cir) {
        furnaceXpTweaks$writeXpToDrops(state, level, blockEntity, cir);
    }

    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;",
            at = @At("RETURN"))
    private static void furnaceXpTweaks$storeXpInDropsFallback(BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity blockEntity, CallbackInfoReturnable<List<ItemStack>> cir) {
        furnaceXpTweaks$writeXpToDrops(state, level, blockEntity, cir);
    }

    @Unique
    private static void furnaceXpTweaks$writeXpToDrops(BlockState state, ServerLevel level, @Nullable BlockEntity blockEntity, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (blockEntity == null) return;

        // Use Universal XP detection logic
        double xpExact = XpUtils.getStoredXpFromBlockEntity(blockEntity);
        double flooredXp = Math.floor(xpExact);
        if (flooredXp < 1) return;

        List<ItemStack> drops = cir.getReturnValue();
        if (drops == null || drops.isEmpty()) return;

        for (ItemStack stack : drops) {
            // Check if the drop is the block itself (or related item)
            if (stack.getItem() != state.getBlock().asItem()) continue;

            TypedEntityData<?> existingData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            CompoundTag tag = existingData != null ? existingData.copyTagWithoutId() : new CompoundTag();

            // Clear unstackable block entity data
            tag.remove("RecipesUsed");
            tag.remove("recipesUsed");
            tag.remove("recipes");
            tag.remove("x");
            tag.remove("y");
            tag.remove("z");
            tag.remove("id");

            // Embed the calculated double XP
            tag.putDouble(FURNACE_XP_TWEAKS$XP_KEY, flooredXp);

            stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(blockEntity.getType(), tag));
            break;
        }
    }
}
