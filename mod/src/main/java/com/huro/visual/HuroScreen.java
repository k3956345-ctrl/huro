package com.huro.visual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** HURO VISUAL menu (Right Shift). */
public class HuroScreen extends Screen {
    private static final String[] TABS = {"Visuals", "HUD", "Ultimate", "Optimization", "Friends", "Markers"};
    private static final int SIDE = 92, RH = 20, GAP = 3;
    private static int lastTab = 0;

    private enum T { TOGGLE, SLIDER, CYCLE, BUTTON, HEADER, ITEM, INPUT }

    private static final class Row {
        final T type; final String label;
        BooleanSupplier getB; Consumer<Boolean> setB;
        DoubleSupplier getD; DoubleConsumer setD; double min, max, step;
        String[] opts; IntSupplier getI; IntConsumer setI;
        Runnable action, delete;
        Row(T type, String label) { this.type = type; this.label = label; }
    }

    private int tab = lastTab;
    private double scroll = 0;
    private List<Row> rows = new ArrayList<>();
    private String input = "";
    private boolean inputFocus = false;
    private Row dragging;
    private int px, py, pw, ph, cx, cy, cw, ch;

    public HuroScreen() {
        super(Text.literal("HURO VISUAL"));
        rebuild();
    }

    // ================================================================ row factories

    private Row toggle(String l, BooleanSupplier g, Consumer<Boolean> s) {
        Row r = new Row(T.TOGGLE, l); r.getB = g; r.setB = s; return r;
    }
    private Row slider(String l, double min, double max, double step, DoubleSupplier g, DoubleConsumer s) {
        Row r = new Row(T.SLIDER, l); r.min = min; r.max = max; r.step = step; r.getD = g; r.setD = s; return r;
    }
    private Row cycle(String l, String[] opts, IntSupplier g, IntConsumer s) {
        Row r = new Row(T.CYCLE, l); r.opts = opts; r.getI = g; r.setI = s; return r;
    }
    private Row button(String l, Runnable a) { Row r = new Row(T.BUTTON, l); r.action = a; return r; }
    private Row header(String l) { return new Row(T.HEADER, l); }
    private Row item(String l, Runnable click, Runnable del) {
        Row r = new Row(T.ITEM, l); r.action = click; r.delete = del; return r;
    }
    private Row inputRow(String placeholder) { return new Row(T.INPUT, placeholder); }

    // ================================================================ tab content

