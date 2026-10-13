package dev.verdant.modules;

import dev.verdant.module.Category;
import dev.verdant.module.Module;
import dev.verdant.render.EspTarget;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/** Base class for modules that periodically scan the world and expose boxes to draw. */
public abstract class EspModule extends Module {
    public EspSettings esp;
    protected final dev.verdant.setting.NumberSetting scanInterval;
    private int timer;
    private ClientLevel lastLevel;
    private volatile List<EspTarget> targets = List.of();

    protected EspModule(String name, String description) {
        super(name, description, Category.VISUALS, true);
        scanInterval = new dev.verdant.setting.NumberSetting("Scan Interval", 10, 2, 40, 2);
    }

    public List<EspTarget> targets() {
        return targets;
    }

    /** Called when the world changed or the scan interval elapsed. Must not hold on to the level. */
    protected abstract List<EspTarget> scan(Minecraft mc, ClientLevel level);

    @Override
    public void onEnable() {
        timer = Integer.MAX_VALUE;
    }

    @Override
    public void onDisable() {
        targets = List.of();
    }

    @Override
    public void onTick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            targets = List.of();
            lastLevel = null;
            return;
        }
        if (level != lastLevel) {
            lastLevel = level;
            timer = Integer.MAX_VALUE;
        }
        if (++timer < scanInterval.getI()) return;
        timer = 0;
        try {
            targets = scan(mc, level);
        } catch (RuntimeException e) {
            targets = List.of();
        }
    }
}
