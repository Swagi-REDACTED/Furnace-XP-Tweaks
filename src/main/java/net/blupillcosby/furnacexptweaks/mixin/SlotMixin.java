package net.blupillcosby.furnacexptweaks.mixin;

import net.blupillcosby.furnacexptweaks.access.SlotExtra;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Slot.class)
public abstract class SlotMixin implements SlotExtra {
    @Unique
    private boolean furnaceXpTweaks$isOutput = false;

    @Override
    public boolean furnaceXpTweaks$isOutput() {
        return furnaceXpTweaks$isOutput;
    }

    @Override
    public void furnaceXpTweaks$setOutput(boolean value) {
        this.furnaceXpTweaks$isOutput = value;
    }
}
