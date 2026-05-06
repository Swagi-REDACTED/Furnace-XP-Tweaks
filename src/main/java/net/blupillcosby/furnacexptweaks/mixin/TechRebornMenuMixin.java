package net.blupillcosby.furnacexptweaks.mixin;

import net.blupillcosby.furnacexptweaks.access.AbstractFurnaceMenuAccessorExtra;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import java.lang.reflect.Field;

/**
 * Universal Menu Compatibility for TechReborn.
 * This mixin targets TechReborn's BuiltScreenHandler to provide the BlockPos 
 * and stored XP data needed for the universal XP button.
 */
@Pseudo
@Mixin(targets = "reborncore.common.screen.BuiltScreenHandler", remap = false)
public abstract class TechRebornMenuMixin extends AbstractContainerMenu implements AbstractFurnaceMenuAccessorExtra {

    protected TechRebornMenuMixin() {
        super(null, 0);
    }

    @Override
    public BlockPos furnaceXpTweaks$getBlockPos() {
        try {
            // BuiltScreenHandler has getPos()
            return (BlockPos) this.getClass().getMethod("getPos").invoke(this);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public double furnaceXpTweaks$getStoredXpPoints() {
        try {
            // BuiltScreenHandler has getBlockEntity()
            Object be = this.getClass().getMethod("getBlockEntity").invoke(this);
            if (be != null) {
                // Try to get 'experience' field (TechReborn specific) via hierarchy
                Class<?> clazz = be.getClass();
                while (clazz != null && clazz != Object.class) {
                    try {
                        Field expField = null;
                        try {
                            expField = clazz.getDeclaredField("experience");
                        } catch (NoSuchFieldException e) {
                            expField = clazz.getDeclaredField("furnaceXpTweaks$storedExperience");
                        }
                        expField.setAccessible(true);
                        return ((Number) expField.get(be)).doubleValue();
                    } catch (NoSuchFieldException e) {
                        clazz = clazz.getSuperclass();
                    } catch (Exception ignored) {
                        break;
                    }
                }
                
                // Fallback: Check if it has a getExperience method
                try {
                    return ((Number) be.getClass().getMethod("getExperience").invoke(be)).doubleValue();
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
