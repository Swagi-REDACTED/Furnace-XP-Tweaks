package net.blupillcosby.furnacexptweaks.mixin;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "reborncore.common.blockentity.MachineBaseBlockEntity", remap = false)
public abstract class TechRebornExperienceMixin implements net.blupillcosby.furnacexptweaks.access.TechRebornExperienceAccessor {

    @Unique
    public float furnaceXpTweaks$storedExperience = 0.0f;

    @Unique
    public float furnaceXpTweaks$getExperience() {
        return furnaceXpTweaks$storedExperience;
    }

    @Unique
    public void furnaceXpTweaks$setExperience(float experience) {
        this.furnaceXpTweaks$storedExperience = experience;
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void furnaceXpTweaks$loadExperience(ValueInput view, CallbackInfo ci) {
        if (!net.blupillcosby.furnacexptweaks.XpUtils.isModSupportEnabled()) return;
        this.furnaceXpTweaks$storedExperience = view.getFloatOr("furnacexptweaks:experience", 0.0f);
        if (this.furnaceXpTweaks$storedExperience == 0) {
            this.furnaceXpTweaks$storedExperience = view.getFloatOr("Experience", 0.0f);
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void furnaceXpTweaks$saveExperience(ValueOutput view, CallbackInfo ci) {
        if (net.blupillcosby.furnacexptweaks.XpUtils.isModSupportEnabled()) {
            view.putFloat("furnacexptweaks:experience", this.furnaceXpTweaks$storedExperience);
        }
    }

}
