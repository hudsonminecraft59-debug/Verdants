package dev.verdant.module;

import dev.verdant.modules.InterfaceModule;
import dev.verdant.modules.SpawnFinder;
import dev.verdant.modules.StorageEsp;
import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final StorageEsp STORAGE_ESP = new StorageEsp();
    public static final SpawnFinder SPAWN_FINDER = new SpawnFinder();
    public static final InterfaceModule INTERFACE = new InterfaceModule();

    private static final List<Module> ALL = new ArrayList<>(List.of(STORAGE_ESP, SPAWN_FINDER, INTERFACE));

    private ModuleManager() {}

    public static List<Module> all() { return ALL; }

    public static List<Module> in(Category category) {
        List<Module> out = new ArrayList<>();
        for (Module m : ALL) if (m.category == category) out.add(m);
        return out;
    }

    public static int activeCount() {
        int n = 0;
        for (Module m : ALL) if (m.toggleable && m.isEnabled()) n++;
        return n;
    }
}
