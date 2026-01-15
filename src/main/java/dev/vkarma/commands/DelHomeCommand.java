package dev.vkarma.commands;


import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import dev.vkarma.data.DataHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

public class DelHomeCommand extends AbstractCommand {

    private final DataHandler dataHandler;
    private final RequiredArg<String> nameArg;

    public DelHomeCommand(DataHandler dataHandler) {
        super("delhome", "List your current homes");
        this.setPermissionGroup(GameMode.Adventure);
        this.dataHandler = dataHandler;

        nameArg = withRequiredArg("name", "The name of the home to be deleted", ArgTypes.STRING);
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@NotNull CommandContext context) {

        if (!(context.isPlayer())) {
            context.sendMessage(Message.raw("Error: attempted to list homes from a non-player context"));
            return CompletableFuture.completedFuture(null);
        }

        String homeName = nameArg.get(context);

        if (dataHandler.deleteHome(context.sender().getUuid().toString(), homeName)) {
            context.sendMessage(Message.raw("Successfully deleted home '" + homeName).color(Color.GREEN));
        } else {
            context.sendMessage(Message.raw("Failed to delete home '" + homeName + "', " +
                    "list your current homes with /homelist").color(Color.ORANGE));
        }

        return CompletableFuture.completedFuture(null);
    }
}