    private void rebuild() {
        HuroConfig c = HuroConfig.get();
        HuroData d = HuroData.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Row> r = new ArrayList<>();

        switch (tab) {
            case 0 -> {
                r.add(header("ЭФФЕКТЫ УДАРОВ"));
                r.add(toggle("Частицы при ударе", () -> c.hitParticles, v -> c.hitParticles = v));
                r.add(cycle("Тип частиц", HuroVisualClient.PARTICLE_NAMES, () -> c.hitParticleType, v -> c.hitParticleType = v));
                r.add(slider("Кол-во частиц", 1, 60, 1, () -> c.hitParticleCount, v -> c.hitParticleCount = (int) v));
                r.add(toggle("Вспышка экрана", () -> c.hitFlash, v -> c.hitFlash = v));
                r.add(header("ЗВУК УДАРА"));
                r.add(toggle("Hit Sound", () -> c.hitSound, v -> c.hitSound = v));
                r.add(cycle("Звук", HuroVisualClient.SOUND_NAMES, () -> c.hitSoundType, v -> c.hitSoundType = v));
                r.add(slider("Громкость", 0, 2, 0.05, () -> c.hitSoundVolume, v -> c.hitSoundVolume = (float) v));
                r.add(slider("Тон", 0.5, 2, 0.05, () -> c.hitSoundPitch, v -> c.hitSoundPitch = (float) v));
                r.add(button("Проверить звук", () -> mc.getSoundManager().play(PositionedSoundInstance.master(
                    HuroVisualClient.SOUNDS[Math.floorMod(c.hitSoundType, HuroVisualClient.SOUNDS.length)], c.hitSoundPitch, c.hitSoundVolume))));
                r.add(header("АНИМАЦИИ"));
                r.add(toggle("Рывок камеры при ударе", () -> c.hitKick, v -> c.hitKick = v));
                r.add(slider("Сила рывка", 0.5, 6, 0.1, () -> c.hitKickAmount, v -> c.hitKickAmount = (float) v));
                r.add(header("МИР"));
                r.add(toggle("Fullbright", () -> c.fullbright, v -> c.fullbright = v));
                r.add(toggle("Своё время суток", () -> c.timeOverride, v -> c.timeOverride = v));
                r.add(slider("Время (тики)", 0, 24000, 250, () -> c.customTime, v -> c.customTime = (int) v));
                r.add(cycle("Погода", HuroVisualClient.WEATHER_NAMES, () -> c.weatherMode, v -> c.weatherMode = v));
                r.add(header("ПРИЦЕЛ"));
                r.add(toggle("Свой прицел", () -> c.customCrosshair, v -> c.customCrosshair = v));
                r.add(cycle("Цвет прицела", Palette.COLOR_NAMES, () -> c.crosshairColor, v -> c.crosshairColor = v));
                r.add(slider("Размер", 1, 15, 1, () -> c.crosshairSize, v -> c.crosshairSize = (int) v));
                r.add(slider("Зазор", 0, 10, 1, () -> c.crosshairGap, v -> c.crosshairGap = (int) v));
                r.add(slider("Толщина", 1, 4, 1, () -> c.crosshairThickness, v -> c.crosshairThickness = (int) v));
                r.add(toggle("Точка в центре", () -> c.crosshairDot, v -> c.crosshairDot = v));
            }
            case 1 -> {
                r.add(header("ЭЛЕМЕНТЫ HUD"));
                r.add(toggle("FPS", () -> c.showFps, v -> c.showFps = v));
                r.add(toggle("CPS", () -> c.showCps, v -> c.showCps = v));
                r.add(toggle("Ping", () -> c.showPing, v -> c.showPing = v));
                r.add(toggle("Координаты", () -> c.showCoords, v -> c.showCoords = v));
                r.add(toggle("Keystrokes", () -> c.showKeystrokes, v -> c.showKeystrokes = v));
                r.add(toggle("Броня", () -> c.showArmor, v -> c.showArmor = v));
                r.add(toggle("Зелья", () -> c.showPotions, v -> c.showPotions = v));
                r.add(toggle("Счётчик комбо", () -> c.showCombo, v -> c.showCombo = v));
                r.add(slider("Размер HUD", 0.5, 2, 0.1, () -> c.hudScale, v -> c.hudScale = (float) v));
            }
            case 2 -> {
                r.add(header("ULTIMATE"));
                r.add(toggle("Эффект убийства", () -> c.killEffect, v -> c.killEffect = v));
                r.add(button("Ultimate-пресет (всё красиво)", () -> { HuroVisualClient.applyUltimatePreset(); rebuild(); }));
                r.add(button("Сбросить все настройки", () -> { HuroConfig.replace(new HuroConfig()); rebuild(); }));
                r.add(header("ПРОФИЛИ"));
                r.add(inputRow("Название профиля..."));
                r.add(button("Сохранить текущие настройки как профиль", () -> {
                    if (HuroConfig.saveProfile(input)) { input = ""; rebuild(); }
                }));
                for (String p : HuroConfig.listProfiles()) {
                    r.add(item("Профиль: " + p, () -> { if (HuroConfig.loadProfile(p)) rebuild(); },
                        () -> { HuroConfig.deleteProfile(p); rebuild(); }));
                }
            }
            case 3 -> {
                r.add(header("ПРОИЗВОДИТЕЛЬНОСТЬ (меняет настройки Minecraft)"));
                r.add(button("Применить FPS-пресет", () -> { HuroVisualClient.applyPerformancePreset(mc); rebuild(); }));
                r.add(slider("Дальность прорисовки", 2, 32, 1, () -> mc.options.getViewDistance().getValue(), v -> mc.options.getViewDistance().setValue((int) v)));
                r.add(slider("Лимит FPS (260 = без)", 30, 260, 10, () -> mc.options.getMaxFps().getValue(), v -> mc.options.getMaxFps().setValue((int) v)));
                r.add(toggle("VSync", () -> mc.options.getEnableVsync().getValue(), v -> mc.options.getEnableVsync().setValue(v)));
                r.add(toggle("Облака", () -> mc.options.getCloudRenderMode().getValue() != CloudRenderMode.OFF,
                    v -> mc.options.getCloudRenderMode().setValue(v ? CloudRenderMode.FANCY : CloudRenderMode.OFF)));
                r.add(toggle("Тени сущностей", () -> mc.options.getEntityShadows().getValue(), v -> mc.options.getEntityShadows().setValue(v)));
                r.add(cycle("Частицы Minecraft", new String[]{"Все", "Меньше", "Минимум"},
                    () -> mc.options.getParticles().getValue().ordinal(),
                    v -> mc.options.getParticles().setValue(ParticlesMode.values()[v])));
            }
            case 4 -> {
                r.add(header("ДРУЗЬЯ"));
                r.add(toggle("Показывать друзей рядом", () -> c.showFriendsNearby, v -> c.showFriendsNearby = v));
                r.add(toggle("Без эффектов ударов по друзьям", () -> c.noEffectsOnFriends, v -> c.noEffectsOnFriends = v));
                r.add(inputRow("Ник игрока..."));
                r.add(button("Добавить друга", () -> { if (d.addFriend(input)) { input = ""; rebuild(); } }));
                r.add(button("Добавить игрока под прицелом", () -> {
                    Entity t = mc.targetedEntity;
                    if (t instanceof PlayerEntity p && d.addFriend(p.getName().getString())) rebuild();
                }));
                for (String f : new ArrayList<>(d.friends)) {
                    r.add(item(f, null, () -> { d.removeFriend(f); rebuild(); }));
                }
            }
            default -> {
                r.add(header("МЕТКИ"));
                r.add(toggle("Показывать метки", () -> c.showMarkers, v -> c.showMarkers = v));
                r.add(inputRow("Название метки..."));
                r.add(button("Поставить метку здесь (клавиша J)", () -> {
                    if (mc.player != null && mc.world != null) {
                        d.addMarker(input, mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            mc.world.getRegistryKey().getValue().toString());
                        input = "";
                        rebuild();
                    }
                }));
                for (int i = 0; i < d.markers.size(); i++) {
                    final int idx = i;
                    HuroData.Marker m = d.markers.get(i);
                    r.add(item(String.format("%s  (%d, %d, %d)", m.name, (int) m.x, (int) m.y, (int) m.z), null,
                        () -> { d.removeMarker(idx); rebuild(); }));
                }
            }
        }
        rows = r;
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    // ================================================================ layout

    private void layout() {
        pw = Math.min(480, width - 10);
        ph = Math.min(310, height - 10);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        cx = px + SIDE + 10;
        cy = py + 36;
        cw = pw - SIDE - 18;
        ch = ph - 44;
    }

    private int rowY(int i) { return cy + i * (RH + GAP) - (int) scroll; }
    private int maxScroll() { return Math.max(0, rows.size() * (RH + GAP) - Math.max(ch, 50)); }
    private int trackX() { return cx + cw - 150; }
    private static final int TRACK_W = 100;

    // ================================================================ rendering

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        layout();
        ctx.fill(0, 0, width, height, 0xA0050210);
        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, Palette.ACCENT_2);
        ctx.fill(px, py, px + pw, py + ph, Palette.BG);
        ctx.fill(px, py, px + pw, py + 30, Palette.PANEL);
        ctx.fill(px, py + 30, px + pw, py + 31, Palette.ACCENT_2);

