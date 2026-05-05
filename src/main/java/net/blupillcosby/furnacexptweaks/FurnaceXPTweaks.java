package net.blupillcosby.furnacexptweaks;

import net.blupillcosby.furnacexptweaks.config.ModConfig;
import net.blupillcosby.furnacexptweaks.network.ClaimXpPayload;
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

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Furnace XP Tweaks!");
        CONFIG = me.fzzyhmstrs.fzzy_config.api.ConfigApiJava.registerAndLoadConfig(ModConfig::new);
        
        PayloadTypeRegistry.serverboundPlay().register(ClaimXpPayload.TYPE, ClaimXpPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ClaimXpPayload.TYPE, ClaimXpPayload::handle);
    }
}
