package com.kukso.hy.warps.command;

//import com.kukso.hy.lib.locale.LocaleMan;
import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.builtin.teleport.Warp;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
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
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Map;

public class SetWarpCommand extends AbstractPlayerCommand {

    @Nonnull
    private static final Message MESSAGE_COMMANDS_TELEPORT_WARP_NOT_LOADED = Message.translation("server.commands.teleport.warp.notLoaded");
    @Nonnull
    private static final Message MESSAGE_COMMANDS_TELEPORT_WARP_RESERVED_KEYWORD = Message.translation("server.commands.teleport.warp.reservedKeyword");

    private final WarpManager warpManager;
    @Nonnull
    private final RequiredArg<String> nameArg;

    public SetWarpCommand(WarpManager warpManager) {
        super("setwarp", "Set a new warp");
        requirePermission("kukso.command.setwarp");

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
//        warpManager.createWarp(name, player.getTransform(), world, player.getUsername(), store);
//        //player.sendMessage(LocaleMan.get(player, "warps.set_success", Map.of("warp", name)));
//        player.sendMessage(Message.raw("Warp " + name + " set successfully."));
//    }

    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (!TeleportPlugin.get().isWarpsLoaded()) {
            context.sendMessage(MESSAGE_COMMANDS_TELEPORT_WARP_NOT_LOADED);
        } else {
            Map<String, Warp> warps = TeleportPlugin.get().getWarps();
            String newId = ((String)this.nameArg.get(context)).toLowerCase();
            if (!"reload".equals(newId) && !"remove".equals(newId) && !"set".equals(newId) && !"list".equals(newId) && !"go".equals(newId)) {
                TransformComponent transformComponent = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());

                assert transformComponent != null;

                HeadRotation headRotationComponent = (HeadRotation)store.getComponent(ref, HeadRotation.getComponentType());

                assert headRotationComponent != null;

                Vector3d position = transformComponent.getPosition();
                Vector3f headRotation = headRotationComponent.getRotation();
                Transform transform = new Transform(position.clone(), headRotation.clone());
                Warp newWarp = new Warp(transform, newId, world, playerRef.getUsername(), Instant.now());
                warps.put(newWarp.getId().toLowerCase(), newWarp);
                TeleportPlugin plugin = TeleportPlugin.get();
                plugin.saveWarps();
                store.addEntity(plugin.createWarp(newWarp, store), AddReason.LOAD);
                context.sendMessage(Message.translation("server.commands.teleport.warp.setWarp").param("name", newWarp.getId()));
            } else {
                context.sendMessage(MESSAGE_COMMANDS_TELEPORT_WARP_RESERVED_KEYWORD);
            }
        }
    }
}
