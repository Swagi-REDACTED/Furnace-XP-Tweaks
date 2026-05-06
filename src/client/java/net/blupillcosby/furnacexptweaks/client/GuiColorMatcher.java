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
    private static Identifier matchedNoExpAltId = null;
    
    private static DynamicTexture dynamicExpTexture = null;
    private static DynamicTexture dynamicNoExpTexture = null;
    private static DynamicTexture dynamicNoExpAltTexture = null;

    private static int topLeftColor = 0xFF373737;
    private static int bottomRightColor = 0xFFFFFFFF;
    private static int centerColor = 0xFF8B8B8B;
    private static boolean initialized = false;

    public static void init() {
        if (initialized && (!FurnaceXPTweaks.CONFIG.matchGuiColors.get() || matchedExpId != null)) return;
        
        initialized = true;
        
        if (!FurnaceXPTweaks.CONFIG.matchGuiColors.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ResourceManager rm = mc.getResourceManager();
        
        Identifier slotSpriteId = Identifier.withDefaultNamespace("textures/gui/sprites/container/slot.png");

        try {
            Optional<Resource> slotRes = rm.getResource(slotSpriteId);
            if (slotRes.isPresent()) {
                try (InputStream stream = slotRes.get().open()) {
                    NativeImage slotImg = NativeImage.read(stream);
                    topLeftColor = slotImg.getPixel(0, 0);
                    bottomRightColor = slotImg.getPixel(slotImg.getWidth() - 1, slotImg.getHeight() - 1);
                    centerColor = slotImg.getPixel(1, 1);
                    slotImg.close();
                }
            }

            matchedExpId = recolorTexture(rm, FurnaceXPTweaks.id("textures/gui/exp.png"), "matched_exp");
            matchedNoExpId = recolorTexture(rm, FurnaceXPTweaks.id("textures/gui/no_exp.png"), "matched_no_exp");
            matchedNoExpAltId = recolorTexture(rm, FurnaceXPTweaks.id("textures/gui/no_exp_alt.png"), "matched_no_exp_alt");

        } catch (IOException e) {
            FurnaceXPTweaks.LOGGER.error("Failed to sample and match GUI colors", e);
        }
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
                    
                    if (net.minecraft.util.ARGB.alpha(pixel) < 5) {
                        resultImg.setPixel(x, y, 0);
                        continue;
                    }

                    if (colorMatch(pixel, 0xFFFFFFFF)) {
                        resultImg.setPixel(x, y, bottomRightColor);
                    } 
                    else if (colorMatch(pixel, 0xFF373737)) {
                        resultImg.setPixel(x, y, topLeftColor);
                    }
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
            else if (dynamicName.equals("matched_no_exp")) dynamicNoExpTexture = dynamicTexture;
            else if (dynamicName.equals("matched_no_exp_alt")) dynamicNoExpAltTexture = dynamicTexture;

            sourceImg.close();
            return dynamicId;
        }
    }

    private static boolean colorMatch(int c1, int c2) {
        return (c1 & 0xFFFFFF) == (c2 & 0xFFFFFF);
    }

    public static Identifier getMatchedTexture(boolean hasExp) {
        if (!initialized) init();
        if (!FurnaceXPTweaks.CONFIG.matchGuiColors.get()) {
            return hasExp ? FurnaceXPTweaks.id("textures/gui/exp.png") : FurnaceXPTweaks.id("textures/gui/no_exp.png");
        }
        return hasExp ? (matchedExpId != null ? matchedExpId : FurnaceXPTweaks.id("textures/gui/exp.png")) 
                      : (matchedNoExpId != null ? matchedNoExpId : FurnaceXPTweaks.id("textures/gui/no_exp.png"));
    }

    public static Identifier getMatchedTextureAlt(boolean hasExp) {
        if (!initialized) init();
        if (!FurnaceXPTweaks.CONFIG.matchGuiColors.get()) {
            return hasExp ? FurnaceXPTweaks.id("textures/gui/exp_alt.png") : FurnaceXPTweaks.id("textures/gui/no_exp_alt.png");
        }
        return hasExp ? FurnaceXPTweaks.id("textures/gui/exp_alt.png") // User said exp_alt doesn't need matching
                      : (matchedNoExpAltId != null ? matchedNoExpAltId : FurnaceXPTweaks.id("textures/gui/no_exp_alt.png"));
    }

    public static void reload(ResourceManager manager) {
        reset();
    }

    public static void reset() {
        if (dynamicExpTexture != null) dynamicExpTexture.close();
        if (dynamicNoExpTexture != null) dynamicNoExpTexture.close();
        if (dynamicNoExpAltTexture != null) dynamicNoExpAltTexture.close();
        dynamicExpTexture = null;
        dynamicNoExpTexture = null;
        dynamicNoExpAltTexture = null;
        matchedExpId = null;
        matchedNoExpId = null;
        matchedNoExpAltId = null;
        initialized = false;
    }
}
