package com.ethan.armoredarsenal.client;

import com.ethan.armoredarsenal.ArmoredArsenal;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.neoforge.client.gui.GuiLayer;

public final class WardenMegabossHudLayer implements GuiLayer {
    private static final Identifier FRAME_TEXTURE =
            ArmoredArsenal.id("textures/gui/warden_megaboss_frame.png");
    private static final int TEXTURE_WIDTH = 2079;
    private static final int TEXTURE_HEIGHT = 756;
    private static final int FRAME_CROP_TOP = 320;
    private static final int FRAME_CROP_HEIGHT = TEXTURE_HEIGHT - FRAME_CROP_TOP;
    private static final int CREST_LEFT = 650;
    private static final int CREST_WIDTH = 779;
    private static final int CREST_HEIGHT = 360;
    private static final int BAR_LEFT = 232;
    private static final int BAR_TOP = 465;
    private static final int BAR_RIGHT = 1821;
    private static final int BAR_BOTTOM = 566;
    private static final double DETECTION_RANGE_SQUARED = 128.0 * 128.0;

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }

        Warden warden = findNearestWarden(minecraft);
        if (warden == null) {
            return;
        }

        int frameWidth = Math.min(460, graphics.guiWidth() - 16);
        if (frameWidth < 160) {
            return;
        }
        float scale = frameWidth / (float) TEXTURE_WIDTH;
        int frameHeight = Math.round(FRAME_CROP_HEIGHT * scale);
        int frameX = (graphics.guiWidth() - frameWidth) / 2;
        int frameY = 8;

        int barX = frameX + Math.round(BAR_LEFT * scale);
        int barY = frameY + Math.round((BAR_TOP - FRAME_CROP_TOP) * scale);
        int barWidth = Math.round((BAR_RIGHT - BAR_LEFT) * scale);
        int barHeight = Math.max(8, Math.round((BAR_BOTTOM - BAR_TOP) * scale));
        float healthFraction = Math.clamp(warden.getHealth() / warden.getMaxHealth(), 0.0F, 1.0F);
        int filledWidth = Math.round(barWidth * healthFraction);

        float crestScale = scale * 0.62F;
        int crestWidth = Math.round(CREST_WIDTH * crestScale);
        int crestHeight = Math.round(CREST_HEIGHT * crestScale);
        int crestX = (graphics.guiWidth() - crestWidth) / 2;

        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xF20A1012);
        if (filledWidth > 0) {
            graphics.fillGradient(barX, barY, barX + filledWidth, barY + barHeight,
                    0xFFDA213B, 0xFF711526);
            graphics.fill(barX, barY, barX + filledWidth, barY + 2, 0xFFFF6A74);
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME_TEXTURE, frameX, frameY,
                0.0F, FRAME_CROP_TOP, frameWidth, frameHeight, TEXTURE_WIDTH, FRAME_CROP_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME_TEXTURE, crestX, barY - crestHeight - 1,
                CREST_LEFT, 0.0F, crestWidth, crestHeight, CREST_WIDTH, CREST_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);

        Font font = minecraft.font;
        Component label = Component.literal("WARDEN MEGABOSS  "
                + Math.max(0L, (long) Math.ceil(warden.getHealth())) + " / "
                + (long) Math.ceil(warden.getMaxHealth()));
        int textY = barY + Math.max(0, (barHeight - font.lineHeight) / 2);
        graphics.centeredText(font, label, graphics.guiWidth() / 2, textY, 0xFFF2FEFF);
    }

    private static Warden findNearestWarden(Minecraft minecraft) {
        Warden nearest = null;
        double nearestDistance = DETECTION_RANGE_SQUARED;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof Warden warden) || !warden.isAlive()) {
                continue;
            }
            double distance = minecraft.player.distanceToSqr(warden);
            if (distance <= nearestDistance) {
                nearest = warden;
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
