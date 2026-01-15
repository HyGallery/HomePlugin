package dev.hygallery.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.ModelTransform;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.packets.player.ClientTeleport;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hygallery.data.DataHandler;
import dev.hygallery.data.Location;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HomeCommand extends AbstractAsyncCommand {

    private final DataHandler dataHandler;

    public HomeCommand(DataHandler dataHandler) {
        super("home", "Teleport to your home point");
        this.dataHandler = dataHandler;
        setAllowsExtraArguments(true);
        requirePermission("openhomes.use");
    }

    private String getHomeName(CommandContext context) {
        String inp = context.getInputString().trim();
        String[] args = inp.split("\\s+");
        if (args.length > 1)
            return args[1];
        return null;
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {

        if (!(context.isPlayer())) {
            context.sendMessage(Message.raw("Error: attempted to teleport home from a non-player context").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Player player = (Player) context.sender();
        Ref<EntityStore> ref = player.getReference();

        if (ref == null || !ref.isValid()) {
            context.sendMessage(Message.raw("Error: invalid ref").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        return CompletableFuture.runAsync(() -> {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            if (playerRef == null) {
                context.sendMessage(Message.raw("Error: could not find player data").color(Color.RED));
                return;
            }

            String playerUuid = playerRef.getUuid().toString();

            // default to "home" if no name is given
            String homeName = getHomeName(context);
            homeName = ((homeName == null) ? "home" : homeName).toLowerCase();

            Location homePosition = dataHandler.getHome(playerUuid, homeName);

            if (homePosition == null) {
                context.sendMessage(Message.raw("You do not have a home named '" + homeName
                    + "'").color(Color.ORANGE));
                context.sendMessage(Message.raw("Current homes: " + dataHandler.getHomeNames(playerUuid)));
                return;
            }

            Vector3d homeCoords = homePosition.getCoords();
            String targetWorldName = homePosition.getWorldName();

            if (!world.getName().equals(targetWorldName)) {
                // Different world - use World.addPlayer() API
                Universe universe = Universe.get();
                Map<String, World> worlds = universe.getWorlds();
                World targetWorld = null;

                for (Map.Entry<String, World> entry : worlds.entrySet()) {
                    if (entry.getValue().getName().equals(targetWorldName)) {
                        targetWorld = entry.getValue();
                        break;
                    }
                }

                if (targetWorld == null) {
                    context.sendMessage(Message.raw("Error: world '" + targetWorldName + "' doesn't exist").color(Color.ORANGE));
                    return;
                }
                final World finalTargetWorld = targetWorld;

                try {
                    // Step 1: Remove player from current world
                    playerRef.removeFromStore();

                    // Step 2: Create transform for target location
                    Transform homeTransform = new Transform(
                            homeCoords.x, homeCoords.y, homeCoords.z,
                            0.0f, 0.0f, 0.0f
                    );

                    // Step 3: Add player to target world (this returns a CompletableFuture)
                    CompletableFuture<PlayerRef> transferFuture = finalTargetWorld.addPlayer(playerRef, homeTransform);

                    // Step 4: Wait for transfer and handle result
                    transferFuture.thenAccept(resultPlayerRef -> {
                        if (resultPlayerRef != null) {
                            context.sendMessage(Message.raw("Changed world to " + targetWorldName).color(Color.GREEN));
                        } else {
                            context.sendMessage(Message.raw("Error: failed to transfer to target world").color(Color.ORANGE));
                        }
                    }).exceptionally(throwable -> {
                        context.sendMessage(Message.raw("Error during transfer: " + throwable.getMessage()).color(Color.ORANGE));
                        throwable.printStackTrace();
                        return null;
                    });

                } catch (Exception e) {
                    context.sendMessage(Message.raw("Error: " + e.getMessage()).color(Color.ORANGE));
                    e.printStackTrace();
                }
            } else {
                // Same world teleport
                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());

                if (transform == null) {
                    context.sendMessage(Message.raw("Error: could not access player transform").color(Color.ORANGE));
                    return;
                }

                transform.teleportPosition(homeCoords);

                Position pos = new Position(homeCoords.x, homeCoords.y, homeCoords.z);
                Direction body = new Direction(0f, 0f, 0f);
                Direction look = new Direction(0f, 0f, 0f);
                ModelTransform modelTransform = new ModelTransform(pos, body, look);

                player.getPlayerConnection().write(new ClientTeleport((byte) 0, modelTransform, true));

                context.sendMessage(Message.raw("Teleported home!").color(Color.GREEN));
            }
        }, world);
    }
}