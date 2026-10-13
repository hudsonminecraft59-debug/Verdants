package dev.verdant.setting;

import com.google.gson.JsonElement;
import java.util.function.BooleanSupplier;

public abstract class Setting {
    public final String name;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name) {
        this.name = name;
    }

    public void visibleWhen(BooleanSupplier supplier) {
        this.visible = supplier;
    }

    public boolean isVisible() {
        return visible.getAsBoolean();
    }

    public abstract JsonElement save();

    public abstract void load(JsonElement element);
}
