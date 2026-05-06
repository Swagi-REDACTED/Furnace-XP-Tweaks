package net.blupillcosby.furnacexptweaks.mixin;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.blupillcosby.furnacexptweaks.XpUtils;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.blupillcosby.furnacexptweaks.access.AbstractFurnaceBlockEntityAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin extends BlockEntity implements AbstractFurnaceBlockEntityAccessor {

    @Shadow @Final private Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed;

    public AbstractFurnaceBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "awardUsedRecipesAndPopExperience", at = @At("HEAD"), cancellable = true)
    private void furnaceXpTweaks$preventXpPopOnTake(ServerPlayer player, CallbackInfo ci) {
        if (!net.blupillcosby.furnacexptweaks.XpUtils.isModSupportEnabled()) {
             // If mod support is off, only suppress for native vanilla furnaces
             boolean isNative = (Object)this instanceof net.minecraft.world.level.block.entity.FurnaceBlockEntity ||
                                (Object)this instanceof net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity ||
                                (Object)this instanceof net.minecraft.world.level.block.entity.SmokerBlockEntity;
             if (!isNative) return;
        }

        // We only want to award recipes, not pop experience or clear storage
        List<RecipeHolder<?>> recipesToAward = this.furnaceXpTweaks$getRecipesOnly();
        player.awardRecipes(recipesToAward);
        // Note: we don't clear recipesUsed here!
        ci.cancel();
    }

    @Inject(method = "getRecipesToAwardAndPopExperience", at = @At("HEAD"), cancellable = true)
    private void furnaceXpTweaks$preventXpPopOnBreak(ServerLevel level, Vec3 position, CallbackInfoReturnable<List<RecipeHolder<?>>> cir) {
        if (!net.blupillcosby.furnacexptweaks.XpUtils.isModSupportEnabled()) {
             // If mod support is off, only suppress for native vanilla furnaces
             boolean isNative = (Object)this instanceof net.minecraft.world.level.block.entity.FurnaceBlockEntity ||
                                (Object)this instanceof net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity ||
                                (Object)this instanceof net.minecraft.world.level.block.entity.SmokerBlockEntity;
             if (!isNative) return;
        }

        // When broken, we don't want to pop experience. 
        // The user wants XP to persist on break/replace, so we keep it in the NBT.
        cir.setReturnValue(Lists.newArrayList());
    }

    @Unique
    private List<RecipeHolder<?>> furnaceXpTweaks$getRecipesOnly() {
        List<RecipeHolder<?>> recipesToAward = Lists.newArrayList();
        if (this.level.recipeAccess() instanceof RecipeManager recipeManager) {
            for (Reference2IntMap.Entry<ResourceKey<Recipe<?>>> entry : this.recipesUsed.reference2IntEntrySet()) {
                recipeManager.byKey(entry.getKey()).ifPresent(recipesToAward::add);
            }
        }
        return recipesToAward;
    }

    @Override
    public void furnaceXpTweaks$claimXp(ServerPlayer player) {
        XpUtils.claimXp(player, this);
    }

    @Override
    public Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> furnaceXpTweaks$getRecipesUsed() {
        return this.recipesUsed;
    }
}
