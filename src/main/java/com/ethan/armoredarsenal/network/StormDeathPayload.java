package com.ethan.armoredarsenal.network;

import com.ethan.armoredarsenal.ArmoredArsenal;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StormDeathPayload(int durationTicks) implements CustomPacketPayload {
    public static final Type<StormDeathPayload> TYPE = new Type<>(ArmoredArsenal.id("storm_death"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StormDeathPayload> STREAM_CODEC =
            StreamCodec.ofMember(StormDeathPayload::encode, StormDeathPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(durationTicks);
    }

    private static StormDeathPayload decode(RegistryFriendlyByteBuf buffer) {
        return new StormDeathPayload(buffer.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
