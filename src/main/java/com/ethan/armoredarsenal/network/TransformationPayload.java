package com.ethan.armoredarsenal.network;

import com.ethan.armoredarsenal.ArmoredArsenal;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TransformationPayload(int playerId, String entityType) implements CustomPacketPayload {
    public static final Type<TransformationPayload> TYPE = new Type<>(ArmoredArsenal.id("transformation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TransformationPayload> STREAM_CODEC =
            StreamCodec.ofMember(TransformationPayload::encode, TransformationPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(playerId);
        buffer.writeUtf(entityType);
    }

    private static TransformationPayload decode(RegistryFriendlyByteBuf buffer) {
        return new TransformationPayload(buffer.readVarInt(), buffer.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}