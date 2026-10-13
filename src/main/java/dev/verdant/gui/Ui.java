package dev.verdant.gui;

import dev.verdant.module.ModuleManager;
import dev.verdant.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Small immediate-mode drawing helpers (rounded rectangles, text, icons) plus the theme palette. */
final class Ui {
    private Ui() {}

    static final int TEXT = 0xFFE8F5EE;
    static final int DIM = 0xFF86A596;
    static final int DIMMER = 0xFF5F7F6E;
    static final int BG = 0xFF050E0B;
    static final int PANEL_BORDER = 0xFF1D4430;
    static final int PANEL_TOP = 0xFF133324;
    static final int PANEL_BOT = 0xFF0C2118;
    static final int HEADER_TOP = 0xFF1F4632;
    static final int HEADER_BOT = 0xFF173726;
    static final int INSET = 0xFF0A1B13;
    static final int INSET_BORDER = 0xFF12291D;
    static final int TRACK = 0xFF1F3A2C;
    static final int OFF_TRACK = 0xFF1B3427;
    static final int PILL = 0xFF0F271B;
    static final int BADGE = 0xFF28523C;

    static float time() {
        return (System.currentTimeMillis() % 100_000_000L) / 1000f;
    }

    static int accent() {
        return 0xFF000000 | ModuleManager.INTERFACE.accent.rgb(time());
    }

    static int radius() {
        return ModuleManager.INTERFACE.cornerRadius.getI();
    }

    // ------------------------------------------------------------------ text

    static int tw(String s) {
        return Minecraft.getInstance().font.width(s);
    }

    static void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(Minecraft.getInstance().font, s, x, y, color, false);
    }

    static void textRight(GuiGraphics g, String s, int right, int y, int color) {
        text(g, s, right - tw(s), y, color);
    }

    // ------------------------------------------------------------------ rounded rectangles

    static void rrect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        rrect(g, x, y, w, h, r, r, r, r, color, color);
    }

    static void rrectV(GuiGraphics g, int x, int y, int w, int h, int r, int top, int bottom) {
        rrect(g, x, y, w, h, r, r, r, r, top, bottom);
    }

    /** Rounded rectangle with per-corner radii (tl, tr, br, bl) and a vertical gradient. */
    static void rrect(GuiGraphics g, int x, int y, int w, int h, int tl, int tr, int br, int bl, int top, int bot) {
        if (w <= 0 || h <= 0) return;
        int maxR = Math.min(w, h) / 2;
        tl = Math.min(tl, maxR);
        tr = Math.min(tr, maxR);
        br = Math.min(br, maxR);
        bl = Math.min(bl, maxR);
        int topRows = Math.max(tl, tr);
        int botRows = Math.max(bl, br);
        boolean flat = top == bot;

        for (int i = 0; i < topRows; i++) {
            span(g, x, y + i, w, inset(i, h, tl, bl), inset(i, h, tr, br), flat ? top : ColorUtil.lerp(top, bot, h <= 1 ? 0f : (float) i / (h - 1)));
        }
        int midTop = topRows;
        int midBot = h - botRows;
        if (midBot > midTop) {
            if (flat) {
                g.fill(x, y + midTop, x + w, y + midBot, top);
            } else {
                int c0 = ColorUtil.lerp(top, bot, (float) midTop / (h - 1));
                int c1 = ColorUtil.lerp(top, bot, (float) (midBot - 1) / (h - 1));
                g.fillGradient(x, y + midTop, x + w, y + midBot, c0, c1);
            }
        }
        for (int i = midBot; i < h; i++) {
            if (i < topRows) continue;
            span(g, x, y + i, w, inset(i, h, tl, bl), inset(i, h, tr, br), flat ? top : ColorUtil.lerp(top, bot, h <= 1 ? 0f : (float) i / (h - 1)));
        }
    }

    private static float inset(int i, int h, int rTop, int rBot) {
        if (rTop > 0 && i < rTop) {
            float dy = rTop - (i + 0.5f);
            return rTop - (float) Math.sqrt(Math.max(0f, (float) rTop * rTop - dy * dy));
        }
        int j = h - 1 - i;
        if (rBot > 0 && j < rBot) {
            float dy = rBot - (j + 0.5f);
            return rBot - (float) Math.sqrt(Math.max(0f, (float) rBot * rBot - dy * dy));
        }
        return 0f;
    }

    private static void span(GuiGraphics g, int x, int y, int w, float fl, float fr, int c) {
        int l = (int) Math.floor(fl);
        int r = (int) Math.floor(fr);
        int x0 = x + l;
        int x1 = x + w - r;
        if (fl - l > 0f) {
            g.fill(x0, y, x0 + 1, y + 1, ColorUtil.withAlpha(c, 1f - (fl - l)));
            x0++;
        }
        if (fr - r > 0f) {
            g.fill(x1 - 1, y, x1, y + 1, ColorUtil.withAlpha(c, 1f - (fr - r)));
            x1--;
        }
        if (x1 > x0) g.fill(x0, y, x1, y + 1, c);
    }

    static void circle(GuiGraphics g, int cx, int cy, int r, int color) {
        rrect(g, cx - r, cy - r, r * 2, r * 2, r, color);
    }

    // ------------------------------------------------------------------ small widgets / glyphs

    static void chevron(GuiGraphics g, int cx, int cy, boolean up, int color) {
        for (int i = 0; i < 4; i++) {
            int yy = up ? cy + 3 - i : cy + i;
            g.fill(cx - 3 + i, yy, cx - 2 + i, yy + 1, color);
            g.fill(cx + 2 - i, yy, cx + 3 - i, yy + 1, color);
        }
    }

    static void toggle(GuiGraphics g, int x, int y, int w, int h, float anim, int accent) {
        rrect(g, x, y, w, h, h / 2, ColorUtil.lerp(OFF_TRACK, accent, anim));
        int kr = h - 4;
        int kx = x + 2 + Math.round((w - 4 - kr) * anim);
        rrect(g, kx, y + 2, kr, kr, kr / 2, ColorUtil.lerp(0xFF5A7B6B, 0xFF0A2217, anim));
    }

    static void cross(GuiGraphics g, int cx, int cy, int color) {
        for (int i = 0; i < 8; i++) {
            g.fill(cx - 4 + i, cy - 4 + i, cx - 3 + i, cy - 3 + i, color);
            g.fill(cx - 4 + i, cy + 3 - i, cx - 3 + i, cy + 4 - i, color);
        }
    }

    static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
