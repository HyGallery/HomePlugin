package dev.vkarma.data;

import com.hypixel.hytale.math.vector.Vector3d;

public class Location {
    private final Vector3d coordLocation;
    private final String worldName;

    public Location(Vector3d location, String worldName) {
        this.coordLocation = location;
        this.worldName = worldName;
    }

    public Vector3d getCoords() {
        return coordLocation;
    }

    public String getWorldName() {
        return worldName;
    }
}
