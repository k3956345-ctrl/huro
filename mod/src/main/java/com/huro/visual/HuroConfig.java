package com.huro.visual;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** All user settings. Serialized to config/huro_visual/settings.json. Profiles are copies of this. */
public class HuroConfig {
    // ---------- Visuals ----------
    public boolean hitParticles = true;
    public int hitParticleType = 0;
    public int hitParticleCount = 12;
    public boolean hitFlash = true;
    public boolean hitSound = true;
    public int hitSoundType = 0;
    public float hitSoundVolume = 1.0f;
    public float hitSoundPitch = 1.0f;
    public boolean hitKick = false;
    public float hitKickAmount = 1.5f;
    public boolean fullbright = false;
    public boolean customCrosshair = false;
    public int crosshairColor = 0;
    public int crosshairSize = 5;
    public int crosshairGap = 2;
    public int crosshairThickness = 1;
    public boolean crosshairDot = false;
    public boolean timeOverride = false;
    public int customTime = 6000;
    public int weatherMode = 0; // 0 server, 1 clear, 2 rain, 3 thunder

    // ---------- HUD ----------
    public boolean showFps = true;
    public boolean showCps = true;
    public boolean showPing = true;
    public boolean showCoords = true;
    public boolean showKeystrokes = true;
    public boolean showArmor = true;
    public boolean showPotions = true;
    public boolean showCombo = true;
    public boolean showFriendsNearby = true;
    public boolean showMarkers = true;
    public float hudScale = 1.0f;

    // ---------- Ultimate ----------
    public boolean killEffect = true;
    public boolean noEffectsOnFriends = true;

    // ---------- Optimization ----------
    // (applied directly to vanilla options; see HuroVisualClient.applyPerformancePreset)

    // ---------- persistence ----------
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HuroConfig instance;

    public static Path dir() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("huro_visual");
        try { Files.createDirectories(p); } catch (IOException ignored) {}
        return p;
    }

    public static HuroConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        Path f = dir().resolve("settings.json");
        try {
            if (Files.exists(f)) {
                HuroConfig c = GSON.fromJson(Files.readString(f), HuroConfig.class);
                instance = c != null ? c : new HuroConfig();
                return;
            }
        } catch (Exception e) {
            HuroVisualClient.LOG.warn("Failed to read settings, using defaults", e);
        }
        instance = new HuroConfig();
    }

    public static void save() {
        try {
            Files.writeString(dir().resolve("settings.json"), GSON.toJson(get()));
        } catch (IOException e) {
            HuroVisualClient.LOG.warn("Failed to save settings", e);
        }
    }

    public static void replace(HuroConfig c) {
        instance = c;
        save();
    }

    // ---------- profiles ----------
    private static Path profilesDir() {
        Path p = dir().resolve("profiles");
        try { Files.createDirectories(p); } catch (IOException ignored) {}
        return p;
    }

    private static String safe(String name) {
        return name.replaceAll("[^\\p{L}\\p{N}_\\- ]", "").trim();
    }

    public static List<String> listProfiles() {
        List<String> out = new ArrayList<>();
        try (var s = Files.list(profilesDir())) {
            s.filter(x -> x.getFileName().toString().endsWith(".json"))
             .forEach(x -> { String n = x.getFileName().toString(); out.add(n.substring(0, n.length() - 5)); });
        } catch (IOException ignored) {}
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    public static boolean saveProfile(String name) {
        name = safe(name);
        if (name.isEmpty()) return false;
        try {
            Files.writeString(profilesDir().resolve(name + ".json"), GSON.toJson(get()));
            return true;
        } catch (IOException e) { return false; }
    }

    public static boolean loadProfile(String name) {
        try {
            HuroConfig c = GSON.fromJson(Files.readString(profilesDir().resolve(safe(name) + ".json")), HuroConfig.class);
            if (c == null) return false;
            replace(c);
            return true;
        } catch (Exception e) { return false; }
    }

    public static void deleteProfile(String name) {
        try { Files.deleteIfExists(profilesDir().resolve(safe(name) + ".json")); } catch (IOException ignored) {}
    }
}
