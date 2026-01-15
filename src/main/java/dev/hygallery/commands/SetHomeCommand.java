package dev.hygallery.commands;


import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hygallery.data.DataHandler;
import dev.hygallery.data.Location;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

public class SetHomeCommand extends AbstractAsyncCommand {

    private final DataHandler dataHandler;

    public SetHomeCommand(DataHandler dataHandler) {
        super("sethome", "Set your home point");
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
    protected CompletableFuture<Void> executeAsync(@NotNull CommandContext context) {

        if (!(context.isPlayer())) {
            context.sendMessage(Message.raw("Error: attempted to set home from a non-player context"));
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> ref = ((Player)context.sender()).getReference();

        if (ref == null || !ref.isValid()) {
            context.sendMessage(Message.raw("Error: invalid ref"));
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        String worldName = world.getName();

        if (worldName.toLowerCase().contains("instance")) {
            context.sendMessage(Message.raw("Error: cannot set home in non-persistent world."));
            return CompletableFuture.completedFuture(null);
        }

        // Access sensitive components in the context of the world executor
        return CompletableFuture.runAsync(() -> {
            // PlayerRef component contains actual player data
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            if (playerRef == null)
                return;

            String playerUuid = playerRef.getUuid().toString();

            TransformComponent playerTransform = store.getComponent(ref, TransformComponent.getComponentType());

            if (playerTransform == null)
                return;

            Vector3d playerPosition = playerTransform.getPosition();

            Vector3d copiedPosition = new Vector3d(playerPosition.x, playerPosition.y, playerPosition.z);

            Location location = new Location(copiedPosition, world.getName());

            // default to "home" if no name is given
            String homeName = getHomeName(context);
            homeName = ((homeName == null) ? "home" : homeName).toLowerCase();

            dataHandler.setHome(playerUuid, homeName, location);

            context.sendMessage(Message.raw("Home '" + homeName + "' Set!").color(Color.GREEN));
        }, world);


    }
}
