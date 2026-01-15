package dev.vkarma.commands;


import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import dev.vkarma.data.DataHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class HomeListCommand extends AbstractCommand {

    private final DataHandler dataHandler;

    public HomeListCommand(DataHandler dataHandler) {
        super("homelist", "List your current homes");
        this.setPermissionGroup(GameMode.Adventure);
        this.dataHandler = dataHandler;
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@NotNull CommandContext context) {

        if (!(context.isPlayer())) {
            context.sendMessage(Message.raw("Error: attempted to list homes from a non-player context"));
            return CompletableFuture.completedFuture(null);
        }

        Set<String> homes = dataHandler.getHomeNames(context.sender().getUuid().toString());

        context.sendMessage(Message.raw("Current homes: " + homes));
        return CompletableFuture.completedFuture(null);
    }
}
