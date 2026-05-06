package net.blupillcosby.furnacexptweaks.mixin.client;

import net.blupillcosby.furnacexptweaks.access.AbstractFurnaceMenuAccessorExtra;
import net.blupillcosby.furnacexptweaks.client.XpButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Robust XP button injector.
 * Targets AbstractContainerScreen.extractRenderState() instead of init() or render().
 * In MC 26.1, extractRenderState is the primary method for processing GUI state.
 * Using a boolean flag ensures setup (TR button suppression and XP button addition) happens exactly once per screen.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ScreenMixin {

    @Inject(method = "init()V", at = @At("TAIL"))
    private void furnaceXpTweaks$onInit(CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        AbstractContainerMenu menu = screen.getMenu();
        if (menu == null) return;
        
        BlockPos pos = furnaceXpTweaks$findBlockPos(menu);

        if (!furnaceXpTweaks$isFurnaceLike(screen, menu, pos)) return;

        // 1. Suppression of native mod buttons
        ScreenAccessor sa = (ScreenAccessor) this;
        sa.furnaceXpTweaks$getRenderables().removeIf(r -> {
            String name = r.getClass().getName();
            return name.contains("XpButtonWidget") || name.contains("ExperienceButton") || name.contains("XpButton");
        });
        sa.furnaceXpTweaks$getChildren().removeIf(c -> {
            String name = c.getClass().getName();
            return name.contains("XpButtonWidget") || name.contains("ExperienceButton") || name.contains("XpButton");
        });

        // 2. Dynamic Slot Detection
        int[] out = furnaceXpTweaks$findOutputSlotPos(menu);
        
        // 3. Button Placement
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
        int left = acc.furnaceXpTweaks$getLeftPos();
        int top = acc.furnaceXpTweaks$getTopPos();

        // Standard offsets
        int offsetX = net.blupillcosby.furnacexptweaks.FurnaceXPTweaks.XP_BUTTON_X_OFFSET;
        int offsetY = net.blupillcosby.furnacexptweaks.FurnaceXPTweaks.XP_BUTTON_Y_OFFSET;

        // Default furnace pos: (116, 35). With +30 offset: (146, 36)
        // If we found a slot, we use its position as base, otherwise we use config offsets as absolute from left/top
        int btnX = out != null ? left + out[0] + 30 : left + offsetX;
        int btnY = out != null ? top + out[1] + 1 : top + offsetY;

        sa.furnaceXpTweaks$addRenderableWidget(new XpButton(btnX, btnY, screen::getMenu, pos));
    }

    @Unique
    private boolean furnaceXpTweaks$isFurnaceLike(AbstractContainerScreen<?> screen, AbstractContainerMenu menu, BlockPos pos) {
        boolean isVanilla = menu instanceof AbstractFurnaceMenu;
        
        // If mod support is off, only allow the button for vanilla AbstractFurnaceMenu screens
        if (!net.blupillcosby.furnacexptweaks.XpUtils.isModSupportEnabled()) {
            return isVanilla;
        }

        if (isVanilla) return true;

        if (pos != null) {
            try {
                String name = (String) menu.getClass().getMethod("getName").invoke(menu);
                if (name != null) {
                    String lo = name.toLowerCase();
                    if (lo.contains("furnace") || lo.contains("smelter") || lo.contains("smelting") || lo.contains("blast") || lo.contains("smoker")) return true;
                }
            } catch (Exception ignored) {}
        }

        String title = screen.getTitle().getString().toLowerCase();
        return title.contains("furnace") || title.contains("smelter") || title.contains("smelting") || title.contains("blast") || title.contains("smoker");
    }

    @Unique
    private BlockPos furnaceXpTweaks$findBlockPos(AbstractContainerMenu menu) {
        if (menu instanceof AbstractFurnaceMenuAccessorExtra acc) {
            BlockPos pos = acc.furnaceXpTweaks$getBlockPos();
            if (pos != null) return pos;
        }

        // Try to derive BlockPos from any slot's BlockEntity container
        try {
            Field f;
            try { f = AbstractContainerMenu.class.getDeclaredField("slots"); f.setAccessible(true); }
            catch (NoSuchFieldException e) { f = AbstractContainerMenu.class.getField("slots"); }
            
            @SuppressWarnings("unchecked")
            List<Slot> slots = (List<Slot>) f.get(menu);
            if (slots != null) {
                for (Slot slot : slots) {
                    if (slot.container instanceof net.minecraft.world.level.block.entity.BlockEntity be) {
                        return be.getBlockPos();
                    }
                }
            }
        } catch (Exception ignored) {}

        // Fallback: Use reflection to scan the menu for BlockEntity or BlockPos fields
        Class<?> clazz = menu.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                try {
                    Object val = f.get(menu);
                    if (val instanceof net.minecraft.world.level.block.entity.BlockEntity be) {
                        return be.getBlockPos();
                    } else if (val instanceof BlockPos bp) {
                        return bp;
                    }
                } catch (Exception ignored) {}
            }
            clazz = clazz.getSuperclass();
        }

        return null;
    }

    @Unique
    private int[] furnaceXpTweaks$findOutputSlotPos(AbstractContainerMenu menu) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return null;
            Container playerInv = mc.player.getInventory();

            Field f;
            try { f = AbstractContainerMenu.class.getDeclaredField("slots"); f.setAccessible(true); }
            catch (NoSuchFieldException e) { f = AbstractContainerMenu.class.getField("slots"); }

            @SuppressWarnings("unchecked")
            List<Slot> slots = (List<Slot>) f.get(menu);
            if (slots == null || slots.isEmpty()) return null;

            // Priority 1: AbstractFurnaceMenu known index
            if (menu instanceof AbstractFurnaceMenu && slots.size() >= 3) {
                Slot out = slots.get(2);
                return new int[]{out.x, out.y};
            }

            Slot bestSlot = null;
            for (Slot slot : slots) {
                if (slot.container == playerInv) continue;
                if (!slot.isActive()) continue;

                // Priority 1: Specifically marked output slots (via SlotExtra flag)
                if (slot instanceof net.blupillcosby.furnacexptweaks.access.SlotExtra extra && extra.furnaceXpTweaks$isOutput()) {
                    bestSlot = slot;
                    break; 
                }

                // Priority 2: Heuristic-based detection (fallback)
                boolean isLikelyOutput = !slot.mayPlace(ItemStack.EMPTY);
                
                // Check for common Output slot class names
                String slotClassName = slot.getClass().getName();
                if (slotClassName.toLowerCase().contains("output") || slotClassName.toLowerCase().contains("result")) {
                    isLikelyOutput = true;
                }

                if (isLikelyOutput) {
                    if (bestSlot == null || slot.x > bestSlot.x) {
                        bestSlot = slot;
                    } else if (slot.x == bestSlot.x && Math.abs(slot.y - 35) < Math.abs(bestSlot.y - 35)) {
                        bestSlot = slot;
                    }
                }
            }

            if (bestSlot == null) {
                for (Slot slot : slots) {
                    if (slot.container == playerInv) continue;
                    if (bestSlot == null || slot.x > bestSlot.x) bestSlot = slot;
                }
            }

            if (bestSlot != null) {
                return new int[]{bestSlot.x, bestSlot.y};
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
