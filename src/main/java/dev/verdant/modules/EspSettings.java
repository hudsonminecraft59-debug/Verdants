package dev.verdant.modules;

import dev.verdant.module.Module;
import dev.verdant.render.EspTarget;
import dev.verdant.setting.BoolSetting;
import dev.verdant.setting.ColorSetting;
import dev.verdant.setting.EnumSetting;
import dev.verdant.setting.NumberSetting;
import dev.verdant.setting.Setting;
import java.util.function.BooleanSupplier;

/** The shared block of look-and-feel options used by both ESP style modules. */
public class EspSettings {
    public final EnumSetting mode;
    public final EnumSetting outlineStyle;
    public final NumberSetting cornerLength;
    public final NumberSetting outlineWidth;
    public final BoolSetting screenConstant;
    public final EnumSetting outlineColorMode;
    public final ColorSetting outlineColor;
    public final NumberSetting outlineOpacity;
    public final EnumSetting fillColorMode;
    public final ColorSetting fillColor;
    public final NumberSetting fillOpacity;
    public final BoolSetting fillGradient;
    public final NumberSetting gradientStrength;
    public final NumberSetting expand;
    public final BoolSetting throughWalls;
    public final NumberSetting range;
    public final BoolSetting distanceFade;
    public final NumberSetting fadeStrength;
    public final BoolSetting pulse;
    public final NumberSetting pulseSpeed;
    public final NumberSetting pulseDepth;
    public final NumberSetting rainbowSpeed;

    public EspSettings(Module m, boolean defaultFill) {
        mode = m.add(new EnumSetting("Render Mode", "Both", "Outline", "Fill", "Both"));
        outlineStyle = m.add(new EnumSetting("Outline Style", "Solid", "Solid", "Corners"));
        vis(outlineStyle, this::outlineOn);
        cornerLength = m.add(new NumberSetting("Corner Length", 0.25, 0.1, 0.5, 0.05));
        vis(cornerLength, () -> outlineOn() && outlineStyle.is("Corners"));
        outlineWidth = m.add(new NumberSetting("Outline Width", 2.0, 0.5, 10.0, 0.5));
        vis(outlineWidth, this::outlineOn);
        screenConstant = m.add(new BoolSetting("Constant Screen Width", true));
        vis(screenConstant, this::outlineOn);
        outlineColorMode = m.add(new EnumSetting("Outline Color", "Match", "Match", "Custom", "Rainbow"));
        vis(outlineColorMode, this::outlineOn);
        outlineColor = m.add(new ColorSetting("Custom Outline", 0xFFFFFF));
        vis(outlineColor, () -> outlineOn() && outlineColorMode.is("Custom"));
        outlineOpacity = m.add(new NumberSetting("Outline Opacity", 1.0, 0.05, 1.0, 0.05));
        vis(outlineOpacity, this::outlineOn);

        fillColorMode = m.add(new EnumSetting("Fill Color", "Match", "Match", "Custom", "Rainbow"));
        vis(fillColorMode, this::fillOn);
        fillColor = m.add(new ColorSetting("Custom Fill", 0xFFFFFF));
        vis(fillColor, () -> fillOn() && fillColorMode.is("Custom"));
        fillOpacity = m.add(new NumberSetting("Fill Opacity", defaultFill ? 0.25 : 0.15, 0.0, 1.0, 0.05));
        vis(fillOpacity, this::fillOn);
        fillGradient = m.add(new BoolSetting("Fill Gradient", false));
        vis(fillGradient, this::fillOn);
        gradientStrength = m.add(new NumberSetting("Gradient Strength", 0.6, 0.0, 1.0, 0.05));
        vis(gradientStrength, () -> fillOn() && fillGradient.get());

        expand = m.add(new NumberSetting("Expand", 0.002, -0.1, 0.3, 0.005));
        throughWalls = m.add(new BoolSetting("Through Walls", true));
        range = m.add(new NumberSetting("Range", 128, 16, 256, 8));
        distanceFade = m.add(new BoolSetting("Distance Fade", true));
        fadeStrength = m.add(new NumberSetting("Fade Strength", 0.7, 0.0, 1.0, 0.05));
        vis(fadeStrength, distanceFade::get);
        pulse = m.add(new BoolSetting("Pulse", false));
        pulseSpeed = m.add(new NumberSetting("Pulse Speed", 1.0, 0.1, 4.0, 0.1));
        vis(pulseSpeed, pulse::get);
        pulseDepth = m.add(new NumberSetting("Pulse Depth", 0.5, 0.0, 1.0, 0.05));
        vis(pulseDepth, pulse::get);
        rainbowSpeed = m.add(new NumberSetting("Rainbow Speed", 1.0, 0.1, 5.0, 0.1));
        vis(rainbowSpeed, this::anyRainbow);
    }

    private static void vis(Setting s, BooleanSupplier v) {
        s.visibleWhen(v);
    }

    public boolean outlineOn() { return !mode.is("Fill"); }
    public boolean fillOn() { return !mode.is("Outline"); }

    private boolean anyRainbow() {
        return (outlineOn() && outlineColorMode.is("Rainbow")) || (fillOn() && fillColorMode.is("Rainbow"));
    }

    private int baseRgb(EspTarget t, float time) {
        return t.color() != null ? t.color().rgb(time) : t.fixedRgb();
    }

    public int outlineRgb(EspTarget t, float time) {
        if (outlineColorMode.is("Custom")) return outlineColor.rgb(time);
        if (outlineColorMode.is("Rainbow")) return rainbowRgb(time);
        return baseRgb(t, time);
    }

    public int fillRgb(EspTarget t, float time) {
        if (fillColorMode.is("Custom")) return fillColor.rgb(time);
        if (fillColorMode.is("Rainbow")) return rainbowRgb(time);
        return baseRgb(t, time);
    }

    private int rainbowRgb(float time) {
        return dev.verdant.util.ColorUtil.hsbToRgb(time * 0.1f * rainbowSpeed.getF(), 0.7f, 1f);
    }

    /** Alpha multiplier contributed by the source color setting (its alpha slider). */
    public float sourceAlpha(EspTarget t, boolean forOutline) {
        float a = t.color() != null ? t.color().alpha : 1f;
        if (forOutline && outlineColorMode.is("Custom")) a = outlineColor.alpha;
        if (!forOutline && fillColorMode.is("Custom")) a = fillColor.alpha;
        return a;
    }
}
