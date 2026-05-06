package net.blupillcosby.furnacexptweaks.client;

import net.blupillcosby.furnacexptweaks.FurnaceXPTweaks;
import net.blupillcosby.furnacexptweaks.XpUtils;
import net.blupillcosby.furnacexptweaks.network.SyncXpPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class FurnaceXPTweaksClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register the receiver here so we can access XpButton (Client-only)
        ClientPlayNetworking.registerGlobalReceiver(SyncXpPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                XpButton.updateReceivedXp(payload.pos(), payload.xp());
                
                // Also update the client-side BlockEntity if it exists, 
                // so modded menus pulling from BE fields see the change instantly.
                if (context.client().level != null) {
                    BlockEntity be = context.client().level.getBlockEntity(payload.pos());
                    if (be != null) {
                        XpUtils.setStoredXpInBlockEntity(be, payload.xp());
                    }
                }
            });
        });

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            FurnaceXPTweaks.id("gui_color_reload"),
            (ResourceManagerReloadListener) manager -> {
                GuiColorMatcher.reload(manager);
            }
        );
    }
}
