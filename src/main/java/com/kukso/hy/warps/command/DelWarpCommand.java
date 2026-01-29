package com.kukso.hy.warps.command;

import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.builtin.teleport.Warp;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.Message;
//import com.kukso.hy.lib.locale.LocaleMan;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.Universe;
import com.kukso.hy.warps.WarpManager;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;

import javax.annotation.Nonnull;
import java.util.Map;

public class DelWarpCommand extends CommandBase {
    private static final Message MESSAGE_COMMANDS_TELEPORT_WARP_NOT_LOADED = Message.translation("server.commands.teleport.warp.notLoaded");
    private final WarpManager warpManager;
    private final RequiredArg<String> nameArg;

    public DelWarpCommand(WarpManager warpManager) {
        super("delwarp", "Delete a warp");
        requirePermission("kukso.command.delwarp");

        this.warpManager = warpManager;
        this.nameArg = this.withRequiredArg("name", "Warp name", ArgTypes.STRING);
    }

//    @Override
//    protected void execute(
//            @Nonnull CommandContext context,
//            @Nonnull Store<EntityStore> store,
//            @Nonnull Ref<EntityStore> ref,
//            @Nonnull PlayerRef player,
//            @Nonnull World world) {
//        String name = context.get(nameArg);
//
//        if (warpManager.getWarp(name) == null) {
//            //player.sendMessage(LocaleMan.get(player, "warps.not_found", Map.of("warp", name)));
//            player.sendMessage(Message.raw("Warp " + name + " not found!"));
//            return;
//        }
//
//        warpManager.deleteWarp(name);
//        //player.sendMessage(LocaleMan.get(player, "warps.delete_success", Map.of("warp", name)));
//        player.sendMessage(Message.raw("Warp " + name + " deleted!"));
//    }

    protected void executeSync(@Nonnull CommandContext context) {
        if (!TeleportPlugin.get().isWarpsLoaded()) {
            context.sendMessage(MESSAGE_COMMANDS_TELEPORT_WARP_NOT_LOADED);
        } else {
            Map<String, Warp> warps = TeleportPlugin.get().getWarps();
            String warpName = ((String)this.nameArg.get(context)).toLowerCase();
            Warp old = (Warp)warps.remove(warpName);
            if (old == null) {
                context.sendMessage(Message.translation("server.commands.teleport.warp.unknownWarp").param("name", warpName));
            } else {
                TeleportPlugin.get().saveWarps();
                context.sendMessage(Message.translation("server.commands.teleport.warp.removedWarp").param("name", warpName));
                World targetWorld = Universe.get().getWorld(old.getWorld());
                if (targetWorld != null) {
                    ComponentType<EntityStore, TeleportPlugin.WarpComponent> warpComponentType = TeleportPlugin.WarpComponent.getComponentType();
                    Store<EntityStore> store = targetWorld.getEntityStore().getStore();
                    targetWorld.execute(() -> store.forEachEntityParallel(warpComponentType, (index, archetypeChunk, commandBuffer) -> {
                        TeleportPlugin.WarpComponent warpComponent = (TeleportPlugin.WarpComponent)archetypeChunk.getComponent(index, warpComponentType);
                        if (warpComponent != null && warpComponent.warp().getId().equals(old.getId())) {
                            commandBuffer.removeEntity(archetypeChunk.getReferenceTo(index), RemoveReason.REMOVE);
                        }

                    }));
                }
            }

        }
    }
}
