package dev.verdant.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.verdant.util.ColorUtil;

/** HSB color with alpha and an optional animated rainbow mode. */
public class ColorSetting extends Setting {
    public float hue, sat, bri, alpha = 1f;
    public boolean rainbow;
    public float rainbowSpeed = 1f;

    public ColorSetting(String name, int rgb) {
        super(name);
        setRgb(rgb);
    }

    public void setRgb(int rgb) {
        float[] hsb = ColorUtil.rgbToHsb(rgb);
        hue = hsb[0];
        sat = hsb[1];
        bri = hsb[2];
    }

    /** 0xRRGGBB (static color, ignores rainbow). */
    public int rgb() {
        return ColorUtil.hsbToRgb(hue, sat, bri);
    }

    /** 0xRRGGBB, animated when rainbow is enabled. */
    public int rgb(float timeSec) {
        if (rainbow) return ColorUtil.hsbToRgb(timeSec * 0.1f * rainbowSpeed, Math.max(0.55f, sat), Math.max(0.8f, bri));
        return rgb();
    }

    public String hex() {
        return String.format("#%06X", rgb());
    }

    @Override public JsonElement save() {
        JsonObject o = new JsonObject();
        o.addProperty("h", hue);
        o.addProperty("s", sat);
        o.addProperty("b", bri);
        o.addProperty("a", alpha);
        o.addProperty("rainbow", rainbow);
        return o;
    }

    @Override public void load(JsonElement e) {
        if (e == null || !e.isJsonObject()) return;
        JsonObject o = e.getAsJsonObject();
        if (o.has("h")) hue = o.get("h").getAsFloat();
        if (o.has("s")) sat = o.get("s").getAsFloat();
        if (o.has("b")) bri = o.get("b").getAsFloat();
        if (o.has("a")) alpha = o.get("a").getAsFloat();
        if (o.has("rainbow")) rainbow = o.get("rainbow").getAsBoolean();
    }
}
