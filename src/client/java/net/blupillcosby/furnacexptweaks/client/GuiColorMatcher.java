package net.blupillcosby.furnacexptweaks.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.blupillcosby.furnacexptweaks.FurnaceXPTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class GuiColorMatcher {
    private static Identifier matchedExpId = null;
    private static Identifier matchedNoExpId = null;
    private static DynamicTexture dynamicExpTexture = null;
    private static DynamicTexture dynamicNoExpTexture = null;

    private static int topLeftColor = 0xFF373737;
    private static int bottomRightColor = 0xFFFFFFFF;
    private static int centerColor = 0xFF8B8B8B;
    private static boolean initialized = false;

    public static void init() {
        if (initialized && (!FurnaceXPTweaks.CONFIG.matchGuiColors.get() || matchedExpId != null)) return;
        
        initialized = true; // Mark as initialized so we don't spam if it fails
        
        if (!FurnaceXPTweaks.CONFIG.matchGuiColors.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ResourceManager rm = mc.getResourceManager();
        
        // Analyze the slot sprite as requested
        Identifier slotSpriteId = Identifier.withDefaultNamespace("textures/gui/sprites/container/slot.png");

        try {
            Optional<Resource> slotRes = rm.getResource(slotSpriteId);
            if (slotRes.isPresent()) {
                try (InputStream stream = slotRes.get().open()) {
                    NativeImage slotImg = NativeImage.read(stream);
                    // Sample corners and center for backplate
                    topLeftColor = slotImg.getPixel(0, 0);
                    bottomRightColor = slotImg.getPixel(slotImg.getWidth() - 1, slotImg.getHeight() - 1);
                    centerColor = slotImg.getPixel(1, 1);
                    slotImg.close();
                }
            }

            // Perform recoloring for both textures
            matchedExpId = recolorTexture(rm, FurnaceXPTweaks.id("textures/gui/exp.png"), "matched_exp");
            matchedNoExpId = recolorTexture(rm, FurnaceXPTweaks.id("textures/gui/no_exp.png"), "matched_no_exp");

        } catch (IOException e) {
            FurnaceXPTweaks.LOGGER.error("Failed to sample and match GUI colors", e);
        }
        initialized = true;
    }

    private static Identifier recolorTexture(ResourceManager rm, Identifier sourceId, String dynamicName) throws IOException {
        Optional<Resource> res = rm.getResource(sourceId);
        if (res.isEmpty()) return sourceId;

        try (InputStream stream = res.get().open()) {
            NativeImage sourceImg = NativeImage.read(stream);
            int width = sourceImg.getWidth();
            int height = sourceImg.getHeight();
            NativeImage resultImg = new NativeImage(width, height, true);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = sourceImg.getPixel(x, y);
                    
                    // Preserve transparency
                    if (net.minecraft.util.ARGB.alpha(pixel) < 5) {
                        resultImg.setPixel(x, y, 0);
                        continue;
                    }

                    // Recolor based on user mapping
                    // #ffffff (White/Highlight) -> bottomRightColor
                    if (colorMatch(pixel, 0xFFFFFFFF)) {
                        resultImg.setPixel(x, y, bottomRightColor);
                    } 
                    // #373737 (Dark/Shadow) -> topLeftColor
                    else if (colorMatch(pixel, 0xFF373737)) {
                        resultImg.setPixel(x, y, topLeftColor);
                    }
                    // #8b8b8b (Gray/Background) -> centerColor
                    else if (colorMatch(pixel, 0xFF8B8B8B)) {
                        resultImg.setPixel(x, y, centerColor);
                    }
                    else {
                        resultImg.setPixel(x, y, pixel);
                    }
                }
            }

            DynamicTexture dynamicTexture = new DynamicTexture(() -> dynamicName, resultImg);
            Identifier dynamicId = FurnaceXPTweaks.id(dynamicName);
            Minecraft.getInstance().getTextureManager().register(dynamicId, dynamicTexture);
            
            if (dynamicName.equals("matched_exp")) dynamicExpTexture = dynamicTexture;
            else dynamicNoExpTexture = dynamicTexture;

            sourceImg.close();
            return dynamicId;
        }
    }

    private static boolean colorMatch(int c1, int c2) {
        // Simple comparison ignoring alpha since we checked it already
        return (c1 & 0xFFFFFF) == (c2 & 0xFFFFFF);
    }

    public static Identifier getMatchedTexture(boolean hasExp) {
        if (!initialized || (FurnaceXPTweaks.CONFIG.matchGuiColors.get() && matchedExpId == null)) init();
        if (!FurnaceXPTweaks.CONFIG.matchGuiColors.get()) {
            return hasExp ? FurnaceXPTweaks.id("textures/gui/exp.png") : FurnaceXPTweaks.id("textures/gui/no_exp.png");
        }
        return hasExp ? (matchedExpId != null ? matchedExpId : FurnaceXPTweaks.id("textures/gui/exp.png")) 
                      : (matchedNoExpId != null ? matchedNoExpId : FurnaceXPTweaks.id("textures/gui/no_exp.png"));
    }

    public static void reset() {
        if (dynamicExpTexture != null) dynamicExpTexture.close();
        if (dynamicNoExpTexture != null) dynamicNoExpTexture.close();
        dynamicExpTexture = null;
        dynamicNoExpTexture = null;
        matchedExpId = null;
        matchedNoExpId = null;
        initialized = false;
    }
}
