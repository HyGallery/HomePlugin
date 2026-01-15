package dev.vkarma;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.vkarma.commands.DelHomeCommand;
import dev.vkarma.commands.HomeCommand;
import dev.vkarma.commands.HomeListCommand;
import dev.vkarma.commands.SetHomeCommand;
import dev.vkarma.data.DataHandler;

import javax.annotation.Nonnull;

public class HomePlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final DataHandler dataHandler;
    private static HomePlugin instance;

    public HomePlugin(@Nonnull JavaPluginInit init) {
        super(init);

        try {
            this.dataHandler = new DataHandler();
            instance = this;
            LOGGER.atInfo().log("Home plugin initialized successfully.");
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("FAILED to initialize HomePlugin!");
            throw new RuntimeException("HomePlugin initialization failed", e);
        }
    }

    @Override
    protected void setup() {
        try {
            this.getCommandRegistry().registerCommand(new HomeCommand(getDataHandler()));
            this.getCommandRegistry().registerCommand(new SetHomeCommand(getDataHandler()));
            this.getCommandRegistry().registerCommand(new HomeListCommand(getDataHandler()));
            this.getCommandRegistry().registerCommand(new DelHomeCommand(getDataHandler()));
            LOGGER.atInfo().log("Successfully registered home commands");
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("FAILED to register commands!");
            throw new RuntimeException("Command registration failed", e);
        }
    }

    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public static HomePlugin getInstance() {
        return instance;
    }
}