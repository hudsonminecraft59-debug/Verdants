package dev.verdant.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.verdant.module.Module;
import dev.verdant.module.ModuleManager;
import dev.verdant.setting.Setting;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigManager() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("verdant.json");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            for (Module m : ModuleManager.all()) {
                if (!root.has(m.name)) continue;
                JsonObject mo = root.getAsJsonObject(m.name);
                if (m.toggleable && mo.has("enabled")) m.setEnabled(mo.get("enabled").getAsBoolean());
                if (!mo.has("settings")) continue;
                JsonObject so = mo.getAsJsonObject("settings");
                for (Setting s : m.settings()) {
                    if (so.has(s.name)) s.load(so.get(s.name));
                }
            }
        } catch (Exception e) {
            System.err.println("[Verdant] Failed to load config: " + e);
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        for (Module m : ModuleManager.all()) {
            JsonObject mo = new JsonObject();
            mo.addProperty("enabled", m.isEnabled());
            JsonObject so = new JsonObject();
            for (Setting s : m.settings()) so.add(s.name, s.save());
            mo.add("settings", so);
            root.add(m.name, mo);
        }
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(root, w);
        } catch (Exception e) {
            System.err.println("[Verdant] Failed to save config: " + e);
        }
    }
}
