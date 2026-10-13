package dev.verdant.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.verdant.VerdantMod;
import dev.verdant.config.ConfigManager;
import dev.verdant.module.Category;
import dev.verdant.module.Module;
import dev.verdant.module.ModuleManager;
import dev.verdant.modules.InterfaceModule;
import dev.verdant.setting.BoolSetting;
import dev.verdant.setting.ColorSetting;
import dev.verdant.setting.EnumSetting;
import dev.verdant.setting.NumberSetting;
import dev.verdant.setting.Setting;
import dev.verdant.util.ColorUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Verdant ClickGUI: rounded green panels with expandable module rows. */
public class ClickGuiScreen extends Screen {
    private static final int HEADER = 44;
    private static final int ROW = 30;
    private static final int TOP = 62;

    private interface Drag { void to(double mouseX); }

    private final List<PanelUi> panels = new ArrayList<>();
    private Drag drag;
    private float scroll, scrollTarget;
    private float ak = 0.2f; // per-frame animation factor
    private long lastNs = System.nanoTime();
    private long flashUntil;
    private int contentHeight;

    // Hit rectangles for the header controls (set during render).
    private int saveX, saveY, closeX, closeY;

    public ClickGuiScreen() {
        super(Component.literal("Verdant"));
        for (Category c : Category.values()) {
            List<Module> mods = ModuleManager.in(c);
            if (!mods.isEmpty()) panels.add(new PanelUi(c, mods));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        super.onClose();
    }

    private InterfaceModule ifc() {
        return ModuleManager.INTERFACE;
    }

    // ================================================================== rendering

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        ak = 1f - (float) Math.exp(-dt * 14f * ifc().animSpeed.getF());

        int accent = Ui.accent();
        int pw = (int) ifc().panelWidth.get();
        int gap = 14;
        int count = panels.size();
        int total = count * pw + (count - 1) * gap;
        int startX = Math.max(6, (width - total) / 2);
        int footerH = ifc().showFooter.get() ? 26 : 0;

        // backdrop
        g.fill(0, 0, width, height, ColorUtil.argb(0x050E0B, ifc().backdropDim.getF()));
        Ui.rrect(g, width - 250, height - 220, 360, 360, 180, ColorUtil.argb(accent, 0.05f));

        // scrolling
        int maxHeight = 0;
        for (PanelUi p : panels) maxHeight = Math.max(maxHeight, p.totalHeight());
        contentHeight = maxHeight;
        int viewH = height - TOP - footerH - 8;
        float maxScroll = Math.max(0, maxHeight - viewH);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll += (scrollTarget - scroll) * Math.min(1f, ak * 1.5f);

        // panels (clipped below the header and above the footer)
        g.enableScissor(0, TOP - 6, width, height - footerH);
        for (int i = 0; i < count; i++) {
            panels.get(i).render(g, startX + i * (pw + gap), TOP - Math.round(scroll), pw, mx, my);
        }
        g.disableScissor();

        drawHeader(g, startX, total, mx, my, accent);
        if (footerH > 0) drawFooter(g, startX, total, footerH, accent);
    }

    private void drawHeader(GuiGraphics g, int x, int total, int mx, int my, int accent) {
        int ly = 14;
        // logo badge with leaf
        Ui.rrect(g, x, ly, 30, 30, 10, Ui.PANEL_BORDER);
        Ui.rrect(g, x + 1, ly + 1, 28, 28, 9, 0xFF0F281C);
        Ui.rrect(g, x + 8, ly + 8, 14, 14, 7, 1, 7, 1, accent, accent);
        for (int i = 0; i < 9; i++) g.fill(x + 10 + i, ly + 20 - i, x + 12 + i, ly + 21 - i, 0xFF0F281C);
        Ui.text(g, "VERDANT", x + 40, ly + 5, Ui.TEXT);
        Ui.text(g, "Storage ESP  |  Spawn Finder", x + 40, ly + 18, Ui.DIM);

        // close button
        closeX = x + total - 24;
        closeY = ly + 3;
        boolean hc = Ui.in(mx, my, closeX, closeY, 24, 24);
        Ui.rrect(g, closeX, closeY, 24, 24, 9, hc ? 0xFF1C3D2B : Ui.PILL);
        Ui.cross(g, closeX + 12, closeY + 12, hc ? Ui.TEXT : Ui.DIM);

        // save pill
        boolean flashing = System.currentTimeMillis() < flashUntil;
        String label = flashing ? "Saved" : "Save Config";
        int pillW = Ui.tw("Save Config") + 34;
        saveX = closeX - 8 - pillW;
        saveY = closeY;
        boolean hs = Ui.in(mx, my, saveX, saveY, pillW, 24);
        Ui.rrect(g, saveX, saveY, pillW, 24, 12, Ui.PANEL_BORDER);
        Ui.rrect(g, saveX + 1, saveY + 1, pillW - 2, 22, 11, hs ? 0xFF1C3D2B : Ui.PILL);
        Ui.circle(g, saveX + 13, saveY + 12, 3, flashing ? accent : Ui.DIMMER);
        Ui.text(g, label, saveX + 23, saveY + 8, Ui.TEXT);
    }