        // logo "H"
        ctx.fill(px + 8, py + 5, px + 28, py + 25, Palette.ACCENT);
        ctx.fill(px + 8, py + 5, px + 28, py + 7, 0xFFC9A5FF);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(px + 13, py + 9, 0);
        ctx.getMatrices().scale(1.6f, 1.6f, 1f);
        ctx.drawText(textRenderer, "H", 0, 0, 0xFFFFFFFF, true);
        ctx.getMatrices().pop();
        ctx.drawText(textRenderer, "HURO VISUAL", px + 36, py + 7, Palette.TEXT, true);
        ctx.drawText(textRenderer, "Minecraft 1.21.4 · Fabric", px + 36, py + 18, Palette.TEXT_DIM, false);

        // tabs
        for (int i = 0; i < TABS.length; i++) {
            int ty = py + 38 + i * 26;
            boolean sel = i == tab;
            boolean hov = mx >= px + 6 && mx < px + SIDE && my >= ty && my < ty + 22;
            ctx.fill(px + 6, ty, px + SIDE, ty + 22, sel ? Palette.ACCENT_2 : hov ? Palette.HOVER : Palette.PANEL);
            if (sel) ctx.fill(px + 6, ty, px + 8, ty + 22, Palette.ACCENT);
            ctx.drawText(textRenderer, TABS[i], px + 14, ty + 7, sel ? 0xFFFFFFFF : Palette.TEXT_DIM, false);
        }
        ctx.drawText(textRenderer, "RShift: закрыть", px + 8, py + ph - 24, Palette.TEXT_DIM, false);
        ctx.drawText(textRenderer, "J: метка здесь", px + 8, py + ph - 14, Palette.TEXT_DIM, false);

