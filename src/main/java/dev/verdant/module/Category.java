package dev.verdant.module;

public enum Category {
    VISUALS("Visuals", "Render and ESP"),
    INTERFACE("Interface", "Look and feel");

    public final String title;
    public final String subtitle;

    Category(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }
}
