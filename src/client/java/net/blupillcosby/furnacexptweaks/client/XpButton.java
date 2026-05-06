package net.blupillcosby.furnacexptweaks.client;

import net.blupillcosby.furnacexptweaks.ClientUtils;
import net.blupillcosby.furnacexptweaks.FurnaceXPTweaks;
import net.blupillcosby.furnacexptweaks.XpUtils;
import net.blupillcosby.furnacexptweaks.access.AbstractFurnaceMenuAccessorExtra;
import net.blupillcosby.furnacexptweaks.network.ClaimXpPayload;
import net.blupillcosby.furnacexptweaks.network.RequestXpPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class XpButton extends AbstractWidget {
    private final Supplier<AbstractContainerMenu> menuSupplier;
    private final BlockPos blockPos;
    
    // Cache for XP received from the server (universal fallback)
    private static final Map<BlockPos, Double> CACHED_XP = new HashMap<>();
    private static long lastRequestTime = 0;

    // --- OFFSET FINE-TUNING ---
    // Use these to shift the icons within their 16x16 boundaries.
    public static int REGULAR_X_OFFSET = 0;
    public static int REGULAR_Y_OFFSET = 1; // Moved down slightly
    
    public static int ALT_X_OFFSET = 0;
    public static int ALT_Y_OFFSET = -1; // Moved up slightly
    // --------------------------

    public XpButton(int x, int y, Supplier<AbstractContainerMenu> menuSupplier, BlockPos blockPos) {
        super(x, y, 16, 16, Component.empty());
        this.menuSupplier = menuSupplier;
        this.blockPos = blockPos;
    }

    public static void updateReceivedXp(BlockPos pos, double xp) {
        CACHED_XP.put(pos, xp);
    }

    @Override
    public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double currentXp = 0;
        AbstractContainerMenu menu = menuSupplier.get();
        if (menu instanceof AbstractFurnaceMenuAccessorExtra menuExtra) {
            currentXp = menuExtra.furnaceXpTweaks$getStoredXpPoints();
        }
        if (currentXp < 1.0 && blockPos != null) {
            currentXp = CACHED_XP.getOrDefault(blockPos, 0.0);
        }

        if (currentXp < 1.0) return; // Don't send packet if not claimable

        if (blockPos != null) {
            double remainder = currentXp - Math.floor(currentXp);
            CACHED_XP.put(blockPos, remainder);
            
            // Optimistically update the client-side BlockEntity state
            if (Minecraft.getInstance().level != null) {
                BlockEntity be = Minecraft.getInstance().level.getBlockEntity(blockPos);
                if (be != null) {
                    XpUtils.setStoredXpInBlockEntity(be, remainder);
                }
            }
            
            ClientPlayNetworking.send(new ClaimXpPayload(blockPos));
        } else {
            // Fallback for menus where pos wasn't passed directly
            if (menu instanceof AbstractFurnaceMenuAccessorExtra menuExtra) {
                BlockPos pos = menuExtra.furnaceXpTweaks$getBlockPos();
                if (pos != null) {
                    double remainder = currentXp - Math.floor(currentXp);
                    CACHED_XP.put(pos, remainder);
                    
                    if (Minecraft.getInstance().level != null) {
                        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
                        if (be != null) {
                            XpUtils.setStoredXpInBlockEntity(be, remainder);
                        }
                    }
                    
                    ClientPlayNetworking.send(new ClaimXpPayload(pos));
                }
            }
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        double furnaceXpExact = 0;
        
        // Attempt 1: Try synced data slots (Fast, for vanilla/modded-extended furnaces)
        AbstractContainerMenu menu = menuSupplier.get();
        if (menu instanceof AbstractFurnaceMenuAccessorExtra menuExtra) {
            furnaceXpExact = menuExtra.furnaceXpTweaks$getStoredXpPoints();
        }
        
        // Attempt 2: Universal Fallback (For TechReborn/other custom menus)
        if (furnaceXpExact <= 0 && blockPos != null) {
            furnaceXpExact = CACHED_XP.getOrDefault(blockPos, 0.0);
            
            // Periodically request fresh data from server
            long now = System.currentTimeMillis();
            if (now - lastRequestTime > 500) { // Twice per second
                ClientPlayNetworking.send(new RequestXpPayload(blockPos));
                lastRequestTime = now;
            }
        }

        boolean hasExp = furnaceXpExact >= 1.0;
        boolean useAlt = FurnaceXPTweaks.CONFIG.useAltTextures.get();
        
        Identifier texture;
        int x = this.getX();
        int y = this.getY();

        if (useAlt) {
            texture = hasExp ? FurnaceXPTweaks.id("textures/gui/exp_alt.png") : GuiColorMatcher.getMatchedTextureAlt(false);
            
            // Default: draw full 32x32 texture scaled to 16x16
            float u = 0;
            float v = 0;
            int regionWidth = 32;
            int regionHeight = 32;
            int drawX = x;
            int drawY = y;
            int drawW = 16;
            int drawH = 16;
            
            if (!hasExp) {
                drawX = x + ALT_X_OFFSET;
                drawY = y + ALT_Y_OFFSET;
            } else {
                drawX = x + ALT_X_OFFSET;
                drawY = y + ALT_Y_OFFSET;
            }

            ClientUtils.drawTexturedScaled(graphics, texture, drawX, drawY, drawW, drawH, u, v, regionWidth, regionHeight, 32, 32);
        } else {
            texture = GuiColorMatcher.getMatchedTexture(hasExp);
            ClientUtils.drawTextured(graphics, texture, x + REGULAR_X_OFFSET, y + REGULAR_Y_OFFSET, 0, 0, 16, 16, 16, 16);
        }

        Minecraft mc = Minecraft.getInstance();
        if (this.isHovered()) {
            LocalPlayer player = mc.player;
            if (player == null || furnaceXpExact < 1.0) {
                double exactLevel = XpUtils.xpToLevel(furnaceXpExact);
                int wholeLevel = (int) Math.floor(exactLevel);
                double pointsIntoLevel = furnaceXpExact - XpUtils.getExperienceToLevelDouble(wholeLevel);
                this.setTooltip(Tooltip.create(Component.literal("Levels: " + wholeLevel + "\nPoints: " + (int) Math.floor(pointsIntoLevel))));
            } else {
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
                
                this.setTooltip(Tooltip.create(Component.literal("Levels: +" + levelsGained + "\nPoints: +" + pointsGained)));
            }
        } else {
            this.setTooltip(null);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    private String furnaceXpTweaks$fmt(double value) {
        long shifted = Math.round(value * 10);
        long intPart = shifted / 10;
        int dec = (int)(shifted % 10);
        if (dec == 0) return String.valueOf(intPart);
        return intPart + "." + dec;
    }
}
