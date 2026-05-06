package net.blupillcosby.furnacexptweaks.mixin;

import net.blupillcosby.furnacexptweaks.access.TechRebornExperienceAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.level.block.entity.BlockEntity.class)
public abstract class UniversalBlockEntityMixin implements TechRebornExperienceAccessor {

    @Unique
    private float furnaceXpTweaks$universalExperience = 0.0f;

    @Override
    @Unique
    public float furnaceXpTweaks$getExperience() {
        return furnaceXpTweaks$universalExperience;
    }

    @Override
    @Unique
    public void furnaceXpTweaks$setExperience(float experience) {
        this.furnaceXpTweaks$universalExperience = experience;
    }

    /**
     * Ensures that our custom experience value is loaded from the BlockEntity's data.
     * This works for any BlockEntity in the game, providing a universal storage layer.
     */
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void furnaceXpTweaks$loadUniversalXp(ValueInput input, CallbackInfo ci) {
        if (!net.blupillcosby.furnacexptweaks.XpUtils.isModSupportEnabled()) return;
        this.furnaceXpTweaks$universalExperience = input.getFloatOr("furnacexptweaks:experience", 0.0f);
    }

    /**
     * Ensures that our custom experience value is saved into the BlockEntity's data.
     * This makes the experience persistent across world reloads and block break/place.
     */
    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void furnaceXpTweaks$saveUniversalXp(ValueOutput output, CallbackInfo ci) {
        if (net.blupillcosby.furnacexptweaks.XpUtils.isModSupportEnabled() && this.furnaceXpTweaks$universalExperience > 0) {
            output.putFloat("furnacexptweaks:experience", this.furnaceXpTweaks$universalExperience);
        }
    }
}
