package com.ethan.armoredarsenal.client;

import com.ethan.armoredarsenal.network.TransformationPayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

public final class ClientTransformationState {
    private static final Map<Integer, EntityType<?>> TRANSFORMATIONS = new HashMap<>();
    private static final Map<Integer, LivingEntity> DUMMIES = new HashMap<>();

    public static void accept(TransformationPayload payload) {
        if (payload.entityType().isBlank()) {
            TRANSFORMATIONS.remove(payload.playerId());
            LivingEntity old = DUMMIES.remove(payload.playerId());
            if (old != null) old.discard();
            return;
        }
        Identifier id = Identifier.tryParse(payload.entityType());
        if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            TRANSFORMATIONS.put(payload.playerId(), BuiltInRegistries.ENTITY_TYPE.getValue(id));
            DUMMIES.remove(payload.playerId());
        }
    }

    public static void render(RenderPlayerEvent.Pre<?> event) {
        EntityType<?> type = TRANSFORMATIONS.get(event.getRenderState().id);
        Minecraft minecraft = Minecraft.getInstance();
        if (type == null || minecraft.level == null) return;
        Entity player = minecraft.level.getEntity(event.getRenderState().id);
        if (!(player instanceof LivingEntity livingPlayer)) return;

        LivingEntity dummy = DUMMIES.get(event.getRenderState().id);
        if (dummy == null || dummy.getType() != type) {
            Entity created = type.create(minecraft.level, EntitySpawnReason.COMMAND);
            if (!(created instanceof LivingEntity createdLiving)) return;
            dummy = createdLiving;
            DUMMIES.put(event.getRenderState().id, dummy);
        }

        copyPose(livingPlayer, dummy);
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        EntityRenderState state = dispatcher.extractEntity(dummy, event.getPartialTick());
        CameraRenderState camera = minecraft.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        dispatcher.submit(state, camera, 0.0, 0.0, 0.0, event.getPoseStack(), event.getSubmitNodeCollector());
        event.setCanceled(true);
    }

    private static void copyPose(LivingEntity player, LivingEntity dummy) {
        dummy.setPos(player.position());
        dummy.setXRot(player.getXRot());
        dummy.setYRot(player.getYRot());
        dummy.xRotO = player.xRotO;
        dummy.yRotO = player.yRotO;
        dummy.yBodyRot = player.yBodyRot;
        dummy.yBodyRotO = player.yBodyRotO;
        dummy.yHeadRot = player.yHeadRot;
        dummy.yHeadRotO = player.yHeadRotO;
        dummy.tickCount = player.tickCount;
        dummy.setPose(player.getPose());
    }

    private ClientTransformationState() {}
}