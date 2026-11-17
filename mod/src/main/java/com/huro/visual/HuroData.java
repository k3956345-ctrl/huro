package com.huro.visual;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Friends and markers. Kept separate from settings so profiles never overwrite them. */
public class HuroData {
    public static class Marker {
        public String name;
        public double x, y, z;
        public String dim;
        public int color;
    }

    public List<String> friends = new ArrayList<>();
    public List<Marker> markers = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HuroData instance;

    public static HuroData get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        Path f = HuroConfig.dir().resolve("data.json");
        try {
            if (Files.exists(f)) {
                HuroData d = GSON.fromJson(Files.readString(f), HuroData.class);
                if (d != null) {
                    if (d.friends == null) d.friends = new ArrayList<>();
                    if (d.markers == null) d.markers = new ArrayList<>();
                    instance = d;
                    return;
                }
            }
        } catch (Exception e) {
            HuroVisualClient.LOG.warn("Failed to read data.json", e);
        }
        instance = new HuroData();
    }

    public static void save() {
        try {
            Files.writeString(HuroConfig.dir().resolve("data.json"), GSON.toJson(get()));
        } catch (IOException e) {
            HuroVisualClient.LOG.warn("Failed to save data.json", e);
        }
    }

    public boolean isFriend(String name) {
        for (String f : friends) if (f.equalsIgnoreCase(name)) return true;
        return false;
    }

    public boolean addFriend(String name) {
        name = name.trim();
        if (name.isEmpty() || isFriend(name)) return false;
        friends.add(name);
        save();
        return true;
    }

    public void removeFriend(String name) {
        friends.removeIf(f -> f.equalsIgnoreCase(name));
        save();
    }

    public void addMarker(String name, double x, double y, double z, String dim) {
        Marker m = new Marker();
        m.name = name.isBlank() ? "Marker " + (markers.size() + 1) : name.trim();
        m.x = x; m.y = y; m.z = z; m.dim = dim;
        m.color = markers.size() % Palette.COLORS.length;
        markers.add(m);
        save();
    }

    public void removeMarker(int i) {
        if (i >= 0 && i < markers.size()) { markers.remove(i); save(); }
    }
}
