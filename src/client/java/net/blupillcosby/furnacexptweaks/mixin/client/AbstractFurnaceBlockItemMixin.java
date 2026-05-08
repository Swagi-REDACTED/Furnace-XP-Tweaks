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

        // Display the item's inherent XP value (Levels and Points) relative to the player's current XP
        double playerXpExact = XpUtils.getPlayerExperienceExact(player);
        double combinedXp = playerXpExact + furnaceXpExact;
        double finalExactLevel = XpUtils.xpToLevel(combinedXp);
        int finalWholeLevel = (int) Math.floor(finalExactLevel);

        int levelsGained = finalWholeLevel - player.experienceLevel;
        int pointsGained;
        if (levelsGained > 0) {
            pointsGained = (int) (Math.floor(combinedXp) - XpUtils.getExperienceToLevelDouble(finalWholeLevel));
        } else {
            pointsGained = (int) (Math.floor(combinedXp) - Math.floor(playerXpExact));
        }

        tooltip.add(furnaceXpTweaks$tag("Levels", "+" + levelsGained, ChatFormatting.GOLD));
        tooltip.add(furnaceXpTweaks$tag("Points", "+" + pointsGained, ChatFormatting.GREEN));
    }

    /** 
     * Formats a sleek tooltip tag. 
     */
    @Unique
    private static Component furnaceXpTweaks$tag(String label, String value, ChatFormatting color) {
        return Component.literal(label + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(color));
    }

}
