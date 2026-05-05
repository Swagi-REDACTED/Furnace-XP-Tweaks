package net.blupillcosby.furnacexptweaks.mixin.client;

import net.blupillcosby.furnacexptweaks.XpUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(Item.class)
public abstract class AbstractFurnaceBlockItemMixin {

    /** Key for the pre-calculated double XP embedded server-side. */
    @Unique
    private static final String FURNACE_XP_TWEAKS$XP_KEY = "furnacexptweaks:stored_xp";

    /**
     * Injects at the HEAD of appendHoverText to ensure XP tags appear directly below the item name.
     * Displays potential level gain and resulting fractional points using 100% mathematical accuracy.
     */
    @Inject(method = "appendHoverText", at = @At("HEAD"))
    private void furnaceXpTweaks$appendXpTooltip(
            ItemStack stack, Item.TooltipContext context,
            TooltipDisplay display, Consumer<Component> builder,
            TooltipFlag flag, CallbackInfo ci) {

        // Only process furnaces with block entity data
        if (!(stack.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof AbstractFurnaceBlock)) return;
        
        TypedEntityData<?> entityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (entityData == null) return;

        CompoundTag fullTag = entityData.copyTagWithoutId();

        // Retrieve pre-calculated double XP (set on break)
        if (!fullTag.contains(FURNACE_XP_TWEAKS$XP_KEY)) return;
        double furnaceXpExact = fullTag.getDouble(FURNACE_XP_TWEAKS$XP_KEY).orElse(0.0);
        
        if (furnaceXpExact <= 0) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            // Static display: shows how much XP is in the furnace (Levels and Overflow)
            double exactLevel = XpUtils.xpToLevel(furnaceXpExact);
            int wholeLevel = (int) Math.floor(exactLevel);
            double pointsIntoLevel = furnaceXpExact - XpUtils.getExperienceToLevelDouble(wholeLevel);
            
            builder.accept(furnaceXpTweaks$tag("Levels", "+" + wholeLevel, ChatFormatting.GOLD));
            builder.accept(furnaceXpTweaks$tag("Points", furnaceXpTweaks$fmt(pointsIntoLevel), ChatFormatting.GREEN));
            return;
        }

        // Reconstruct player's total XP as an exact double
        double playerXpExact = XpUtils.getPlayerExperienceExact(player);

        // Combined XP after harvest
        double combinedXp = playerXpExact + furnaceXpExact;
        
        // Final resulting level and points using inverse quadratic formula
        double finalExactLevel = XpUtils.xpToLevel(combinedXp);
        int finalWholeLevel = (int) Math.floor(finalExactLevel);
        int levelsGained = finalWholeLevel - player.experienceLevel;
        double finalPointsIntoLevel = combinedXp - XpUtils.getExperienceToLevelDouble(finalWholeLevel);

        // Render sleek, enchantment-style tags
        builder.accept(furnaceXpTweaks$tag("Levels", "+" + levelsGained, ChatFormatting.GOLD));
        builder.accept(furnaceXpTweaks$tag("Points", furnaceXpTweaks$fmt(finalPointsIntoLevel), ChatFormatting.GREEN));
    }

    /** 
     * Formats a sleek tooltip tag. 
     */
    @Unique
    private static Component furnaceXpTweaks$tag(String label, String value, ChatFormatting color) {
        return Component.literal(label + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(color));
    }

    /** 
     * Format to 1 decimal place with improved rounding (.5 and above rounds up).
     */
    @Unique
    private static String furnaceXpTweaks$fmt(double value) {
        long shifted = Math.round(value * 10);
        long intPart = shifted / 10;
        int dec = (int)(shifted % 10);
        if (dec == 0) return String.valueOf(intPart);
        return intPart + "." + dec;
    }
}
