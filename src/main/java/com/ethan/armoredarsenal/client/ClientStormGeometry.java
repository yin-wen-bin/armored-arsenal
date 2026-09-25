package com.ethan.armoredarsenal.client;

import com.ethan.armoredarsenal.ArmoredArsenal;
import com.ethan.armoredarsenal.content.StormShape;
import com.ethan.armoredarsenal.content.StormShape.HeadSpec;
import com.ethan.armoredarsenal.network.StormVisualPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

public final class ClientStormGeometry {
    private static final ContextKey<Snapshot> RENDER_STATE =
            new ContextKey<>(ArmoredArsenal.id("storm_geometry"));
    private static StormVisualPayload visual;
    private static int age;

    public static void accept(StormVisualPayload payload) {
        visual = payload;
        age = 0;
    }

    public static void clientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            visual = null;
        } else if (visual != null && ++age > 20) {
            visual = null;
        }
    }

    public static void extract(ExtractLevelRenderStateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (visual == null || minecraft.level == null
                || !visual.dimension().equals(minecraft.level.dimension().identifier().toString())) {
            return;
        }
        event.getRenderState().setRenderData(RENDER_STATE,
                new Snapshot(visual, age, event.getCamera().position()));
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        Snapshot snapshot = event.getLevelRenderState().getRenderData(RENDER_STATE);
        if (snapshot == null || snapshot.visual().position().distanceToSqr(snapshot.camera()) > 390.0 * 390.0) {
            return;
        }
        PoseStack stack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        Vec3 relative = snapshot.visual().position().subtract(snapshot.camera());
        stack.pushPose();
        stack.translate(relative.x, relative.y, relative.z);
        int death = snapshot.visual().deathTicks() == 0 ? 0
                : snapshot.visual().deathTicks() + snapshot.age();
        if (snapshot.visual().core()) {
            collector.submitCustomGeometry(stack, RenderTypes.debugQuads(),
                    (pose, buffer) -> renderCore(pose, buffer, death, snapshot.age()));
        } else {
            for (int group = 0; group < 3; group++) {
                int currentGroup = group;
                stack.pushPose();
                if (group == 0 && death > 20) {
                    float scale = Math.max(0.08F, 1.0F - (death - 20) / 76.0F);
                    stack.translate(0.0, 5.0, 5.0);
                    stack.scale(scale, scale, scale);
                    stack.translate(0.0, -5.0, -5.0);
                } else if (group != 0 && death > 8) {
                    float fall = Math.min(45.0F, (death - 8) * (death - 8) * 0.014F);
                    stack.translate((group == 1 ? -1 : 1) * fall * 0.10F, -fall, 0.0);
                }
                collector.submitCustomGeometry(stack, RenderTypes.debugQuads(),
                        (pose, buffer) -> renderGroup(pose, buffer, currentGroup, snapshot.age(), death));
                if (group != 0 && death == 0) {
                    collector.submitCustomGeometry(stack, RenderTypes.lightning(),
                            (pose, buffer) -> renderTractorCone(pose, buffer, currentGroup, snapshot.age()));
                }
                stack.popPose();
            }
            if (death > 18) {
                collector.submitCustomGeometry(stack, RenderTypes.debugQuads(),
                        (pose, buffer) -> renderFragments(pose, buffer, death));
            }
        }
        stack.popPose();
    }

    private static void renderGroup(PoseStack.Pose pose, VertexConsumer buffer, int group, int age, int death) {
        Vec3 center = group == 0 ? new Vec3(0, 5, 5)
                : new Vec3(group == 1 ? -16 : 16, 3, 2);
        Vec3 radii = group == 0 ? new Vec3(10, 9, 8) : new Vec3(7, 7, 7);
        ellipsoid(pose, buffer, center, radii, 0xFF16151C);
        if (group == 0) {
            ellipsoid(pose, buffer, new Vec3(0, 4, -3), new Vec3(3.1, 3.1, 0.24),
                    death > 0 ? 0xFFAD5FF3 : 0xFF45215C);
        }
        for (HeadSpec head : StormShape.HEADS) {
            if (head.group() == group) {
                face(pose, buffer, new Vec3(head.x(), head.y(), head.z()), 2.55, age);
            }
        }
        for (int side : new int[] {-1, 1}) {
            for (int front : new int[] {-1, 1}) {
                Vec3 previous = center.add(side * 3.5, -4.0, front * 4.0);
                for (int step = 1; step <= 8; step++) {
                    double t = step / 8.0;
                    Vec3 next = center.add(side * (3.5 + t * 12.0), -4.0 - t * 13.0,
                            front * (4.0 + t * 7.0) + Math.sin(t * 4.0 + age * 0.16 + side) * t * 1.3);
                    tube(pose, buffer, previous, next, 1.45 * (1.0 - t) + 0.18,
                            0xFF0F0E14);
                    previous = next;
                }
            }
        }
    }

    private static void renderCore(PoseStack.Pose pose, VertexConsumer buffer, int death, int age) {
        for (int side : new int[] {-1, 1}) {
            face(pose, buffer, new Vec3(side * 6.0, 4.8, 3.5), 2.25, age);
            for (int front : new int[] {-1, 1}) {
                Vec3 previous = new Vec3(side * 9.5, 5.5, 6.0 + front * 2.0);
                for (int step = 1; step <= 10; step++) {
                    double t = step / 10.0;
                    Vec3 next = new Vec3(side * (9.5 - t * 7.3),
                            5.5 - t * 3.8 + Math.sin(t * 5.5 + age * 0.12 + front) * 0.45,
                            6.0 + front * (2.0 + t * 4.0) - t * 6.5);
                    tube(pose, buffer, previous, next, 0.95 * (1.0 - t) + 0.13,
                            death > 0 ? 0xFF25202E : 0xFF0B0B10);
                    previous = next;
                }
            }
        }
    }

    private static void renderFragments(PoseStack.Pose pose, VertexConsumer buffer, int death) {
        double progress = Math.min(1.0, (death - 18) / 72.0);
        for (int piece = 0; piece < 18; piece++) {
            double angle = piece * 2.39996;
            double rise = Math.sin(piece * 1.87);
            double distance = 4.0 + progress * (11.0 + piece % 5 * 2.0);
            Vec3 center = new Vec3(Math.cos(angle) * distance,
                    5.0 + rise * (5.0 + progress * 9.0) - progress * progress * 12.0,
                    5.0 + Math.sin(angle) * distance);
            double radius = (0.45 + piece % 4 * 0.17) * (1.0 - progress * 0.85);
            ellipsoid(pose, buffer, center, new Vec3(radius, radius * 1.5, radius),
                    piece % 4 == 0 ? 0xFF6E348E : 0xFF17151C, 5, 8);
        }
    }

    private static void face(PoseStack.Pose pose, VertexConsumer buffer, Vec3 center, double radius, int age) {
        ellipsoid(pose, buffer, center, new Vec3(radius, radius * 0.9, radius), 0xFF0D0D13);
        for (int eye : new int[] {-1, 1}) {
            Vec3 eyeCenter = center.add(eye * radius * 0.43, radius * 0.27, -radius * 0.91);
            ellipsoid(pose, buffer, eyeCenter,
                    new Vec3(radius * 0.24, radius * 0.19, radius * 0.10), 0xFF7F35B9);
            ellipsoid(pose, buffer, eyeCenter.add(0, 0, -radius * 0.09),
                    new Vec3(radius * 0.13, radius * 0.12, radius * 0.05), 0xFFD9A0FF);
        }
        double mouthOpen = 0.36 + 0.06 * Math.sin(age * 0.24);
        ellipsoid(pose, buffer, center.add(0, -radius * 0.38, -radius * 0.93),
                new Vec3(radius * 0.50, radius * mouthOpen, radius * 0.11), 0xFF592278);
        ellipsoid(pose, buffer, center.add(0, -radius * 0.38, -radius * 1.025),
                new Vec3(radius * 0.38, radius * mouthOpen * 0.67, radius * 0.03), 0xFF08050D);
    }

    private static void renderTractorCone(PoseStack.Pose pose, VertexConsumer buffer, int group, int age) {
        double x = group == 1 ? -16 : 16;
        double pulse = 1.0 + Math.sin(age * 0.22) * 0.09;
        Vec3 top = new Vec3(x, 5.9, -12.0);
        Vec3 bottom = new Vec3(x, -25.0, -17.0);
        cone(pose, buffer, top, bottom, 0.75, 4.3 * pulse, 0x75A756ED);
        cone(pose, buffer, top, bottom, 0.30, 1.20 * pulse, 0x70D7A4FF);
    }

    private static void ellipsoid(PoseStack.Pose pose, VertexConsumer buffer, Vec3 center, Vec3 radii, int color) {
        ellipsoid(pose, buffer, center, radii, color, 10, 16);
    }

    private static void ellipsoid(PoseStack.Pose pose, VertexConsumer buffer, Vec3 center, Vec3 radii,
                                  int color, int rings, int segments) {
        for (int ring = 0; ring < rings; ring++) {
            double a0 = Math.PI * ring / rings;
            double a1 = Math.PI * (ring + 1) / rings;
            for (int segment = 0; segment < segments; segment++) {
                double b0 = Math.PI * 2.0 * segment / segments;
                double b1 = Math.PI * 2.0 * (segment + 1) / segments;
                sphereVertex(pose, buffer, center, radii, a0, b0, color);
                sphereVertex(pose, buffer, center, radii, a1, b0, color);
                sphereVertex(pose, buffer, center, radii, a1, b1, color);
                sphereVertex(pose, buffer, center, radii, a0, b1, color);
            }
        }
    }

    private static void sphereVertex(PoseStack.Pose pose, VertexConsumer buffer, Vec3 center, Vec3 radii,
                                     double latitude, double longitude, int color) {
        double nx = Math.sin(latitude) * Math.cos(longitude);
        double ny = Math.cos(latitude);
        double nz = Math.sin(latitude) * Math.sin(longitude);
        double light = 0.68 + Math.max(0.0, nx * -0.32 + ny * 0.75 + nz * -0.56) * 0.55;
        int shaded = shade(color, light);
        vertex(pose, buffer, center.add(nx * radii.x, ny * radii.y, nz * radii.z), shaded);
    }

    private static void tube(PoseStack.Pose pose, VertexConsumer buffer, Vec3 start, Vec3 end,
                             double radius, int color) {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 reference = Math.abs(direction.y) < 0.8 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 across = direction.cross(reference).normalize();
        Vec3 depth = direction.cross(across).normalize();
        for (int side = 0; side < 10; side++) {
            double a0 = Math.PI * 2.0 * side / 10.0;
            double a1 = Math.PI * 2.0 * (side + 1) / 10.0;
            Vec3 r0 = across.scale(Math.cos(a0) * radius).add(depth.scale(Math.sin(a0) * radius));
            Vec3 r1 = across.scale(Math.cos(a1) * radius).add(depth.scale(Math.sin(a1) * radius));
            int shaded = shade(color, 0.72 + 0.28 * Math.max(0.0, Math.cos(a0)));
            quad(pose, buffer, start.add(r0), start.add(r1), end.add(r1.scale(0.84)),
                    end.add(r0.scale(0.84)), shaded);
        }
    }

    private static void cone(PoseStack.Pose pose, VertexConsumer buffer, Vec3 top, Vec3 bottom,
                             double topRadius, double bottomRadius, int color) {
        for (int side = 0; side < 20; side++) {
            double a0 = Math.PI * 2.0 * side / 20.0;
            double a1 = Math.PI * 2.0 * (side + 1) / 20.0;
            Vec3 t0 = top.add(Math.cos(a0) * topRadius, 0, Math.sin(a0) * topRadius);
            Vec3 t1 = top.add(Math.cos(a1) * topRadius, 0, Math.sin(a1) * topRadius);
            Vec3 b0 = bottom.add(Math.cos(a0) * bottomRadius, 0, Math.sin(a0) * bottomRadius);
            Vec3 b1 = bottom.add(Math.cos(a1) * bottomRadius, 0, Math.sin(a1) * bottomRadius);
            quad(pose, buffer, t0, t1, b1, b0, color);
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer,
                             Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        vertex(pose, buffer, a, color);
        vertex(pose, buffer, b, color);
        vertex(pose, buffer, c, color);
        vertex(pose, buffer, d, color);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vec3 position, int color) {
        buffer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z).setColor(color);
    }

    private static int shade(int color, double brightness) {
        int r = Math.min(255, (int) (((color >> 16) & 255) * brightness));
        int g = Math.min(255, (int) (((color >> 8) & 255) * brightness));
        int b = Math.min(255, (int) ((color & 255) * brightness));
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private record Snapshot(StormVisualPayload visual, int age, Vec3 camera) {}

    private ClientStormGeometry() {}
}
