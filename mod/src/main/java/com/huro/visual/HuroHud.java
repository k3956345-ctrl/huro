package com.huro.visual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class HuroHud {
    private HuroHud() {}

    private static final ArrayDeque<Long> LMB = new ArrayDeque<>();
    private static final ArrayDeque<Long> RMB = new ArrayDeque<>();
    private static boolean lmbDown, rmbDown;

    private static int cps(ArrayDeque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    private static void pollClicks(MinecraftClient mc) {
        long win = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS;
        if (mc.currentScreen == null) {
            if (l && !lmbDown) LMB.addLast(System.currentTimeMillis());
            if (r && !rmbDown) RMB.addLast(System.currentTimeMillis());
        }
        lmbDown = l;
        rmbDown = r;
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        pollClicks(mc);

        HuroConfig c = HuroConfig.get();
        TextRenderer tr = mc.textRenderer;
        int W = ctx.getScaledWindowWidth();
        int H = ctx.getScaledWindowHeight();

        if (c.hitFlash && HuroVisualClient.flashTicks > 0) {
            int a = HuroVisualClient.flashTicks * 8;
            ctx.fill(0, 0, W, H, (a << 24) | 0x9B5CFF);
        }

        if (c.customCrosshair && mc.currentScreen == null && mc.options.getPerspective().isFirstPerson()) {
            drawCrosshair(ctx, c, W, H);
        }
        if (c.showMarkers) drawMarkers(ctx, mc, tr, W, H);

        float s = Math.max(0.5f, Math.min(2.0f, c.hudScale));
        int sw = (int) (W / s);
        int sh = (int) (H / s);
        ctx.getMatrices().push();
        ctx.getMatrices().scale(s, s, 1f);

        drawInfoStack(ctx, mc, tr, c);
        if (c.showKeystrokes) drawKeystrokes(ctx, mc, tr, sh);
        if (c.showArmor) drawArmor(ctx, mc, tr, sw, sh);
        if (c.showPotions) drawPotions(ctx, mc, tr, sw);

        ctx.getMatrices().pop();
    }

    // ------------------------------------------------------------- pills

    private static void pill(DrawContext ctx, TextRenderer tr, String text, int x, int y, int color) {
        int w = tr.getWidth(text);
        ctx.fill(x - 4, y - 2, x + w + 3, y + 10, Palette.HUD_BG);
        ctx.fill(x - 4, y - 2, x - 2, y + 10, Palette.ACCENT);
        ctx.drawText(tr, text, x, y, color, true);
    }

    private static void drawInfoStack(DrawContext ctx, MinecraftClient mc, TextRenderer tr, HuroConfig c) {
        int x = 8, y = 6;
        List<String> lines = new ArrayList<>();
        if (c.showFps) lines.add("FPS  " + mc.getCurrentFps());
        if (c.showCps) lines.add("CPS  " + cps(LMB) + " | " + cps(RMB));
        if (c.showPing) {
            PlayerListEntry e = mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) : null;
            lines.add("PING  " + (e != null ? e.getLatency() + " ms" : "-"));
        }
        if (c.showCoords) {
            lines.add(String.format("XYZ  %.1f / %.1f / %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
            lines.add("DIR  " + mc.player.getHorizontalFacing().asString());
        }
        if (c.showCombo && HuroVisualClient.combo > 0 && System.currentTimeMillis() - HuroVisualClient.lastHitMs < 3000) {
            lines.add("COMBO  x" + HuroVisualClient.combo);
        }
        for (String l : lines) {
            pill(ctx, tr, l, x, y, Palette.TEXT);
            y += 14;
        }
        if (c.showFriendsNearby) {
            HuroData d = HuroData.get();
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p != mc.player && d.isFriend(p.getName().getString())) {
                    pill(ctx, tr, "★ " + p.getName().getString() + "  " + (int) mc.player.distanceTo(p) + "m", x, y, 0xFF4DFF88);
                    y += 14;
                }
            }
        }
    }

    // ------------------------------------------------------------- keystrokes

    private static void key(DrawContext ctx, TextRenderer tr, String label, int x, int y, int w, int h, boolean down) {
        ctx.fill(x, y, x + w, y + h, down ? Palette.ACCENT : Palette.HUD_BG);
        ctx.fill(x, y, x + w, y + 1, Palette.ACCENT_2);
        ctx.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - 8) / 2, down ? 0xFFFFFFFF : Palette.TEXT_DIM, false);
    }

    private static void drawKeystrokes(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int sh) {
        GameOptions o = mc.options;
        int x = 8, y = sh - 90;
        key(ctx, tr, "W", x + 22, y, 20, 20, o.forwardKey.isPressed());
        key(ctx, tr, "A", x, y + 22, 20, 20, o.leftKey.isPressed());
        key(ctx, tr, "S", x + 22, y + 22, 20, 20, o.backKey.isPressed());
        key(ctx, tr, "D", x + 44, y + 22, 20, 20, o.rightKey.isPressed());
        key(ctx, tr, "LMB " + cps(LMB), x, y + 44, 31, 20, lmbDown);
        key(ctx, tr, "RMB " + cps(RMB), x + 33, y + 44, 31, 20, rmbDown);
        key(ctx, tr, "SPACE", x, y + 66, 64, 14, o.jumpKey.isPressed());
    }

    // ------------------------------------------------------------- armor

    private static void drawArmor(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int sw, int sh) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
        int y = sh / 2 - 45;
        for (EquipmentSlot slot : slots) {
            ItemStack st = mc.player.getEquippedStack(slot);
            if (!st.isEmpty()) {
                ctx.fill(sw - 24, y - 1, sw - 6, y + 17, Palette.HUD_BG);
                ctx.drawItem(st, sw - 23, y);
                if (st.isDamageable()) {
                    String d = String.valueOf(st.getMaxDamage() - st.getDamage());
                    ctx.drawText(tr, d, sw - 28 - tr.getWidth(d), y + 4, Palette.TEXT, true);
                }
            }
            y += 20;
        }
    }

    // ------------------------------------------------------------- potions

    private static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
    }

    private static void drawPotions(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int sw) {
        int y = 6;
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            if (!e.shouldShowIcon()) continue; // hides our own hidden fullbright effect
            String time = e.isInfinite() ? "∞" : String.format("%d:%02d", e.getDuration() / 1200, (e.getDuration() / 20) % 60);
            String text = e.getEffectType().value().getName().getString() + " " + roman(e.getAmplifier() + 1) + "  " + time;
            int w = tr.getWidth(text);
            int x = sw - w - 8;
            ctx.fill(x - 4, y - 2, x + w + 3, y + 10, Palette.HUD_BG);
            ctx.fill(x + w + 1, y - 2, x + w + 3, y + 10, Palette.ACCENT);
            ctx.drawText(tr, text, x, y, 0xFF000000 | e.getEffectType().value().getColor(), true);
            y += 14;
        }
    }

    // ------------------------------------------------------------- crosshair

    private static void drawCrosshair(DrawContext ctx, HuroConfig c, int W, int H) {
        int col = Palette.color(c.crosshairColor);
        int cx = W / 2, cy = H / 2;
        int t = Math.max(1, c.crosshairThickness), g = c.crosshairGap, len = c.crosshairSize;
        int o = t / 2;
        ctx.fill(cx - g - len, cy - o, cx - g, cy - o + t, col);       // left
        ctx.fill(cx + g, cy - o, cx + g + len, cy - o + t, col);       // right
        ctx.fill(cx - o, cy - g - len, cx - o + t, cy - g, col);       // up
        ctx.fill(cx - o, cy + g, cx - o + t, cy + g + len, col);       // down
        if (c.crosshairDot) ctx.fill(cx - o, cy - o, cx - o + t, cy - o + t, col);
    }

    // ------------------------------------------------------------- markers

    private static void drawMarkers(DrawContext ctx, MinecraftClient mc, TextRenderer tr, int W, int H) {
        HuroData d = HuroData.get();
        if (d.markers.isEmpty()) return;
        String dim = mc.world.getRegistryKey().getValue().toString();
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d eye = cam.getPos();
        double yaw = Math.toRadians(cam.getYaw()), pitch = Math.toRadians(cam.getPitch());
        Vec3d fwd = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3d right = new Vec3d(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3d up = right.crossProduct(fwd);
        double tanHalf = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        double aspect = (double) W / H;

        for (HuroData.Marker m : d.markers) {
            if (!dim.equals(m.dim)) continue;
            Vec3d v = new Vec3d(m.x - eye.x, m.y + 1.0 - eye.y, m.z - eye.z);
            double f = v.dotProduct(fwd);
            if (f < 0.5) continue;
            double xn = v.dotProduct(right) / f / (tanHalf * aspect);
            double yn = v.dotProduct(up) / f / tanHalf;
            if (Math.abs(xn) > 1.0 || Math.abs(yn) > 1.0) continue;
            int sx = (int) (W / 2.0 + xn * W / 2.0);
            int sy = (int) (H / 2.0 - yn * H / 2.0);
            int col = Palette.color(m.color);
            String label = m.name + "  " + (int) v.length() + "m";
            int w = tr.getWidth(label);
            ctx.fill(sx - w / 2 - 3, sy - 2, sx + w / 2 + 3, sy + 10, Palette.HUD_BG);
            ctx.fill(sx - 3, sy + 11, sx + 3, sy + 14, col);
            ctx.drawText(tr, label, sx - w / 2, sy, col, true);
        }
    }
}
