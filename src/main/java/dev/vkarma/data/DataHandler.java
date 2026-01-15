package dev.vkarma.data;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;

import java.io.*;
import java.util.*;

public class DataHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final Map<String, Location> homes;
    private final File dataFile;

    public DataHandler() {
        LOGGER.atInfo().log("DataHandler constructor started");

        this.homes = new HashMap<>();

        // Create data directory
        File dataFolder = new File("plugins/VKarma_HomePlugin");

        LOGGER.atInfo().log("Data folder path: " + dataFolder.getAbsolutePath());

        if (!dataFolder.exists()) {
            LOGGER.atInfo().log("Data folder doesn't exist, creating...");
            if (!dataFolder.mkdirs()) {
                LOGGER.atSevere().log("Failed to create plugin data folder at: " + dataFolder.getAbsolutePath());
                // Don't throw exception, might still work if it exists now
            } else {
                LOGGER.atInfo().log("Successfully created data folder");
            }
        } else {
            LOGGER.atInfo().log("Data folder already exists");
        }

        this.dataFile = new File(dataFolder, "homeData.txt");
        LOGGER.atInfo().log("Data file path: " + dataFile.getAbsolutePath());

        try {
            loadHomes();
            LOGGER.atInfo().log("DataHandler initialized successfully with " + homes.size() + " homes");
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("Error during loadHomes()");
            // Don't rethrow - allow plugin to continue with empty homes map
        }
    }

    public void setHome(String uuid, Location location) {
        homes.put(uuid, location);
        saveHomes();
        LOGGER.atInfo().log("Set home - UUID: " + uuid);
    }

    public Location getHome(String uuid) {
        Location home = homes.get(uuid);
        if (home == null) {
            LOGGER.atInfo().log("No home found for UUID: " + uuid);
        }
        return home;
    }

    private void loadHomes() {
        if (!dataFile.exists()) {
            LOGGER.atInfo().log("No data file found at: " + dataFile.getAbsolutePath());
            return;
        }

        LOGGER.atInfo().log("Loading homes from file...");

        try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                try {
                    String[] parts = line.split(",");
                    if (parts.length == 5) {
                        String uuid = parts[0];
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);
                        double z = Double.parseDouble(parts[3]);
                        String worldName = parts[4];

                        Vector3d coords = new Vector3d(x, y, z);
                        homes.put(uuid, new Location(coords, worldName));
                    } else {
                        LOGGER.atWarning().log("Invalid line format (line " + lineCount + "): " + line);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.atWarning().log("Failed to parse coordinates on line " + lineCount + ": " + line);
                }
            }
            LOGGER.atInfo().log("Successfully loaded " + homes.size() + " player homes from " + lineCount + " lines");
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Error loading homes from file");
            throw new RuntimeException("Failed to load homes", e);
        }
    }

    private void saveHomes() {
        LOGGER.atInfo().log("Saving " + homes.size() + " homes to file...");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile))) {
            for (Map.Entry<String, Location> entry : homes.entrySet()) {
                String uuid = entry.getKey();
                Location loc = entry.getValue();
                Vector3d coords = loc.getCoords();

                writer.write(String.format("%s,%.2f,%.2f,%.2f,%s%n",
                        uuid,
                        coords.x,
                        coords.y,
                        coords.z,
                        loc.getWorldName()
                ));
            }
            LOGGER.atInfo().log("Successfully saved homes to file");
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Error saving homes to file");
        }
    }
}