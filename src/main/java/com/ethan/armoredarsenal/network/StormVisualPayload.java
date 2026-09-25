package com.ethan.armoredarsenal.network;

import com.ethan.armoredarsenal.ArmoredArsenal;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record StormVisualPayload(String dimension, double x, double y, double z,
                                 int deathTicks, boolean core) implements CustomPacketPayload {
    public static final Type<StormVisualPayload> TYPE = new Type<>(ArmoredArsenal.id("storm_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StormVisualPayload> STREAM_CODEC =
            StreamCodec.ofMember(StormVisualPayload::encode, StormVisualPayload::decode);

    public static StormVisualPayload at(String dimension, Vec3 position, int deathTicks, boolean core) {
        return new StormVisualPayload(dimension, position.x, position.y, position.z, deathTicks, core);
    }

    public Vec3 position() {
        return new Vec3(x, y, z);
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(dimension);
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeVarInt(deathTicks);
        buffer.writeBoolean(core);
    }

    private static StormVisualPayload decode(RegistryFriendlyByteBuf buffer) {
        return new StormVisualPayload(buffer.readUtf(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readVarInt(), buffer.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
