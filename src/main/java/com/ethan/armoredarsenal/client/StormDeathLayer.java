package com.ethan.armoredarsenal.client;

import com.ethan.armoredarsenal.network.StormDeathPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.gui.GuiLayer;

public final class StormDeathLayer implements GuiLayer {
    private static int remaining;
    private static int total;

    public static void start(StormDeathPayload payload) {
        remaining = Math.max(1, payload.durationTicks());
        total = remaining;
    }

    public static void tick() {
        if (remaining > 0) {
            remaining--;
        }
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (remaining <= 0 || Minecraft.getInstance().player == null) {
            return;
        }
        int elapsed = total - remaining;
        int opacity = Math.min(245, elapsed * 18);
        if (remaining < 12) {
            opacity = Math.min(opacity, remaining * 21);
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        graphics.fill(0, 0, width, height, (opacity << 24) | 0xFFFFFF);
        if (elapsed < 10 || remaining < 12) {
            return;
        }
        int radius = Math.max(22, Math.min(width / 10, height / 4));
        int cy = height / 2 - radius / 5;
        double opening = Math.min(1.0, Math.max(0.0, (elapsed - 14) / 42.0));
        double fall = elapsed > 56 ? (elapsed - 56) * (elapsed - 56) * 0.12 : 0.0;
        for (int side : new int[] {-1, 1}) {
            int cx = width / 2 + side * (radius + Math.max(12, radius / 3));
            int headY = cy + (int) fall;
            disc(graphics, cx, headY, radius, radius, 0xFF101015);
            disc(graphics, cx, headY - radius / 6, radius * 4 / 5, radius * 3 / 4, 0xFF17151C);
            int eyeY = headY - radius / 3;
            int eyeSize = Math.max(4, radius / 7);
            for (int eye : new int[] {-1, 1}) {
                int ex = cx + eye * radius / 2;
                disc(graphics, ex, eyeY, eyeSize + 2, eyeSize + 2, 0xFFB786E4);
                for (int pixel = -eyeSize; pixel <= eyeSize; pixel++) {
                    graphics.fill(ex + pixel - 1, eyeY + pixel - 1,
                            ex + pixel + 2, eyeY + pixel + 2, 0xFF0A0710);
                    graphics.fill(ex + pixel - 1, eyeY - pixel - 1,
                            ex + pixel + 2, eyeY - pixel + 2, 0xFF0A0710);
                }
            }
            int mouthHeight = Math.max(3, (int) (radius * (0.13 + opening * 0.42)));
            int mouthY = headY + radius / 3;
            disc(graphics, cx, mouthY, radius * 3 / 5, mouthHeight + 3, 0xFF8555B5);
            disc(graphics, cx, mouthY, radius / 2, mouthHeight, 0xFF040308);
        }
    }

    private static void disc(GuiGraphicsExtractor graphics, int cx, int cy, int rx, int ry, int color) {
        int block = Math.max(2, rx / 18);
        for (int y = -ry; y <= ry; y += block) {
            double sampleY = Math.min(ry, y + block * 0.5);
            double fraction = 1.0 - sampleY * sampleY / (ry * ry);
            int halfWidth = (int) (Math.round(rx * Math.sqrt(Math.max(0.0, fraction)) / block) * block);
            graphics.fill(cx - halfWidth, cy + y,
                    cx + halfWidth + block, cy + Math.min(ry + 1, y + block), color);
        }
    }
}
