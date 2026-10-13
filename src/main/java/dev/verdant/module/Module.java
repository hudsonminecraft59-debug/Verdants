package dev.verdant.module;

import dev.verdant.setting.Setting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;
    public final boolean toggleable;
    private boolean enabled;
    private final List<Setting> settings = new ArrayList<>();

    protected Module(String name, String description, Category category, boolean toggleable) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.toggleable = toggleable;
        this.enabled = !toggleable;
    }

    public <T extends Setting> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting> settings() { return settings; }
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean value) {
        if (!toggleable || value == enabled) return;
        enabled = value;
        if (value) onEnable(); else onDisable();
    }

    public void toggle() { setEnabled(!enabled); }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick(Minecraft mc) {}
}
