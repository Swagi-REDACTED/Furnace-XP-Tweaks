package net.blupillcosby.furnacexptweaks.mixin.client;

import net.blupillcosby.furnacexptweaks.XpUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class AbstractFurnaceBlockItemMixin {

    /**
     * Injects into getTooltipLines to ensure XP tags appear universally at the bottom of the tooltip.
     * This is the absolute final entry point before the tooltip is rendered.
     */
    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void furnaceXpTweaks$appendXpTooltip(
            net.minecraft.world.item.Item.TooltipContext context,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.item.TooltipFlag flag,
            CallbackInfoReturnable<List<Component>> cir) {

        ItemStack stack = (ItemStack) (Object) this;
        List<Component> tooltip = cir.getReturnValue();
        if (tooltip == null) return;
        
        // Use Universal XP extraction from the stack
        double furnaceXpExact = XpUtils.getStoredXpFromStack(stack);
        if (furnaceXpExact <= 0) return;

        // Display the item's inherent XP value (Levels and Points) independently of the player's XP
        double exactLevel = XpUtils.xpToLevel(furnaceXpExact);
        int wholeLevel = (int) Math.floor(exactLevel);
        double pointsIntoLevel = furnaceXpExact - XpUtils.getExperienceToLevelDouble(wholeLevel);
        
        tooltip.add(furnaceXpTweaks$tag("Levels", "+" + wholeLevel, ChatFormatting.GOLD));
        tooltip.add(furnaceXpTweaks$tag("Points", "+" + furnaceXpTweaks$fmt(pointsIntoLevel), ChatFormatting.GREEN));
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
