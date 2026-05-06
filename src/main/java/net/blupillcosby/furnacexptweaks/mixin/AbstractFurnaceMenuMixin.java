package net.blupillcosby.furnacexptweaks.mixin;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.blupillcosby.furnacexptweaks.XpUtils;
import net.blupillcosby.furnacexptweaks.access.AbstractFurnaceBlockEntityAccessor;
import net.blupillcosby.furnacexptweaks.access.AbstractFurnaceMenuAccessorExtra;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceMenu.class)
public abstract class AbstractFurnaceMenuMixin implements AbstractFurnaceMenuAccessorExtra {

    @Shadow
    private Container container;

    /**
     * Shared int[] storage for syncing double XP to the client via DataSlots.
     * Since DataSlot values are capped at 16-bit by the network protocol,
     * we use four slots to store a 64-bit double (split as 4 x 16 bits).
     */
    @Unique
    private final int[] furnaceXpTweaks$xpDataRaw = new int[4];

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/inventory/RecipeBookType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",
            at = @At("TAIL"))
    private void furnaceXpTweaks$addCustomDataSlots(CallbackInfo ci) {
        AbstractContainerMenuAccessor menuAccessor = (AbstractContainerMenuAccessor) (Object) this;
        
        // Sync 64-bit double XP using 4 x 16-bit slots
        for (int i = 0; i < 4; i++) {
            final int index = i;
            menuAccessor.furnaceXpTweaks$invokeAddDataSlot(new DataSlot() {
                @Override
                public int get() {
                    long bits = Double.doubleToRawLongBits(furnaceXpTweaks$getStoredXpPointsInternal());
                    return (int) (bits >> (index * 16)) & 0xFFFF;
                }
                @Override
                public void set(int value) {
                    furnaceXpTweaks$xpDataRaw[index] = value & 0xFFFF;
                }
            });
        }

        // BlockPos slots (X, Y, Z)
        menuAccessor.furnaceXpTweaks$invokeAddDataSlot(new DataSlot() {
            @Override
            public int get() { return container instanceof AbstractFurnaceBlockEntity be ? be.getBlockPos().getX() : 0; }
            @Override
            public void set(int value) { furnaceXpTweaks$syncedX = value; furnaceXpTweaks$updateSyncedPos(); }
        });
        menuAccessor.furnaceXpTweaks$invokeAddDataSlot(new DataSlot() {
            @Override
            public int get() { return container instanceof AbstractFurnaceBlockEntity be ? be.getBlockPos().getY() : 0; }
            @Override
            public void set(int value) { furnaceXpTweaks$syncedY = value; furnaceXpTweaks$updateSyncedPos(); }
        });
        menuAccessor.furnaceXpTweaks$invokeAddDataSlot(new DataSlot() {
            @Override
            public int get() { return container instanceof AbstractFurnaceBlockEntity be ? be.getBlockPos().getZ() : 0; }
            @Override
            public void set(int value) { furnaceXpTweaks$syncedZ = value; furnaceXpTweaks$updateSyncedPos(); }
        });
    }

    @Unique private int furnaceXpTweaks$syncedX, furnaceXpTweaks$syncedY, furnaceXpTweaks$syncedZ;
    @Unique private BlockPos furnaceXpTweaks$syncedPos;

    @Unique
    private void furnaceXpTweaks$updateSyncedPos() {
        this.furnaceXpTweaks$syncedPos = new BlockPos(furnaceXpTweaks$syncedX, furnaceXpTweaks$syncedY, furnaceXpTweaks$syncedZ);
    }

    /**
     * Internal method to get the current XP points as double.
     * On server: calculates from BlockEntity using Exact method.
     * On client: reconstructs value from 4 synced 16-bit halves.
     */
    @Unique
    private double furnaceXpTweaks$getStoredXpPointsInternal() {
        if (!(container instanceof AbstractFurnaceBlockEntity furnaceBlockEntity)) {
            // Client side: reconstruct 64-bit bits from four 16-bit fragments
            long bits = 0;
            for (int i = 0; i < 4; i++) {
                bits |= ((long) (furnaceXpTweaks$xpDataRaw[i] & 0xFFFF)) << (i * 16);
            }
            return Double.longBitsToDouble(bits);
        }

        // Server side: calculate exact double from recipes + universal field
        double totalXp = 0;
        
        // 1. Universal field (fractional remainders)
        if (net.blupillcosby.furnacexptweaks.XpUtils.isModSupportEnabled() && furnaceBlockEntity instanceof net.blupillcosby.furnacexptweaks.access.TechRebornExperienceAccessor acc) {
            totalXp += acc.furnaceXpTweaks$getExperience();
        }

        // 2. Recipe map
        Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed =
                ((AbstractFurnaceBlockEntityAccessor) furnaceBlockEntity).furnaceXpTweaks$getRecipesUsed();

        if (furnaceBlockEntity.getLevel().recipeAccess() instanceof RecipeManager recipeManager) {
            totalXp += XpUtils.calculateXpFromRecipesExact(recipesUsed, recipeManager);
        }
        return totalXp;
    }

    @Override
    public double furnaceXpTweaks$getStoredXpPoints() {
        return furnaceXpTweaks$getStoredXpPointsInternal();
    }

    @Override
    public BlockPos furnaceXpTweaks$getBlockPos() {
        if (container instanceof AbstractFurnaceBlockEntity furnaceBlockEntity) {
            return furnaceBlockEntity.getBlockPos();
        }
        return furnaceXpTweaks$syncedPos;
    }
}