    private void drawFooter(GuiGraphics g, int x, int total, int footerH, int accent) {
        int fy = height - footerH + 8;
        Ui.text(g, "RSHIFT toggle   |   Scroll to pan   |   Right-click a module to expand", x, fy, Ui.DIMMER);
        String right = ModuleManager.activeCount() + " active   v" + VerdantMod.VERSION;
        int rw = Ui.tw(right);
        Ui.circle(g, x + total - rw - 8, fy + 4, 2, accent);
        Ui.textRight(g, right, x + total, fy, Ui.DIM);
    }

    // ================================================================== input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int btn = event.button();

        if (Ui.in(mx, my, closeX, closeY, 24, 24)) {
            onClose();
            return true;
        }
        int pillW = Ui.tw("Save Config") + 34;
        if (Ui.in(mx, my, saveX, saveY, pillW, 24)) {
            ConfigManager.save();
            flashUntil = System.currentTimeMillis() + 1200;
            return true;
        }
        int footerH = ifc().showFooter.get() ? 26 : 0;
        if (my >= TOP - 6 && my < height - footerH) {
            for (PanelUi p : panels) {
                if (p.click(mx, my, btn)) return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        drag = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (drag != null) {
            drag.to(event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        scrollTarget -= (float) (scrollY * 30.0);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RSHIFT) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    // ================================================================== panel

    private final class PanelUi {
        final Category category;
        final List<ModuleUi> modules = new ArrayList<>();
        boolean open = true;
        float openAnim = 1f;
        int x, y, w;

        PanelUi(Category category, List<Module> mods) {
            this.category = category;
            for (Module m : mods) modules.add(new ModuleUi(m));
        }

        int bodyHeight() {
            int h = 10;
            for (ModuleUi m : modules) h += m.height() + 4;
            return h + 2;
        }

        int totalHeight() {
            return HEADER + Math.round(bodyHeight() * openAnim);
        }

        void render(GuiGraphics g, int x, int y, int w, int mx, int my) {
            this.x = x;
            this.y = y;
            this.w = w;
            openAnim += ((open ? 1f : 0f) - openAnim) * ak;
            if (Math.abs((open ? 1f : 0f) - openAnim) < 0.003f) openAnim = open ? 1f : 0f;

            int accent = Ui.accent();
            int r = Ui.radius();
            int h = totalHeight();

            Ui.rrect(g, x, y, w, h, r, Ui.PANEL_BORDER);
            Ui.rrectV(g, x + 1, y + 1, w - 2, h - 2, r - 1, Ui.PANEL_TOP, Ui.PANEL_BOT);
            int rb = Math.round((r - 1) * (1f - openAnim));
            Ui.rrect(g, x + 1, y + 1, w - 2, HEADER - 1, r - 1, r - 1, rb, rb, Ui.HEADER_TOP, Ui.HEADER_BOT);

            // icon badge + glyph
            Ui.rrect(g, x + 12, y + 11, 24, 24, 8, Ui.BADGE);
            drawGlyph(g, x + 12, y + 11, accent);
            Ui.text(g, category.title, x + 46, y + 12, Ui.TEXT);
            Ui.text(g, category.subtitle, x + 46, y + 24, Ui.DIM);
            Ui.chevron(g, x + w - 20, y + 20, open, Ui.DIM);

            if (openAnim > 0.01f) {
                g.enableScissor(x + 1, y + HEADER, x + w - 1, y + h - 1);
                g.fill(x + 1, y + HEADER, x + w - 1, y + HEADER + 1, Ui.INSET);
                int cy = y + HEADER + 8;
                for (ModuleUi m : modules) {
                    m.render(g, x, cy, w, mx, my, accent);
                    cy += m.height() + 4;
                }
                g.disableScissor();
            }
        }

        private void drawGlyph(GuiGraphics g, int bx, int by, int accent) {
            if (category == Category.VISUALS) {
                Ui.rrect(g, bx + 4, by + 7, 16, 10, 5, accent);
                Ui.rrect(g, bx + 5, by + 8, 14, 8, 4, Ui.BADGE);
                Ui.circle(g, bx + 12, by + 12, 3, accent);
            } else {
                for (int k = 0; k < 3; k++) {
                    g.fill(bx + 5, by + 7 + k * 5, bx + 19, by + 8 + k * 5, accent);
                    int kx = bx + 7 + ((k * 5) % 11);
                    Ui.circle(g, kx, by + 7 + k * 5, 2, accent);
                }
            }
        }

        boolean click(double mx, double my, int btn) {
            if (mx < x || mx >= x + w || my < y || my >= y + totalHeight()) return false;
            if (my < y + HEADER) {
                if (btn == 0 || btn == 1) open = !open;
                return true;
            }
            if (openAnim < 0.95f) return true;
            for (ModuleUi m : modules) {
                if (m.click(mx, my, btn)) return true;
            }
            return true;
        }
    }

    // ================================================================== module row

    private final class ModuleUi {
        final Module module;
        final List<SettingUi> widgets = new ArrayList<>();
        boolean expanded;
        float anim, toggleAnim;
        int x, y, w;

        ModuleUi(Module module) {
            this.module = module;
            toggleAnim = module.isEnabled() ? 1f : 0f;
            for (Setting s : module.settings()) {
                if (s instanceof BoolSetting b) widgets.add(new BoolUi(b));
                else if (s instanceof NumberSetting n) widgets.add(new NumberUi(n));
                else if (s instanceof EnumSetting e) widgets.add(new EnumUi(e));
                else if (s instanceof ColorSetting c) widgets.add(new ColorUi(c));
            }
        }

        int innerHeight() {
            int h = 10;
            for (SettingUi s : widgets) if (s.setting.isVisible()) h += s.height();
            return h + 2;
        }

        int boxHeight() {
            return Math.round(innerHeight() * anim);
        }

        int height() {
            int bh = boxHeight();
            return ROW + (bh > 0 ? 2 + bh : 0);
        }

        void render(GuiGraphics g, int x, int y, int w, int mx, int my, int accent) {
            this.x = x;
            this.y = y;
            this.w = w;
            anim += ((expanded ? 1f : 0f) - anim) * ak;
            if (Math.abs((expanded ? 1f : 0f) - anim) < 0.003f) anim = expanded ? 1f : 0f;
            toggleAnim += ((module.isEnabled() ? 1f : 0f) - toggleAnim) * ak;

            if (Ui.in(mx, my, x + 6, y, w - 12, ROW)) {
                Ui.rrect(g, x + 6, y + 1, w - 12, ROW - 2, 8, 0x14FFFFFF);
            }
            Ui.text(g, module.name, x + 16, y + 11, Ui.TEXT);
            Ui.chevron(g, x + w - 20, y + 13, expanded, Ui.DIM);
            if (module.toggleable) Ui.toggle(g, x + w - 56, y + 8, 26, 14, toggleAnim, accent);

            int bh = boxHeight();
            if (bh <= 0) return;
            int bx = x + 8;
            int by = y + ROW + 2;
            int bw = w - 16;
            Ui.rrect(g, bx, by, bw, bh, 9, Ui.INSET_BORDER);
            Ui.rrect(g, bx + 1, by + 1, bw - 2, bh - 2, 8, Ui.INSET);

            g.enableScissor(bx + 1, by + 1, bx + bw - 1, by + bh - 1);
            int wy = by + 8;
            for (SettingUi s : widgets) {
                if (!s.setting.isVisible()) continue;
                s.render(g, bx + 12, wy, bw - 24, mx, my, accent);
                wy += s.height();
            }
            g.disableScissor();
        }

        boolean click(double mx, double my, int btn) {
            if (my >= y && my < y + ROW && mx >= x && mx < x + w) {
                boolean onToggle = module.toggleable && Ui.in(mx, my, x + w - 60, y + 4, 34, 22);
                if (onToggle && btn == 0) module.toggle();
                else expanded = !expanded;
                return true;
            }
            if (anim > 0.95f && expanded) {
                for (SettingUi s : widgets) {
                    if (!s.setting.isVisible()) continue;
                    if (s.click(mx, my, btn)) return true;
                }
            }
            return false;
        }
    }

    // ================================================================== setting widgets

    private abstract class SettingUi {
        final Setting setting;
        int x, y, w;

        SettingUi(Setting setting) { this.setting = setting; }

        abstract int height();
        abstract void render(GuiGraphics g, int x, int y, int w, int mx, int my, int accent);
        boolean click(double mx, double my, int btn) { return false; }

        void place(int x, int y, int w) { this.x = x; this.y = y; this.w = w; }
    }

    private final class BoolUi extends SettingUi {
        final BoolSetting s;
        float anim;

        BoolUi(BoolSetting s) { super(s); this.s = s; anim = s.get() ? 1f : 0f; }

        @Override int height() { return 22; }

        @Override void render(GuiGraphics g, int x, int y, int w, int mx, int my, int accent) {
            place(x, y, w);
            anim += ((s.get() ? 1f : 0f) - anim) * ak;
            Ui.text(g, s.name, x, y + 7, Ui.DIM);
            Ui.toggle(g, x + w - 22, y + 5, 22, 12, anim, accent);
        }

        @Override boolean click(double mx, double my, int btn) {
            if (btn == 0 && Ui.in(mx, my, x - 4, y, w + 8, 22)) { s.toggle(); return true; }
            return false;
        }
    }

    private final class NumberUi extends SettingUi {
        final NumberSetting s;

        NumberUi(NumberSetting s) { super(s); this.s = s; }

        @Override int height() { return 30; }

        @Override void render(GuiGraphics g, int x, int y, int w, int mx, int my, int accent) {
            place(x, y, w);
            Ui.text(g, s.name, x, y + 4, Ui.DIM);
            Ui.textRight(g, s.display(), x + w, y + 4, accent);
            slider(g, x, y + 19, w, s.normalized(), accent);
        }

        @Override boolean click(double mx, double my, int btn) {
            if (btn == 0 && Ui.in(mx, my, x - 4, y + 12, w + 8, 16)) {
                drag = m -> s.set(s.min + ColorUtil.clamp01((float) ((m - x) / w)) * (s.max - s.min));
                drag.to(mx);
                return true;
            }
            return false;
        }
    }

    private void slider(GuiGraphics g, int x, int y, int w, float t, int accent) {
        t = ColorUtil.clamp01(t);
        Ui.rrect(g, x, y, w, 3, 1, Ui.TRACK);
        int fw = Math.round(w * t);
        if (fw > 0) Ui.rrect(g, x, y, fw, 3, 1, accent);
        Ui.circle(g, x + fw, y + 1, 4, 0xFFF2FBF6);
    }

    private final class EnumUi extends SettingUi {
        final EnumSetting s;
        boolean open;
        float anim;

        EnumUi(EnumSetting s) { super(s); this.s = s; }

        @Override int height() { return 22 + Math.round(s.options.length * 16 * anim); }

        @Override void render(GuiGraphics g, int x, int y, int w, int mx, int my, int accent) {
            place(x, y, w);
            anim += ((open ? 1f : 0f) - anim) * ak;
            Ui.text(g, s.name, x, y + 7, Ui.DIM);
            String v = s.get();
            int pw = Ui.tw(v) + 26;
            int px = x + w - pw;
            Ui.rrect(g, px, y + 2, pw, 18, 7, Ui.INSET_BORDER);
            Ui.rrect(g, px + 1, y + 3, pw - 2, 16, 6, Ui.PILL);
            Ui.text(g, v, px + 7, y + 7, Ui.TEXT);
            Ui.chevron(g, px + pw - 10, y + 9, open, Ui.DIM);

            if (anim > 0.01f) {
                g.enableScissor(x - 6, y + 22, x + w + 6, y + height());
                for (int i = 0; i < s.options.length; i++) {
                    int oy = y + 22 + i * 16;
                    boolean sel = i == s.index();
                    if (Ui.in(mx, my, x, oy, w, 16)) Ui.rrect(g, x - 2, oy, w + 4, 15, 6, 0x14FFFFFF);
                    Ui.text(g, s.options[i], x + 6, oy + 4, sel ? accent : Ui.DIM);
                    if (sel) Ui.circle(g, x + w - 6, oy + 7, 2, accent);
                }
                g.disableScissor();
            }
        }

        @Override boolean click(double mx, double my, int btn) {
            if (Ui.in(mx, my, x - 4, y, w + 8, 22)) { open = !open; return true; }
            if (open && anim > 0.95f) {
                for (int i = 0; i < s.options.length; i++) {
                    if (Ui.in(mx, my, x - 4, y + 22 + i * 16, w + 8, 16)) {
                        s.set(i);
                        open = false;
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private final class ColorUi extends SettingUi {
        static final int SLIDER_ROW = 20;
        static final int OPEN_EXTRA = 4 * SLIDER_ROW + 22;
        static final String[] LABELS = {"H", "S", "B", "A"};

        final ColorSetting s;
        boolean open;
        float anim;
        float rainbowAnim;

        ColorUi(ColorSetting s) { super(s); this.s = s; rainbowAnim = s.rainbow ? 1f : 0f; }

        @Override int height() { return 22 + Math.round(OPEN_EXTRA * anim); }

        private float get(int i) {
            return switch (i) { case 0 -> s.hue; case 1 -> s.sat; case 2 -> s.bri; default -> s.alpha; };
        }

        private void set(int i, float v) {
            v = ColorUtil.clamp01(v);
            switch (i) { case 0 -> s.hue = v; case 1 -> s.sat = v; case 2 -> s.bri = v; default -> s.alpha = v; }
        }

        private int trackColor(int i, float t) {
            return 0xFF000000 | switch (i) {
                case 0 -> ColorUtil.hsbToRgb(t, 1f, 1f);
                case 1 -> ColorUtil.hsbToRgb(s.hue, t, s.bri);
                case 2 -> ColorUtil.hsbToRgb(s.hue, s.sat, t);
                default -> ColorUtil.lerp(0xFF0A1B13, 0xFF000000 | s.rgb(), t) & 0xFFFFFF;
            };
        }

        @Override void render(GuiGraphics g, int x, int y, int w, int mx, int my, int accent) {
            place(x, y, w);
            anim += ((open ? 1f : 0f) - anim) * ak;
            rainbowAnim += ((s.rainbow ? 1f : 0f) - rainbowAnim) * ak;

            int shown = 0xFF000000 | s.rgb(Ui.time());
            String hex = s.rainbow ? "Rainbow" : s.hex();
            Ui.text(g, s.name, x, y + 7, Ui.DIM);
            Ui.textRight(g, hex, x + w, y + 7, Ui.TEXT);
            Ui.circle(g, x + w - Ui.tw(hex) - 9, y + 11, 4, shown);

            if (anim > 0.01f) {
                g.enableScissor(x - 6, y + 22, x + w + 6, y + height());
                int tx = x + 12;
                int tw = w - 12 - 34;
                for (int i = 0; i < 4; i++) {
                    int sy = y + 22 + i * SLIDER_ROW;
                    Ui.text(g, LABELS[i], x, sy + 6, Ui.DIMMER);
                    for (int px = 0; px < tw; px += 2) {
                        int c = trackColor(i, tw <= 1 ? 0f : (float) px / (tw - 1));
                        g.fill(tx + px, sy + 8, tx + Math.min(px + 2, tw), sy + 12, c);
                    }
                    int kx = tx + Math.round(tw * get(i));
                    Ui.circle(g, kx, sy + 10, 4, 0xFFF2FBF6);
                    String val = i == 0 ? Math.round(get(i) * 360f) + "d" : Math.round(get(i) * 100f) + "%";
                    Ui.textRight(g, val, x + w, sy + 6, Ui.DIM);
                }
                int ry = y + 22 + 4 * SLIDER_ROW;
                Ui.text(g, "Rainbow", x, ry + 7, Ui.DIM);
                Ui.toggle(g, x + w - 22, ry + 5, 22, 12, rainbowAnim, accent);
                g.disableScissor();
            }
        }

        @Override boolean click(double mx, double my, int btn) {
            if (Ui.in(mx, my, x - 4, y, w + 8, 22)) { open = !open; return true; }
            if (!open || anim < 0.95f) return false;
            int tx = x + 12;
            int tw = w - 12 - 34;
            for (int i = 0; i < 4; i++) {
                int sy = y + 22 + i * SLIDER_ROW;
                if (btn == 0 && Ui.in(mx, my, tx - 5, sy + 3, tw + 10, 14)) {
                    final int idx = i;
                    drag = m -> set(idx, (float) ((m - tx) / tw));
                    drag.to(mx);
                    return true;
                }
            }
            int ry = y + 22 + 4 * SLIDER_ROW;
            if (btn == 0 && Ui.in(mx, my, x - 4, ry, w + 8, 22)) { s.rainbow = !s.rainbow; return true; }
            return false;
        }
    }
}
