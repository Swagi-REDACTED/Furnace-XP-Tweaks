package net.blupillcosby.furnacexptweaks.client;

import net.blupillcosby.furnacexptweaks.FurnaceXPTweaks;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.server.packs.PackType;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class FurnaceXPTweaksClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            FurnaceXPTweaks.id("gui_color_reload"),
            (ResourceManagerReloadListener) manager -> {
                GuiColorMatcher.reset();
            }
        );
    }
}
