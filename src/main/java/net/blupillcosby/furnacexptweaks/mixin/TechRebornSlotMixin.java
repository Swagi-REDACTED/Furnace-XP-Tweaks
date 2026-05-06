package net.blupillcosby.furnacexptweaks.mixin;

import net.blupillcosby.furnacexptweaks.access.SlotExtra;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "reborncore.common.screen.slot.SlotOutput", remap = false)
public abstract class TechRebornSlotMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void furnaceXpTweaks$markAsOutput(CallbackInfo ci) {
        if (this instanceof SlotExtra extra) {
            extra.furnaceXpTweaks$setOutput(true);
        }
    }
}
