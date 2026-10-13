package dev.verdant.render;

import dev.verdant.setting.ColorSetting;
import net.minecraft.world.phys.AABB;

/**
 * One highlighted box. If {@code color} is non-null it supplies the RGB (and the alpha multiplier),
 * otherwise {@code fixedRgb} is used (e.g. dyed shulker boxes).
 */
public record EspTarget(AABB box, ColorSetting color, int fixedRgb, boolean outlineOnly) {
    public static EspTarget of(AABB box, ColorSetting color) {
        return new EspTarget(box, color, 0xFFFFFF, false);
    }

    public static EspTarget fixed(AABB box, int rgb) {
        return new EspTarget(box, null, rgb, false);
    }
}
