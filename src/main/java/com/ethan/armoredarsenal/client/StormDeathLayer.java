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
        int opacity = Math.min(255, elapsed * 13);
        if (remaining < 20) {
            opacity = Math.min(opacity, remaining * 13);
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        graphics.fill(0, 0, width, height, (opacity << 24) | 0xFFFFFF);
        if (elapsed < 12 || remaining < 13) {
            return;
        }
        int size = Math.max(30, Math.min(width / 5, height / 3));
        int cy = height / 2;
        for (int side : new int[] {-1, 1}) {
            int cx = width / 2 + side * (size / 2 + 12);
            int left = cx - size / 2;
            int top = cy - size / 2;
            graphics.fill(left, top, left + size, top + size, 0xFF111016);
            int eyeY = top + size / 3;
            int eyeSize = Math.max(3, size / 10);
            for (int eye : new int[] {-1, 1}) {
                int ex = cx + eye * size / 4;
                graphics.fill(ex - eyeSize, eyeY - eyeSize, ex + eyeSize, eyeY + eyeSize, 0xFFE5D8F4);
                for (int pixel = -eyeSize; pixel <= eyeSize; pixel++) {
                    graphics.fill(ex + pixel, eyeY + pixel, ex + pixel + 1, eyeY + pixel + 1, 0xFF111016);
                    graphics.fill(ex + pixel, eyeY - pixel, ex + pixel + 1, eyeY - pixel + 1, 0xFF111016);
                }
            }
            int mouthHeight = Math.max(4, Math.min(size / 3, (elapsed - 12) * size / 75));
            graphics.fill(cx - size / 4, top + size * 2 / 3,
                    cx + size / 4, top + size * 2 / 3 + mouthHeight, 0xFFEEE7F8);
        }
    }
}
