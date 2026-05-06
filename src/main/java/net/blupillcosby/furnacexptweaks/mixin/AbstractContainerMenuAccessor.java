package net.blupillcosby.furnacexptweaks.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
    @Invoker("addDataSlot")
    DataSlot furnaceXpTweaks$invokeAddDataSlot(DataSlot slot);

    @Accessor("slots")
    NonNullList<Slot> furnaceXpTweaks$getSlots();
}
