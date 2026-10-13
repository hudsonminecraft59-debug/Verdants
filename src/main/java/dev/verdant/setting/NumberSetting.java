package dev.verdant.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting {
    public final double min, max, step;
    private double value;

    public NumberSetting(String name, double def, double min, double max, double step) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = def;
    }

    public double get() { return value; }
    public float getF() { return (float) value; }
    public int getI() { return (int) Math.round(value); }

    public void set(double v) {
        v = Math.max(min, Math.min(max, v));
        if (step > 0) v = min + Math.round((v - min) / step) * step;
        value = Math.max(min, Math.min(max, v));
    }

    public float normalized() {
        return (float) ((value - min) / (max - min));
    }

    public String display() {
        if (step >= 1.0) return Integer.toString((int) Math.round(value));
        if (step >= 0.1) return String.format("%.1f", value);
        return String.format("%.2f", value);
    }

    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement e) {
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) set(e.getAsDouble());
    }
}
