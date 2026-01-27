package com.kukso.hy.warps;

import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.builtin.teleport.Warp;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarpManager {
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> warmingUp = ConcurrentHashMap.newKeySet();

    public WarpManager() {
    }

    public boolean isWarmingUp(UUID uuid) {
        return warmingUp.contains(uuid);
    }

    public void setWarmingUp(UUID uuid, boolean isWarmingUp) {
        if (isWarmingUp) {
            warmingUp.add(uuid);
        } else {
            warmingUp.remove(uuid);
        }
    }

    public int getWarmup() {
        return WarpConfigManager.get().warmup;
    }

    public int getCooldown() {
        return WarpConfigManager.get().cooldown;
    }

    public long getRemainingCooldown(UUID uuid) {
        if (!cooldowns.containsKey(uuid)) return 0;
        long remaining = cooldowns.get(uuid) - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void setCooldown(UUID uuid) {
        if (getCooldown() <= 0) return;
        cooldowns.put(uuid, System.currentTimeMillis() + (getCooldown() * 1000L));
    }

    public void createWarp(String name, Transform transform, World world, String creator, Store<EntityStore> store) {
        // Use native Warp class
        Warp warp = new Warp(transform, name, world, creator, Instant.now());
        
        // Delegate to TeleportPlugin
        TeleportPlugin.get().createWarp(warp, store);
        TeleportPlugin.get().saveWarps();
    }

    public void deleteWarp(String name) {
        TeleportPlugin.get().getWarps().remove(name);
        TeleportPlugin.get().saveWarps();
    }

    public Warp getWarp(String name) {
        return (Warp) TeleportPlugin.get().getWarps().get(name);
    }

    public Collection<String> getWarpNames() {
        return (Collection<String>) TeleportPlugin.get().getWarps().keySet();
    }
}
