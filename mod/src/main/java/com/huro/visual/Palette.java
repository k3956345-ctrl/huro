package com.huro.visual;

/** Shared colors (ARGB) for the menu, HUD and crosshair. */
public final class Palette {
    private Palette() {}

    public static final int BG        = 0xF00E0818;
    public static final int PANEL     = 0xFF161026;
    public static final int PANEL_2   = 0xFF1E1535;
    public static final int HOVER     = 0xFF2A1D4A;
    public static final int ACCENT    = 0xFF9B5CFF;
    public static final int ACCENT_2  = 0xFF6D28D9;
    public static final int TEXT      = 0xFFEDE7FF;
    public static final int TEXT_DIM  = 0xFF9C8FC4;
    public static final int HUD_BG    = 0x90140A24;

    public static final String[] COLOR_NAMES = {"Фиолетовый", "Белый", "Красный", "Зелёный", "Голубой", "Жёлтый", "Розовый"};
    public static final int[] COLORS = {
        0xFFB57CFF, 0xFFFFFFFF, 0xFFFF4D4D, 0xFF4DFF88, 0xFF4DD2FF, 0xFFFFE14D, 0xFFFF7AD9
    };

    public static int color(int idx) {
        return COLORS[Math.floorMod(idx, COLORS.length)];
    }
}
