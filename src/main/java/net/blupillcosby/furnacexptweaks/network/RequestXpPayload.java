package net.blupillcosby.furnacexptweaks.network;

import net.blupillcosby.furnacexptweaks.FurnaceXPTweaks;
import net.blupillcosby.furnacexptweaks.XpUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Client-to-server request for the exact XP points stored in a block entity.
 * This is used for universal mod compatibility where synced menu slots are unavailable.
 */
public record RequestXpPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<RequestXpPayload> TYPE = new Type<>(FurnaceXPTweaks.id("request_xp"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestXpPayload> CODEC = CustomPacketPayload.codec(RequestXpPayload::write, RequestXpPayload::new);

    private RequestXpPayload(RegistryFriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestXpPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            BlockEntity be = context.player().level().getBlockEntity(payload.pos());
            double xp = XpUtils.getStoredXpFromBlockEntity(be);
            
            // Respond with the current exact XP
            ServerPlayNetworking.send(context.player(), new SyncXpPayload(payload.pos(), xp));
        });
    }
}
