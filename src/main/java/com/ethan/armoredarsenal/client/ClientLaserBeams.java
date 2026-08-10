package com.ethan.armoredarsenal.client;

import com.ethan.armoredarsenal.ArmoredArsenal;
import com.ethan.armoredarsenal.network.LaserBeamPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

public final class ClientLaserBeams {
    private static final ContextKey<RenderState> RENDER_STATE =
            new ContextKey<>(ArmoredArsenal.id("laser_beams"));
    private static final Map<Long, Beam> BEAMS = new HashMap<>();

    public static void accept(LaserBeamPayload payload) {
        BEAMS.put(payload.id(), new Beam(
                payload.start(), payload.end(), payload.color(), payload.width(), payload.lifetimeTicks()));
    }

    public static void clientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            BEAMS.clear();
            return;
        }
        BEAMS.replaceAll((id, beam) -> beam.tick());
        BEAMS.values().removeIf(Beam::expired);
    }

    public static void extract(ExtractLevelRenderStateEvent event) {
        event.getRenderState().setRenderData(
                RENDER_STATE,
                new RenderState(List.copyOf(BEAMS.values()), event.getCamera().position()));
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        RenderState state = event.getLevelRenderState().getRenderData(RENDER_STATE);
        if (state == null || state.beams().isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        for (Beam beam : state.beams()) {
            Vec3 delta = beam.end().subtract(beam.start());
            if (delta.lengthSqr() < 0.0001D) {
                continue;
            }

            Vec3 relativeStart = beam.start().subtract(state.cameraPosition());
            poseStack.pushPose();
            poseStack.translate(relativeStart.x, relativeStart.y, relativeStart.z);
            submitPrism(collector, poseStack, delta, beam.width(), beam.color());
            submitPrism(collector, poseStack, delta, beam.width() * 0.34F, 0xFFFFFFFF);
            poseStack.popPose();
        }
    }

    private static void submitPrism(
            SubmitNodeCollector collector, PoseStack poseStack, Vec3 delta, float width, int color) {
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.lightning(),
                (pose, buffer) -> renderPrism(pose, buffer, delta, width, color));
    }

    private static void renderPrism(
            PoseStack.Pose pose, VertexConsumer buffer, Vec3 delta, float width, int color) {
        Vec3 axis = delta.normalize();
        Vec3 reference = Math.abs(axis.y) < 0.9D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 sideA = axis.cross(reference).normalize().scale(width * 0.5D);
        Vec3 sideB = axis.cross(sideA).normalize().scale(width * 0.5D);

        Vec3[] start = new Vec3[] {
                sideA.add(sideB), sideA.subtract(sideB), sideA.scale(-1.0D).subtract(sideB), sideB.subtract(sideA)
        };
        Vec3[] end = new Vec3[] {
                start[0].add(delta), start[1].add(delta), start[2].add(delta), start[3].add(delta)
        };

        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            quad(buffer, pose, start[i], start[next], end[next], end[i], color);
        }
        quad(buffer, pose, start[3], start[2], start[1], start[0], color);
        quad(buffer, pose, end[0], end[1], end[2], end[3], color);
    }

    private static void quad(
            VertexConsumer buffer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        vertex(buffer, pose, a, color);
        vertex(buffer, pose, b, color);
        vertex(buffer, pose, c, color);
        vertex(buffer, pose, d, color);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Vec3 point, int color) {
        buffer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z).setColor(color);
    }

    private record Beam(Vec3 start, Vec3 end, int color, float width, int ticksRemaining) {
        private Beam tick() {
            return new Beam(start, end, color, width, ticksRemaining - 1);
        }

        private boolean expired() {
            return ticksRemaining <= 0;
        }
    }

    private record RenderState(List<Beam> beams, Vec3 cameraPosition) {}

    private ClientLaserBeams() {}
}