        // rows
        ctx.enableScissor(cx - 2, cy - 2, cx + cw + 2, cy + ch + 2);
        for (int i = 0; i < rows.size(); i++) {
            int y = rowY(i);
            if (y + RH < cy - 2 || y > cy + ch) continue;
            renderRow(ctx, rows.get(i), cx, y, mx, my);
        }
        ctx.disableScissor();

        int ms = maxScroll();
        if (ms > 0) {
            int barH = Math.max(18, (int) (ch * (ch / (double) (ch + ms))));
            int barY = cy + (int) ((ch - barH) * (scroll / ms));
            ctx.fill(cx + cw + 3, cy, cx + cw + 5, cy + ch, Palette.PANEL);
            ctx.fill(cx + cw + 3, barY, cx + cw + 5, barY + barH, Palette.ACCENT);
        }
    }

    private void renderRow(DrawContext ctx, Row r, int x, int y, int mx, int my) {
        boolean hov = mx >= x && mx < x + cw && my >= y && my < y + RH && my >= cy && my <= cy + ch;
        int ty = y + (RH - 8) / 2;
        switch (r.type) {
            case HEADER -> ctx.drawText(textRenderer, r.label, x + 2, ty + 2, Palette.ACCENT, false);
            case TOGGLE -> {
                boolean on = r.getB.getAsBoolean();
                ctx.fill(x, y, x + cw, y + RH, hov ? Palette.HOVER : Palette.PANEL_2);
                ctx.drawText(textRenderer, r.label, x + 8, ty, Palette.TEXT, false);
                int sx = x + cw - 34, sy = y + 4;
                ctx.fill(sx, sy, sx + 26, sy + 12, on ? Palette.ACCENT : 0xFF3A2F57);
                int kx = on ? sx + 15 : sx + 1;
                ctx.fill(kx, sy + 1, kx + 10, sy + 11, 0xFFFFFFFF);
            }
            case SLIDER -> {
                double v = r.getD.getAsDouble();
                double frac = (v - r.min) / (r.max - r.min);
                ctx.fill(x, y, x + cw, y + RH, hov || dragging == r ? Palette.HOVER : Palette.PANEL_2);
                ctx.drawText(textRenderer, r.label, x + 8, ty, Palette.TEXT, false);
                int tx = x + cw - 150;
                ctx.fill(tx, y + 9, tx + TRACK_W, y + 11, 0xFF3A2F57);
                ctx.fill(tx, y + 9, tx + (int) (TRACK_W * Math.max(0, Math.min(1, frac))), y + 11, Palette.ACCENT);
                int kx = tx + (int) (TRACK_W * Math.max(0, Math.min(1, frac)));
                ctx.fill(kx - 2, y + 5, kx + 2, y + 15, 0xFFFFFFFF);
                String s = r.step >= 1 ? String.valueOf((int) Math.round(v)) : String.format("%.2f", v);
                ctx.drawText(textRenderer, s, x + cw - 44 + 2, ty, Palette.TEXT_DIM, false);
            }
            case CYCLE -> {
                ctx.fill(x, y, x + cw, y + RH, hov ? Palette.HOVER : Palette.PANEL_2);
                ctx.drawText(textRenderer, r.label, x + 8, ty, Palette.TEXT, false);
                String s = "< " + r.opts[Math.floorMod(r.getI.getAsInt(), r.opts.length)] + " >";
                ctx.drawText(textRenderer, s, x + cw - 8 - textRenderer.getWidth(s), ty, Palette.ACCENT, false);
            }
            case BUTTON -> {
                ctx.fill(x, y, x + cw, y + RH, hov ? Palette.ACCENT : Palette.ACCENT_2);
                ctx.drawText(textRenderer, r.label, x + (cw - textRenderer.getWidth(r.label)) / 2, ty, 0xFFFFFFFF, false);
            }
            case ITEM -> {
                ctx.fill(x, y, x + cw, y + RH, hov ? Palette.HOVER : Palette.PANEL_2);
                ctx.drawText(textRenderer, r.label, x + 8, ty, Palette.TEXT, false);
                if (r.delete != null) {
                    boolean hx = hov && mx > x + cw - 22;
                    ctx.fill(x + cw - 20, y + 2, x + cw - 2, y + RH - 2, hx ? 0xFFB3261E : 0xFF4A1A2E);
                    ctx.drawText(textRenderer, "x", x + cw - 13, ty, 0xFFFFFFFF, false);
                }
            }
            case INPUT -> {
                ctx.fill(x, y, x + cw, y + RH, inputFocus ? Palette.ACCENT : Palette.ACCENT_2);
                ctx.fill(x + 1, y + 1, x + cw - 1, y + RH - 1, 0xFF0A0614);
                if (input.isEmpty() && !inputFocus) {
                    ctx.drawText(textRenderer, r.label, x + 6, ty, Palette.TEXT_DIM, false);
                } else {
                    String caret = inputFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "";
                    ctx.drawText(textRenderer, input + caret, x + 6, ty, Palette.TEXT, false);
                }
            }
        }
    }

    // ================================================================ input

    private void applySlider(Row r, double mx) {
        double frac = Math.max(0, Math.min(1, (mx - trackX()) / TRACK_W));
        double v = r.min + frac * (r.max - r.min);
        if (r.step > 0) v = Math.round(v / r.step) * r.step;
        v = Math.max(r.min, Math.min(r.max, v));
        r.setD.accept(v);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        layout();
        for (int i = 0; i < TABS.length; i++) {
            int ty = py + 38 + i * 26;
            if (mx >= px + 6 && mx < px + SIDE && my >= ty && my < ty + 22) {
                HuroConfig.save();
                tab = i; lastTab = i; scroll = 0; inputFocus = false;
                rebuild();
                return true;
            }
        }
        inputFocus = false;
        if (mx < cx || mx > cx + cw || my < cy || my > cy + ch) return true;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int y = rowY(i);
            if (my < y || my >= y + RH) continue;
            switch (r.type) {
                case TOGGLE -> { r.setB.accept(!r.getB.getAsBoolean()); HuroConfig.save(); }
                case SLIDER -> { dragging = r; applySlider(r, mx); }
                case CYCLE -> {
                    int n = r.opts.length;
                    r.setI.accept(Math.floorMod(r.getI.getAsInt() + (btn == 1 ? -1 : 1), n));
                    HuroConfig.save();
                }
                case BUTTON -> r.action.run();
                case ITEM -> {
                    if (mx > cx + cw - 22 && r.delete != null) r.delete.run();
                    else if (r.action != null) r.action.run();
                }
                case INPUT -> inputFocus = true;
                default -> { }
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging != null) { applySlider(dragging, mx); return true; }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (dragging != null) { dragging = null; HuroConfig.save(); }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - vAmount * 20));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (inputFocus) inputFocus = false; else close();
            return true;
        }
        if (inputFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !input.isEmpty()) input = input.substring(0, input.length() - 1);
            else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) inputFocus = false;
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (inputFocus && chr >= 32 && input.length() < 28) { input += chr; return true; }
        return false;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void removed() {
        HuroConfig.save();
        HuroData.save();
        MinecraftClient.getInstance().options.write();
    }
}
