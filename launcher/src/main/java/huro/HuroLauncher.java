package huro;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** HURO VISUAL launcher: installs Fabric 1.21.4, Fabric API and the HURO VISUAL mod, then opens Minecraft. */
public class HuroLauncher extends JFrame {
    static final String MC = "1.21.4";
    static final String LOADER = "0.16.10";
    static final String INSTALLER_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.1/fabric-installer-1.0.1.jar";
    static final String MODRINTH = "https://api.modrinth.com/v2/project/fabric-api/version?game_versions=%5B%22" + MC + "%22%5D&loaders=%5B%22fabric%22%5D";

    static final Color BG1 = new Color(0x0E0818), BG2 = new Color(0x2A1050), ACCENT = new Color(0x9B5CFF),
        ACCENT2 = new Color(0x6D28D9), TEXT = new Color(0xEDE7FF), DIM = new Color(0x9C8FC4);

    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    private volatile String status = "Готов к запуску";
    private volatile double progress = 0;
    private volatile boolean busy = false;
    private final JPanel canvas;
    private Rectangle playBtn = new Rectangle(), updBtn = new Rectangle();
    private boolean hoverPlay, hoverUpd;

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new HuroLauncher().setVisible(true));
    }

    HuroLauncher() {
        super("HURO VISUAL Launcher");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        canvas = new JPanel() {
            @Override protected void paintComponent(Graphics g0) { paintUi((Graphics2D) g0); }
        };
        canvas.setPreferredSize(new Dimension(760, 460));
        add(canvas);
        pack();
        setLocationRelativeTo(null);

        canvas.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override public void mouseMoved(java.awt.event.MouseEvent e) {
                hoverPlay = playBtn.contains(e.getPoint());
                hoverUpd = updBtn.contains(e.getPoint());
                canvas.setCursor(Cursor.getPredefinedCursor(hoverPlay || hoverUpd ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                canvas.repaint();
            }
        });
        canvas.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (busy) return;
                if (playBtn.contains(e.getPoint())) run(true);
                else if (updBtn.contains(e.getPoint())) run(false);
            }
        });
    }

    // ------------------------------------------------------------------ UI

    private void paintUi(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = canvas.getWidth(), h = canvas.getHeight();
        g.setPaint(new GradientPaint(0, 0, BG1, w, h, BG2));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(155, 92, 255, 28));
        g.fillOval(-120, h - 220, 420, 420);
        g.fillOval(w - 260, -160, 420, 420);

        // logo
        int lx = w / 2 - 55, ly = 50;
        g.setPaint(new GradientPaint(lx, ly, ACCENT, lx + 110, ly + 110, ACCENT2));
        g.fill(new RoundRectangle2D.Float(lx, ly, 110, 110, 30, 30));
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 84));
        center(g, "H", w / 2, ly + 88);

        g.setFont(new Font("Segoe UI", Font.BOLD, 30));
        g.setColor(TEXT);
        center(g, "HURO VISUAL", w / 2, 205);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        g.setColor(DIM);
        center(g, "Minecraft " + MC + "  ·  Fabric " + LOADER, w / 2, 230);

        // play button
        playBtn = new Rectangle(w / 2 - 130, 262, 260, 62);
        g.setPaint(busy ? new GradientPaint(0, 0, new Color(0x4B3A7A), 0, 1, new Color(0x4B3A7A))
            : new GradientPaint(0, playBtn.y, hoverPlay ? new Color(0xB57CFF) : ACCENT, 0, playBtn.y + 62, ACCENT2));
        g.fill(new RoundRectangle2D.Float(playBtn.x, playBtn.y, playBtn.width, playBtn.height, 24, 24));
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 26));
        center(g, busy ? "ПОДОЖДИТЕ..." : "ИГРАТЬ", w / 2, playBtn.y + 41);

        // update button
        updBtn = new Rectangle(w / 2 - 90, 336, 180, 30);
        g.setColor(hoverUpd && !busy ? new Color(0x2A1D4A) : new Color(0x1E1535));
        g.fill(new RoundRectangle2D.Float(updBtn.x, updBtn.y, updBtn.width, updBtn.height, 14, 14));
        g.setColor(DIM);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        center(g, "Установить / обновить мод", w / 2, updBtn.y + 20);

        // progress + status
        int bw = 420, bx = w / 2 - bw / 2, by = 392;
        g.setColor(new Color(0x1E1535));
        g.fill(new RoundRectangle2D.Float(bx, by, bw, 8, 8, 8));
        g.setColor(ACCENT);
        g.fill(new RoundRectangle2D.Float(bx, by, (float) (bw * progress), 8, 8, 8));
        g.setColor(TEXT);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        center(g, status, w / 2, by + 30);
    }

    private static void center(Graphics2D g, String s, int cx, int baseline) {
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, baseline);
    }

    private void set(String s, double p) {
        status = s; progress = p;
        SwingUtilities.invokeLater(canvas::repaint);
    }

    // ------------------------------------------------------------------ flow

    private void run(boolean launch) {
        busy = true;
        canvas.repaint();
        new Thread(() -> {
            try {
                Path mc = minecraftDir();
                Files.createDirectories(mc);
                installFabric(mc);
                installMods(mc);
                set("Всё установлено ✔", 1.0);
                if (launch) {
                    openMojangLauncher();
                    set("Выберите профиль fabric-loader-" + MC + " и нажмите Играть. В игре: Right Shift", 1.0);
                }
            } catch (Exception e) {
                set("Ошибка: " + e.getMessage(), 0);
                e.printStackTrace();
            } finally {
                busy = false;
                SwingUtilities.invokeLater(canvas::repaint);
            }
        }, "huro-install").start();
    }

    static Path minecraftDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");
        if (os.contains("win")) {
            String app = System.getenv("APPDATA");
            return Paths.get(app != null ? app : home + "\\AppData\\Roaming", ".minecraft");
        }
        if (os.contains("mac")) return Paths.get(home, "Library", "Application Support", "minecraft");
        return Paths.get(home, ".minecraft");
    }

    // ---- Fabric loader

    private void installFabric(Path mc) throws Exception {
        Path ver = mc.resolve("versions").resolve("fabric-loader-" + LOADER + "-" + MC);
        if (Files.exists(ver.resolve("fabric-loader-" + LOADER + "-" + MC + ".json"))) {
            set("Fabric уже установлен", 0.35);
            return;
        }
        if (!Files.exists(mc.resolve("launcher_profiles.json"))) {
            throw new IOException("Сначала один раз откройте официальный Minecraft Launcher (создаст папку игры), затем нажмите снова");
        }
        Path tmp = Files.createTempFile("fabric-installer", ".jar");
        set("Скачиваю установщик Fabric...", 0.05);
        download(INSTALLER_URL, tmp);
        set("Устанавливаю Fabric " + LOADER + " для " + MC + "...", 0.2);
        String javaBin = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        Process p = new ProcessBuilder(javaBin, "-jar", tmp.toString(), "client",
            "-dir", mc.toString(), "-mcversion", MC, "-loader", LOADER)
            .redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (p.waitFor() != 0) throw new IOException("Установщик Fabric: " + out.trim());
        Files.deleteIfExists(tmp);
        set("Fabric установлен", 0.35);
    }

    // ---- mods

    private void installMods(Path mc) throws Exception {
        Path mods = mc.resolve("mods");
        Files.createDirectories(mods);

        set("Проверяю Fabric API...", 0.45);
        try {
            installFabricApi(mods);
        } catch (Exception e) {
            if (!hasFile(mods, "fabric-api")) throw new IOException("не удалось скачать Fabric API (" + e.getMessage() + ")");
        }

        set("Устанавливаю HURO VISUAL...", 0.8);
        try (InputStream in = HuroLauncher.class.getResourceAsStream("/huro-visual.jar")) {
            if (in == null) throw new IOException("в лаунчере нет файла мода (huro-visual.jar)");
            byte[] jar = in.readAllBytes();
            Path target = mods.resolve("huro-visual.jar");
            boolean same = Files.exists(target) && sha(Files.readAllBytes(target)).equals(sha(jar));
            try (Stream<Path> s = Files.list(mods)) {
                for (Path f : (Iterable<Path>) s::iterator) {
                    String n = f.getFileName().toString().toLowerCase();
                    if (n.startsWith("huro-visual") && !n.equals("huro-visual.jar")) Files.deleteIfExists(f);
                }
            }
            if (!same) Files.write(target, jar);
            set(same ? "Мод актуален" : "Мод установлен / обновлён", 0.95);
        }
    }

    private boolean hasFile(Path dir, String prefix) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.anyMatch(f -> f.getFileName().toString().toLowerCase().startsWith(prefix));
        }
    }

    private void installFabricApi(Path mods) throws Exception {
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(MODRINTH))
            .header("User-Agent", "HuroLauncher/1.0").build(), HttpResponse.BodyHandlers.ofString());
        Matcher m = Pattern.compile("\"url\"\\s*:\\s*\"(https://cdn\\.modrinth\\.com/[^\"]+?/(fabric-api-[^\"/]+\\.jar))\"").matcher(r.body());
        if (!m.find()) throw new IOException("Modrinth не вернул Fabric API");
        String url = m.group(1), name = java.net.URLDecoder.decode(m.group(2), "UTF-8");
        Path target = mods.resolve(name);
        if (Files.exists(target)) return;
        List<Path> old = new ArrayList<>();
        try (Stream<Path> s = Files.list(mods)) {
            s.filter(f -> f.getFileName().toString().toLowerCase().startsWith("fabric-api")).forEach(old::add);
        }
        set("Скачиваю Fabric API...", 0.55);
        download(url, target);
        for (Path o : old) Files.deleteIfExists(o);
    }

    // ---- helpers

    private void download(String url, Path target) throws Exception {
        HttpResponse<InputStream> r = http.send(HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", "HuroLauncher/1.0").build(), HttpResponse.BodyHandlers.ofInputStream());
        if (r.statusCode() != 200) throw new IOException("HTTP " + r.statusCode() + " для " + url);
        try (InputStream in = r.body()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha(byte[] data) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private void openMojangLauncher() {
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String[]> attempts = new ArrayList<>();
        if (os.contains("win")) {
            for (String env : new String[]{"ProgramFiles(x86)", "ProgramFiles"}) {
                String base = System.getenv(env);
                if (base != null) {
                    Path exe = Paths.get(base, "Minecraft Launcher", "MinecraftLauncher.exe");
                    if (Files.exists(exe)) attempts.add(new String[]{exe.toString()});
                }
            }
            attempts.add(new String[]{"explorer.exe", "shell:AppsFolder\\Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft"});
        } else if (os.contains("mac")) {
            attempts.add(new String[]{"open", "-a", "Minecraft"});
        } else {
            attempts.add(new String[]{"minecraft-launcher"});
        }
        for (String[] cmd : attempts) {
            try { new ProcessBuilder(cmd).start(); return; } catch (IOException ignored) {}
        }
        set("Откройте Minecraft Launcher вручную: всё уже установлено", 1.0);
    }
}
