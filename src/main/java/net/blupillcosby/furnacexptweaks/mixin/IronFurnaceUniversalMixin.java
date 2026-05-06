package net.blupillcosby.furnacexptweaks.mixin;

import net.blupillcosby.furnacexptweaks.access.TechRebornExperienceAccessor;
import net.blupillcosby.furnacexptweaks.XpUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(targets = "ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase", remap = false)
public abstract class IronFurnaceUniversalMixin implements TechRebornExperienceAccessor {

    @Unique
    private float furnaceXpTweaks$storedXp = 0.0f;

    @Override
    @Unique
    public float furnaceXpTweaks$getExperience() {
        return furnaceXpTweaks$storedXp;
    }

    @Override
    @Unique
    public void furnaceXpTweaks$setExperience(float experience) {
        this.furnaceXpTweaks$storedXp = experience;
    }

    @Inject(method = "setRecipeUsed", at = @At("TAIL"))
    private void furnaceXpTweaks$triggerSync(RecipeHolder<?> recipe, CallbackInfo ci) {
        if (!XpUtils.isModSupportEnabled()) return;
        // Trigger an immediate sync to client when an item is smelted
        XpUtils.syncXpToClients((BlockEntity)(Object)this);
    }

    @Inject(method = "unlockRecipes", at = @At("HEAD"), cancellable = true)
    private void furnaceXpTweaks$cancelAward(ServerPlayer player, CallbackInfo ci) {
        if (!XpUtils.isModSupportEnabled()) return;
        // Transfer XP from the map to our custom storage before it gets cleared
        BlockEntity be = (BlockEntity) (Object) this;
        // getStoredXpFromBlockEntity already includes furnaceXpTweaks$storedXp, 
        // so we assign the result to avoid double-adding.
        this.furnaceXpTweaks$storedXp = (float) XpUtils.getStoredXpFromBlockEntity(be);
        
        // Clear the map RIGHT NOW so the native award logic does nothing
        try {
            java.lang.reflect.Field recipesField = this.getClass().getField("recipes");
            Object map = recipesField.get(this);
            if (map instanceof java.util.Map) {
                ((java.util.Map<?, ?>) map).clear();
            }
        } catch (Exception ignored) {}
    }

    @Inject(method = "grantStoredRecipeExperience", at = @At("HEAD"), cancellable = true)
    private void furnaceXpTweaks$cancelOrbs(net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 pos, CallbackInfoReturnable<List<?>> cir) {
        if (!XpUtils.isModSupportEnabled()) return;
        cir.setReturnValue(new ArrayList<>());
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void furnaceXpTweaks$load(ValueInput input, CallbackInfo ci) {
        if (!XpUtils.isModSupportEnabled()) return;
        this.furnaceXpTweaks$storedXp = input.getFloatOr("furnacexptweaks:experience", 0.0f);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void furnaceXpTweaks$save(ValueOutput output, CallbackInfo ci) {
        if (XpUtils.isModSupportEnabled() && this.furnaceXpTweaks$storedXp > 0) {
            output.putFloat("furnacexptweaks:experience", this.furnaceXpTweaks$storedXp);
        }
    }
}
