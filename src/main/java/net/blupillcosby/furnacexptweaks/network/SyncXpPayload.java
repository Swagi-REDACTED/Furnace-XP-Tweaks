package net.blupillcosby.furnacexptweaks.network;

import net.blupillcosby.furnacexptweaks.FurnaceXPTweaks;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server-to-client response containing the exact XP points for a block entity.
 */
public record SyncXpPayload(BlockPos pos, double xp) implements CustomPacketPayload {
    public static final Type<SyncXpPayload> TYPE = new Type<>(FurnaceXPTweaks.id("sync_xp_universal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncXpPayload> CODEC = CustomPacketPayload.codec(SyncXpPayload::write, SyncXpPayload::new);

    private SyncXpPayload(RegistryFriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readDouble());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeDouble(xp);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
