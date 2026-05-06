package net.blupillcosby.furnacexptweaks;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class ClientUtils {
    public static void drawTextured(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public static void drawTexturedScaled(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        // Correct order for 12-arg blit: RenderPipeline, Identifier, x, y, u, v, width, height, srcWidth, srcHeight, textureWidth, textureHeight
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
    }
}
