package dev.hygallery.data;

import com.google.gson.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class DataHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final Map<String, Map<String, Location>> homes;
    private final File dataFile;
    private final Gson gson;

    public DataHandler() {
        LOGGER.atInfo().log("DataHandler constructor started");

        this.homes = new HashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        // Create data directory
        File dataFolder = new File("plugins/openhomes");

        LOGGER.atInfo().log("Data folder path: " + dataFolder.getAbsolutePath());

        if (!dataFolder.exists()) {
            LOGGER.atInfo().log("Data folder doesn't exist, creating...");
            if (!dataFolder.mkdirs()) {
                LOGGER.atSevere().log("Failed to create plugin data folder at: " + dataFolder.getAbsolutePath());
            } else {
                LOGGER.atInfo().log("Successfully created data folder");
            }
        } else {
            LOGGER.atInfo().log("Data folder already exists");
        }

        this.dataFile = new File(dataFolder, "homeData.json");
        LOGGER.atInfo().log("Data file path: " + dataFile.getAbsolutePath());

        try {
            loadHomes();
            int totalPlayers = homes.size();
            int totalHomes = homes.values().stream().mapToInt(Map::size).sum();
            LOGGER.atInfo().log("DataHandler initialized successfully with " + totalPlayers +
                                    "players and " + totalHomes + " total homes");
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("Error during loadHomes()");
        }
    }

    /**
     * Set a named home for a player
     * @param uuid Player UUID
     * @param homeName Name of the home (e.g., "home", "base", "farm")
     * @param location Location of the home
     */
    public void setHome(String uuid, String homeName, Location location) {
        homes.computeIfAbsent(uuid, k -> new HashMap<>()).put(homeName, location);
        saveHomes();
        LOGGER.atInfo().log("Set home '" + homeName + "' - UUID: " + uuid);
    }

    /**
     * Get a specific named home Location for a player
     * @param uuid Player UUID
     * @param homeName Name of the home
     * @return Location or null if not found
     */
    public Location getHome(String uuid, String homeName) {
        Map<String, Location> playerHomes = homes.get(uuid);
        if (playerHomes == null) {
            LOGGER.atInfo().log("No homes found for UUID: " + uuid);
            return null;
        }

        Location home = playerHomes.get(homeName);
        if (home == null) {
            LOGGER.atInfo().log("No home '" + homeName + "' found for UUID: " + uuid);
        }
        return home;
    }

    /**
     * Get list of home names for a player
     * @param uuid Player UUID
     * @return Set of home names
     */
    public Set<String> getHomeNames(String uuid) {
        Map<String, Location> playerHomes = homes.get(uuid);
        return playerHomes != null ? playerHomes.keySet() : Collections.emptySet();
    }

    /**
     * Delete a specific home
     * @param uuid Player UUID
     * @param homeName Name of the home to delete
     * @return true if deleted, false if not found
     */
    public boolean deleteHome(String uuid, String homeName) {
        Map<String, Location> playerHomes = homes.get(uuid);
        if (playerHomes == null) {
            return false;
        }

        boolean removed = playerHomes.remove(homeName) != null;
        if (removed) {
            if (playerHomes.isEmpty()) {
                homes.remove(uuid);
            }
            saveHomes();
            LOGGER.atInfo().log("Deleted home '" + homeName + "' for UUID: " + uuid);
        }
        return removed;
    }

    private void loadHomes() {
        if (!dataFile.exists()) {
            LOGGER.atInfo().log("No data file found at: " + dataFile.getAbsolutePath());
            return;
        }

        LOGGER.atInfo().log("Loading homes from JSON file...");

        try {
            String json = Files.readString(dataFile.toPath());
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject users = root.getAsJsonObject("users");

            if (users == null) {
                LOGGER.atWarning().log("No 'users' object found in JSON");
                return;
            }

            for (Map.Entry<String, JsonElement> userEntry : users.entrySet()) {
                String uuid = userEntry.getKey();
                JsonObject userHomes = userEntry.getValue().getAsJsonObject();

                Map<String, Location> playerHomes = new HashMap<>();

                for (Map.Entry<String, JsonElement> homeEntry : userHomes.entrySet()) {
                    String homeName = homeEntry.getKey();
                    JsonArray homeData = homeEntry.getValue().getAsJsonArray();

                    if (homeData.size() == 4) {
                        double x = homeData.get(0).getAsDouble();
                        double y = homeData.get(1).getAsDouble();
                        double z = homeData.get(2).getAsDouble();
                        String worldName = homeData.get(3).getAsString();

                        Vector3d coords = new Vector3d(x, y, z);
                        playerHomes.put(homeName, new Location(coords, worldName));
                    } else {
                        LOGGER.atWarning().log("Invalid home data for " + uuid + "." + homeName);
                    }
                }

                homes.put(uuid, playerHomes);
            }

            int totalHomes = homes.values().stream().mapToInt(Map::size).sum();
            LOGGER.atInfo().log("Successfully loaded " + totalHomes + " homes for " + homes.size() + " players");

        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Error reading JSON file");
            throw new RuntimeException("Failed to load homes", e);
        } catch (JsonSyntaxException e) {
            LOGGER.atSevere().withCause(e).log("Invalid JSON format");
            throw new RuntimeException("Failed to parse homes JSON", e);
        }
    }

    private void saveHomes() {
        int totalHomes = homes.values().stream().mapToInt(Map::size).sum();
        LOGGER.atInfo().log("Saving " + totalHomes + " homes for " + homes.size() + " players to file...");

        try {
            JsonObject root = new JsonObject();
            JsonObject users = new JsonObject();

            for (Map.Entry<String, Map<String, Location>> userEntry : homes.entrySet()) {
                String uuid = userEntry.getKey();
                JsonObject userHomes = getJsonObject(userEntry);

                users.add(uuid, userHomes);
            }

            root.add("users", users);

            Files.writeString(dataFile.toPath(), gson.toJson(root));
            LOGGER.atInfo().log("Successfully saved homes to JSON file");

        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Error saving homes to JSON file");
        }
    }

    private static @NotNull JsonObject getJsonObject(Map.Entry<String, Map<String, Location>> userEntry) {
        JsonObject userHomes = new JsonObject();

        for (Map.Entry<String, Location> homeEntry : userEntry.getValue().entrySet()) {
            String homeName = homeEntry.getKey();
            Location location = homeEntry.getValue();
            Vector3d coords = location.getCoords();

            JsonArray homeData = new JsonArray();
            homeData.add(coords.x);
            homeData.add(coords.y);
            homeData.add(coords.z);
            homeData.add(location.getWorldName());

            userHomes.add(homeName, homeData);
        }
        return userHomes;
    }
}