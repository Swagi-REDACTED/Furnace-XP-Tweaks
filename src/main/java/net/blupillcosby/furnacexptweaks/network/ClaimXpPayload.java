package net.blupillcosby.furnacexptweaks.network;

import net.blupillcosby.furnacexptweaks.FurnaceXPTweaks;
import net.blupillcosby.furnacexptweaks.access.AbstractFurnaceBlockEntityAccessor;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

public record ClaimXpPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ClaimXpPayload> TYPE = new Type<>(FurnaceXPTweaks.id("claim_xp"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimXpPayload> CODEC = CustomPacketPayload.codec(ClaimXpPayload::write, ClaimXpPayload::new);

    private ClaimXpPayload(RegistryFriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClaimXpPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            BlockEntity be = context.player().level().getBlockEntity(payload.pos());
            if (be instanceof AbstractFurnaceBlockEntity furnace) {
                ((AbstractFurnaceBlockEntityAccessor) furnace).furnaceXpTweaks$claimXp(context.player());
            }
        });
    }
}
