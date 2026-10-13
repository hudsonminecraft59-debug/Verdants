package dev.verdant.modules;

import dev.verdant.module.Category;
import dev.verdant.module.Module;
import dev.verdant.setting.BoolSetting;
import dev.verdant.setting.ColorSetting;
import dev.verdant.setting.NumberSetting;

/** Always-on module that holds the ClickGUI appearance settings. */
public class InterfaceModule extends Module {
    public final ColorSetting accent = add(new ColorSetting("Accent Color", 0x4FE3A0));
    public final NumberSetting panelWidth = add(new NumberSetting("Panel Width", 156, 130, 200, 2));
    public final NumberSetting cornerRadius = add(new NumberSetting("Corner Radius", 10, 2, 14, 1));
    public final NumberSetting backdropDim = add(new NumberSetting("Backdrop Dim", 0.85, 0.0, 1.0, 0.05));
    public final NumberSetting animSpeed = add(new NumberSetting("Animation Speed", 1.0, 0.25, 3.0, 0.25));
    public final BoolSetting showFooter = add(new BoolSetting("Show Footer", true));

    public InterfaceModule() {
        super("Interface", "ClickGUI appearance", Category.INTERFACE, false);
    }
}
