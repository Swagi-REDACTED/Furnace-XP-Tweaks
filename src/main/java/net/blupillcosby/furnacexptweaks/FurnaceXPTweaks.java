package net.blupillcosby.furnacexptweaks;

import net.blupillcosby.furnacexptweaks.config.ModConfig;
import net.blupillcosby.furnacexptweaks.network.ClaimXpPayload;
import net.blupillcosby.furnacexptweaks.network.RequestXpPayload;
import net.blupillcosby.furnacexptweaks.network.SyncXpPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FurnaceXPTweaks implements ModInitializer {
    public static final String MOD_ID = "furnace_xp_tweaks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfig CONFIG;

    // Default offsets - removed from config UI for cleanliness
    public static final int XP_BUTTON_X_OFFSET = 146;
    public static final int XP_BUTTON_Y_OFFSET = 36;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Furnace XP Tweaks!");
        CONFIG = me.fzzyhmstrs.fzzy_config.api.ConfigApiJava.registerAndLoadConfig(ModConfig::new);
        
        PayloadTypeRegistry.serverboundPlay().register(ClaimXpPayload.TYPE, ClaimXpPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestXpPayload.TYPE, RequestXpPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncXpPayload.TYPE, SyncXpPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ClaimXpPayload.TYPE, ClaimXpPayload::handle);
        ServerPlayNetworking.registerGlobalReceiver(RequestXpPayload.TYPE, RequestXpPayload::handle);
    }
}
