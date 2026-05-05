package net.blupillcosby.furnacexptweaks.client;

import net.blupillcosby.furnacexptweaks.ClientUtils;
import net.blupillcosby.furnacexptweaks.FurnaceXPTweaks;
import net.blupillcosby.furnacexptweaks.XpUtils;
import net.blupillcosby.furnacexptweaks.access.AbstractFurnaceMenuAccessorExtra;
import net.blupillcosby.furnacexptweaks.network.ClaimXpPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractFurnaceMenu;

import java.util.function.Supplier;

public class XpButton extends AbstractWidget {
    private final Supplier<AbstractFurnaceMenu> menuSupplier;

    public XpButton(int x, int y, Supplier<AbstractFurnaceMenu> menuSupplier) {
        super(x, y, 16, 16, Component.empty());
        this.menuSupplier = menuSupplier;
    }

    @Override
    public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        AbstractFurnaceMenu menu = menuSupplier.get();
        if (menu instanceof AbstractFurnaceMenuAccessorExtra menuExtra) {
            BlockPos pos = menuExtra.furnaceXpTweaks$getBlockPos();
            if (pos != null) {
                ClientPlayNetworking.send(new ClaimXpPayload(pos));
            }
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        AbstractFurnaceMenu menu = menuSupplier.get();
        if (menu instanceof AbstractFurnaceMenuAccessorExtra menuExtra) {
            double furnaceXpExact = menuExtra.furnaceXpTweaks$getStoredXpPoints();
            
            // Get the matched texture (recolored based on current resource pack)
            Identifier texture = GuiColorMatcher.getMatchedTexture(furnaceXpExact > 0);

            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();

            ClientUtils.drawTextured(graphics, texture, x, y, 0, 0, w, h, w, h);

            if (this.isHovered()) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null || furnaceXpExact <= 0) {
                    // Fallback to static display if no player
                    double exactLevel = XpUtils.xpToLevel(furnaceXpExact);
                    int wholeLevel = (int) Math.floor(exactLevel);
                    double pointsIntoLevel = furnaceXpExact - XpUtils.getExperienceToLevelDouble(wholeLevel);
                    this.setTooltip(Tooltip.create(Component.literal("Levels: " + wholeLevel + "\nPoints: " + furnaceXpTweaks$fmt(pointsIntoLevel))));
                } else {
                    // Synchronized Dynamic Display: Shows exactly what the player will land on
                    double playerXpExact = XpUtils.getPlayerExperienceExact(player);
                    double combinedXp = playerXpExact + furnaceXpExact;
                    
                    double finalExactLevel = XpUtils.xpToLevel(combinedXp);
                    int finalWholeLevel = (int) Math.floor(finalExactLevel);
                    int levelsGained = finalWholeLevel - player.experienceLevel;
                    double finalPointsIntoLevel = combinedXp - XpUtils.getExperienceToLevelDouble(finalWholeLevel);

                    // Perfectly synced with the inventory tag
                    this.setTooltip(Tooltip.create(Component.literal("Levels: +" + levelsGained + "\nPoints: " + furnaceXpTweaks$fmt(finalPointsIntoLevel))));
                }
            } else {
                this.setTooltip(null);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    /** 
     * Format to 1 decimal place with improved rounding (.5 and above rounds up).
     */
    private String furnaceXpTweaks$fmt(double value) {
        long shifted = Math.round(value * 10);
        long intPart = shifted / 10;
        int dec = (int)(shifted % 10);
        if (dec == 0) return String.valueOf(intPart);
        return intPart + "." + dec;
    }
}
