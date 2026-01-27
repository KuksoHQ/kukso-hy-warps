package com.kukso.hy.warps.command;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.kukso.hy.warps.WarpManager;
import com.hypixel.hytale.builtin.teleport.Warp;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.kukso.hy.warps.util.PermissionUtil;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class WarpCommand extends AbstractPlayerCommand {
    private final WarpManager warpManager;
    private final RequiredArg<String> nameArg;
    private static final String WARMUP_BYPASS_PERMISSION = "kukso.warps.bypass.warmup";
    private static final String COOLDOWN_BYPASS_PERMISSION = "kukso.warps.bypass.cooldown";
    private static final double MOVEMENT_THRESHOLD = 0.5;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public WarpCommand(WarpManager warpManager) {
        super("warp", "Teleport to a warp");
        requirePermission("kukso.command.warp");

        this.warpManager = warpManager;
        this.nameArg = this.withRequiredArg("name", "Warp name", ArgTypes.STRING);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef player,
            @Nonnull World world) {
        UUID playerUuid = player.getUuid();
        String name = context.get(nameArg);
        Warp warp = warpManager.getWarp(name);
        
        if (warp == null) {
            player.sendMessage(Message.raw("Warp not found: " + name));
            return;
        }

        // Cross-world check might be needed if Teleport component doesn't handle it automatically.
        // Assuming Teleport handles it or we restrict it.
        // Native Warp object stores world name.
        if (!warp.getWorld().equals(world.getName())) {
             player.sendMessage(Message.raw("Warp is in another world! Cross-world teleportation is not supported yet."));
             return;
        }

        // Cooldown Check
        boolean bypassCooldown = PermissionUtil.hasPermission(playerUuid, COOLDOWN_BYPASS_PERMISSION);
        if (!bypassCooldown) {
            long remaining = warpManager.getRemainingCooldown(player.getUuid());
            if (remaining > 0) {
                long seconds = (remaining / 1000) + 1;
                player.sendMessage(Message.raw("You must wait " + seconds + " seconds before warping again."));
                return;
            }
        }

        if (warpManager.isWarmingUp(player.getUuid())) {
            player.sendMessage(Message.raw("Teleportation already in progress!"));
            return;
        }

        // Warmup Check
        int warmup = warpManager.getWarmup();
        boolean bypassWarmup = PermissionUtil.hasPermission(playerUuid, WARMUP_BYPASS_PERMISSION);

        if (warmup > 0 && !bypassWarmup) {
            startWarmupCountdown(player, playerUuid, store, ref, world, warp, name, warmup);
        } else {
            executeTeleport(player, playerUuid, store, ref, world, warp, name);
        }
    }

    private void startWarmupCountdown(PlayerRef player, UUID playerUuid, Store<EntityStore> store,
                                       Ref<EntityStore> ref, World world, Warp warp,
                                       String warpName, int warmupSeconds) {
        warpManager.setWarmingUp(playerUuid, true);
        // Copy position values
        Vector3d pos = player.getTransform().getPosition();
        final double startX = pos.x;
        final double startY = pos.y;
        final double startZ = pos.z;
        final int[] secondsRemaining = {warmupSeconds};

        // Send initial notification
        sendWarmupNotification(player, warmupSeconds);

        ScheduledFuture<?>[] futureHolder = new ScheduledFuture<?>[1];
        futureHolder[0] = scheduler.scheduleAtFixedRate(() -> {
            world.execute(() -> {
                // Guard: Check if warmup was already cancelled
                if (!warpManager.isWarmingUp(playerUuid)) {
                    futureHolder[0].cancel(true);
                    return;
                }

                // Check if player moved
                Vector3d currentPosition = player.getTransform().getPosition();
                if (hasPlayerMoved(startX, startY, startZ, currentPosition)) {
                    cancelWarmup(player, playerUuid, futureHolder[0]);
                    return;
                }

                secondsRemaining[0]--;
                if (secondsRemaining[0] <= 0) {
                    futureHolder[0].cancel(true);
                    // Double-check warmup state before teleporting
                    if (warpManager.isWarmingUp(playerUuid)) {
                        executeTeleport(player, playerUuid, store, ref, world, warp, warpName);
                    }
                }
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    private boolean hasPlayerMoved(double startX, double startY, double startZ, Vector3d current) {
        double dx = current.x - startX;
        double dy = current.y - startY;
        double dz = current.z - startZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz) > MOVEMENT_THRESHOLD;
    }

    private void cancelWarmup(PlayerRef player, UUID playerUuid, ScheduledFuture<?> future) {
        warpManager.setWarmingUp(playerUuid, false);
        future.cancel(true);
        player.sendMessage(Message.raw("Teleportation cancelled - you moved!"));
    }

    private void executeTeleport(PlayerRef player, UUID playerUuid, Store<EntityStore> store,
                                  Ref<EntityStore> ref, World world, Warp warp, String warpName) {
        warpManager.setWarmingUp(playerUuid, false);

        Teleport teleport = warp.toTeleport();
        store.addComponent(ref, Teleport.getComponentType(), teleport);
        player.sendMessage(Message.raw("Teleported to " + warpName));

        warpManager.setCooldown(playerUuid);
    }

    private static void sendWarmupNotification(PlayerRef player, int secondsLeft) {
        var packetHandler = player.getPacketHandler();

        var primaryMessage = Message.raw("TELEPORTING").color("#00FF00");
        var secondaryMessage = Message.raw("Do not move until " + secondsLeft + " seconds.").color("#228B22");
        var icon = new ItemStack("Ingredient_Void_Essence", 1).toPacket();

        NotificationUtil.sendNotification(
                packetHandler,
                primaryMessage,
                secondaryMessage,
                icon);
    }
}
