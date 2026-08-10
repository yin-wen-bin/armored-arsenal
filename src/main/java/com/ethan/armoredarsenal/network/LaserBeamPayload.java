package com.ethan.armoredarsenal.network;

import com.ethan.armoredarsenal.ArmoredArsenal;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record LaserBeamPayload(
        long id,
        double startX,
        double startY,
        double startZ,
        double endX,
        double endY,
        double endZ,
        int color,
        float width,
        int lifetimeTicks) implements CustomPacketPayload {
    public static final Type<LaserBeamPayload> TYPE = new Type<>(ArmoredArsenal.id("laser_beam"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LaserBeamPayload> STREAM_CODEC =
            StreamCodec.ofMember(LaserBeamPayload::encode, LaserBeamPayload::decode);

    public static LaserBeamPayload between(long id, Vec3 start, Vec3 end, int color, float width, int lifetimeTicks) {
        return new LaserBeamPayload(
                id, start.x, start.y, start.z, end.x, end.y, end.z, color, width, lifetimeTicks);
    }

    public Vec3 start() {
        return new Vec3(startX, startY, startZ);
    }

    public Vec3 end() {
        return new Vec3(endX, endY, endZ);
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeLong(id);
        buffer.writeDouble(startX);
        buffer.writeDouble(startY);
        buffer.writeDouble(startZ);
        buffer.writeDouble(endX);
        buffer.writeDouble(endY);
        buffer.writeDouble(endZ);
        buffer.writeInt(color);
        buffer.writeFloat(width);
        buffer.writeVarInt(lifetimeTicks);
    }

    private static LaserBeamPayload decode(RegistryFriendlyByteBuf buffer) {
        return new LaserBeamPayload(
                buffer.readLong(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readInt(), buffer.readFloat(), buffer.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}