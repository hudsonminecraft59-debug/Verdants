package dev.verdant.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class EnumSetting extends Setting {
    public final String[] options;
    private int index;

    public EnumSetting(String name, String def, String... options) {
        super(name);
        this.options = options;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(def)) index = i;
        }
    }

    public String get() { return options[index]; }
    public int index() { return index; }
    public void set(int i) { index = Math.max(0, Math.min(options.length - 1, i)); }
    public boolean is(String s) { return options[index].equals(s); }

    @Override public JsonElement save() { return new JsonPrimitive(options[index]); }
    @Override public void load(JsonElement e) {
        if (e == null || !e.isJsonPrimitive()) return;
        String s = e.getAsString();
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(s)) { index = i; return; }
        }
    }
}
