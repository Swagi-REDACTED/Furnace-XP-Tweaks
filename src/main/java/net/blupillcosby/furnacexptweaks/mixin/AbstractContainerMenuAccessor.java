package net.blupillcosby.furnacexptweaks.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
    @Invoker("addDataSlot")
    DataSlot furnaceXpTweaks$invokeAddDataSlot(DataSlot slot);
}
