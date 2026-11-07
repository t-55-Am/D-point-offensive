package com.myname.mymodid.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

public class MyModConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_CAPTURE;
    public static final ModConfigSpec.BooleanValue ENABLE_SCORE;
    public static final ModConfigSpec.BooleanValue ENABLE_TEAM;

    public static final ModConfigSpec.BooleanValue ENABLE_RADAR;
    public static final ModConfigSpec.BooleanValue ENABLE_FLOATING_NAME;
    public static final ModConfigSpec.BooleanValue ENABLE_RANGE_CIRCLE;
    public static final ModConfigSpec.BooleanValue ENABLE_SCORE_BAR;
    public static final ModConfigSpec.BooleanValue ENABLE_CAPTURE_RING;

    public static int scoreBarX = 0;
    public static int scoreBarY = 24;
    public static int scoreBarWidth = 220;
    public static int radarY = 4;
    public static int radarWidth = 0;
    public static boolean hudScoreBar = true;
    public static boolean hudRadar = true;
    public static boolean hudRing = true;
    public static boolean hudRange = true;
    public static boolean hudName = true;
    public static double hudNameScale = 1.0;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ENABLE_CAPTURE = b.comment("capture logic").define("enableCapture", true);
        ENABLE_SCORE = b.comment("score drain").define("enableScore", true);
        ENABLE_TEAM = b.comment("team battle portal").define("enableTeam", true);
        b.push("display");
        ENABLE_RADAR = b.comment("radar bar").define("enableRadar", true);
        ENABLE_FLOATING_NAME = b.comment("floating name + ring").define("enableFloatingName", true);
        ENABLE_RANGE_CIRCLE = b.comment("ground range circle").define("enableRangeCircle", true);
        ENABLE_SCORE_BAR = b.comment("top score bar").define("enableScoreBar", true);
        ENABLE_CAPTURE_RING = b.comment("capture ring on status bar").define("enableCaptureRing", true);
        b.pop();
        SPEC = b.build();
    }

    private static Path hudPath() {
        return FMLPaths.CONFIGDIR.get().resolve("objective_d-hud.json");
    }

    public static void loadHud() {
        try {
            Path p = hudPath();
            if (!Files.exists(p)) return;
            Map<String, Object> m = parseJson(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
            scoreBarX = intOf(m, "scoreBarX", scoreBarX);
            scoreBarY = intOf(m, "scoreBarY", scoreBarY);
            scoreBarWidth = Math.max(40, intOf(m, "scoreBarWidth", scoreBarWidth));
            radarY = intOf(m, "radarY", radarY);
            radarWidth = Math.max(0, intOf(m, "radarWidth", radarWidth));
            hudScoreBar = boolOf(m, "hudScoreBar", ENABLE_SCORE_BAR.get());
            hudRadar = boolOf(m, "hudRadar", ENABLE_RADAR.get());
            hudRing = boolOf(m, "hudRing", ENABLE_CAPTURE_RING.get());
            hudRange = boolOf(m, "hudRange", ENABLE_RANGE_CIRCLE.get());
            hudName = boolOf(m, "hudName", ENABLE_FLOATING_NAME.get());
            hudNameScale = Math.max(0.4, Math.min(2.0, doubleOf(m, "hudNameScale", 1.0)));
        } catch (Exception ignored) {}
    }

    public static void saveHud() {
        try {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"scoreBarX\":").append(scoreBarX).append(',');
            sb.append("\"scoreBarY\":").append(scoreBarY).append(',');
            sb.append("\"scoreBarWidth\":").append(scoreBarWidth).append(',');
            sb.append("\"radarY\":").append(radarY).append(',');
            sb.append("\"radarWidth\":").append(radarWidth).append(',');
            sb.append("\"hudScoreBar\":").append(hudScoreBar).append(',');
            sb.append("\"hudRadar\":").append(hudRadar).append(',');
            sb.append("\"hudRing\":").append(hudRing).append(',');
            sb.append("\"hudRange\":").append(hudRange).append(',');
            sb.append("\"hudName\":").append(hudName).append(',');
            sb.append("\"hudNameScale\":").append(hudNameScale);
            sb.append('}');
            Files.write(hudPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {}
    }

    private static Map<String, Object> parseJson(String s) {
        Map<String, Object> m = new HashMap<>();
        String body = s.replaceAll("^\\s*\\{?\\s*|\\s*\\}?\\s*$", "");
        for (String part : body.split(",")) {
            String[] kv = part.split(":", 2);
            if (kv.length != 2) continue;
            String k = kv[0].trim().replace("\"", "").trim();
            String v = kv[1].trim();
            m.put(k, v);
        }
        return m;
    }

    private static int intOf(Map<String, Object> m, String k, int def) {
        Object v = m.get(k);
        if (v == null) return def;
        try { return (int) Double.parseDouble(v.toString()); } catch (Exception e) { return def; }
    }

    private static boolean boolOf(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        if (v == null) return def;
        return Boolean.parseBoolean(v.toString());
    }

    private static double doubleOf(Map<String, Object> m, String k, double def) {
        Object v = m.get(k);
        if (v == null) return def;
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return def; }
    }
}
