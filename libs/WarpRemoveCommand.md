//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.hypixel.hytale.builtin.teleport.commands.warp;

import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.builtin.teleport.Warp;
import com.hypixel.hytale.builtin.teleport.TeleportPlugin.WarpComponent;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import javax.annotation.Nonnull;

public class WarpRemoveCommand extends CommandBase {
private static final Message MESSAGE_COMMANDS_TELEPORT_WARP_NOT_LOADED = Message.translation("server.commands.teleport.warp.notLoaded");
@Nonnull
private final RequiredArg<String> nameArg;

    public WarpRemoveCommand() {
        super("remove", "server.commands.warp.remove.desc");
        this.nameArg = this.withRequiredArg("name", "server.commands.warp.remove.name.desc", ArgTypes.STRING);
        this.requirePermission(HytalePermissions.fromCommand("warp.remove"));
    }

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
                    ComponentType<EntityStore, TeleportPlugin.WarpComponent> warpComponentType = WarpComponent.getComponentType();
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
