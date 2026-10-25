package com.myname.mymodid.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class MyModConfig {
    public static Configuration config;

    // general
    public static boolean enableCapture = true;
    public static boolean enableScore = true;
    public static boolean enableTeam = true;

    // display
    public static boolean enableRadar = true;
    public static boolean enableFloatingName = true;
    public static boolean enableRangeCircle = true;
    public static boolean enableScoreBar = true;
    public static boolean enableCaptureRing = true;

    // HUD 位置/大小（v1.1.15，可 GUI 配置）
    public static int scoreBarY = 24;      // 分数条顶部 Y
    public static int scoreBarX = 0;       // 0=水平居中，>0 右移，<0 左移
    public static int scoreBarWidth = 220;
    public static int radarY = 4;          // 罗盘顶部 Y
    public static int radarWidth = 0;      // 0=自动(屏幕宽/2)


    public static void init(File file) {
        config = new Configuration(file);
        load();
    }

    public static void load() {
        try {
            config.load();
            String g = Configuration.CATEGORY_GENERAL;
            enableCapture = config.get(g, "enableCapture", true, "capture logic").getBoolean(true);
            enableScore = config.get(g, "enableScore", true, "score drain").getBoolean(true);
            enableTeam = config.get(g, "enableTeam", true, "team battle portal").getBoolean(true);
            String d = "display";
            enableRadar = config.get(d, "enableRadar", true, "radar bar").getBoolean(true);
            enableFloatingName = config.get(d, "enableFloatingName", true, "floating name + ring").getBoolean(true);
            enableRangeCircle = config.get(d, "enableRangeCircle", true, "ground range circle").getBoolean(true);
            enableScoreBar = config.get(d, "enableScoreBar", true, "top score bar").getBoolean(true);
            enableCaptureRing = config.get(d, "enableCaptureRing", true, "capture ring on status bar").getBoolean(true);
            scoreBarY = config.get(d, "scoreBarY", 24, "score bar top Y").getInt(24);
            scoreBarX = config.get(d, "scoreBarX", 0, "score bar X offset (0=center)").getInt(0);
            scoreBarWidth = config.get(d, "scoreBarWidth", 220, "score bar width").getInt(220);
            radarY = config.get(d, "radarY", 4, "radar top Y").getInt(4);
            radarWidth = config.get(d, "radarWidth", 0, "radar width (0=auto half screen)").getInt(0);
        } catch (Exception e) {
            // ignore
        } finally {
            if (config.hasChanged()) config.save();
        }
    }

    public static void save() {
        try {
            config.load();
            config.get(Configuration.CATEGORY_GENERAL, "enableCapture", true).set(enableCapture);
            config.get(Configuration.CATEGORY_GENERAL, "enableScore", true).set(enableScore);
            config.get(Configuration.CATEGORY_GENERAL, "enableTeam", true).set(enableTeam);
            String d = "display";
            config.get(d, "enableRadar", true).set(enableRadar);
            config.get(d, "enableFloatingName", true).set(enableFloatingName);
            config.get(d, "enableRangeCircle", true).set(enableRangeCircle);
            config.get(d, "enableScoreBar", true).set(enableScoreBar);
            config.get(d, "enableCaptureRing", true).set(enableCaptureRing);
            config.get(d, "scoreBarY", 24).set(scoreBarY);
            config.get(d, "scoreBarX", 0).set(scoreBarX);
            config.get(d, "scoreBarWidth", 220).set(scoreBarWidth);
            config.get(d, "radarY", 4).set(radarY);
            config.get(d, "radarWidth", 0).set(radarWidth);
            config.save();
        } catch (Exception e) {
            // ignore
        }
    }
}
