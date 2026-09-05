package com.ethan.armoredarsenal.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class ThrowEnchantHandler {
    private static final String THROW_LEVEL_KEY = "armoredarsenal_throw_level";

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("throw")
                .then(Commands.argument("level", IntegerArgumentType.integer(1, 255))
                        .executes(context -> enchant(
                                context.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(context, "level")))));
        dispatcher.register(Commands.literal("throw255")
                .executes(context -> enchant(context.getSource().getPlayerOrException(), 255)));
    }

    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || throwLevel(event.getItemStack()) <= 0) {
            return;
        }
        event.setCancellationResult(event.getLevel().isClientSide()
                ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack held = event.getItemStack();
        ItemStack projectileStack = held.copyWithCount(1);
        Vec3 direction = player.getLookAngle().normalize();
        Snowball projectile = new Snowball(player.level(), player, projectileStack);
        projectile.setPos(player.getEyePosition().add(direction.scale(0.65D)));
        projectile.setDeltaMovement(direction.scale(2.25D));
        player.level().addFreshEntity(projectile);
        player.level().playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 1.0F, 0.7F);
        player.getCooldowns().addCooldown(held, 8);
        if (!player.isCreative()) {
            held.shrink(1);
        }
    }

    public static void projectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Snowball projectile)
                || throwLevel(projectile.getItem()) <= 0
                || !(projectile.level() instanceof ServerLevel level)) {
            return;
        }
        int enchantmentLevel = throwLevel(projectile.getItem());
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, projectile.getBoundingBox().inflate(1.0D),
                entity -> entity != projectile.getOwner())) {
            target.hurtServer(level, level.damageSources().thrown(projectile, projectile.getOwner()),
                    Math.max(1.0F, enchantmentLevel));
        }
        level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                level, projectile.getX(), projectile.getY(), projectile.getZ(), projectile.getItem().copyWithCount(1)));
        projectile.discard();
        event.setCanceled(true);
    }

    private static int enchant(ServerPlayer player, int level) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("Hold the item you want to enchant with Throw."));
            return 0;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(THROW_LEVEL_KEY, level));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        player.sendSystemMessage(Component.literal("Added Throw " + level + " to " + stack.getHoverName().getString() + "."));
        return 1;
    }

    private static int throwLevel(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr(THROW_LEVEL_KEY, 0);
    }

    private ThrowEnchantHandler() {}
}
