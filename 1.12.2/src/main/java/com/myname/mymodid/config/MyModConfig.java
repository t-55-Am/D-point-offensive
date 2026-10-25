package com.myname.mymodid.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public class MyModConfig {
    public static Configuration config;

    public static boolean enableCapture = true;
    public static boolean enableScore = true;
    public static boolean enableTeam = true;

    public static boolean enableRadar = true;
    public static boolean enableFloatingName = true;
    public static boolean enableRangeCircle = true;
    public static boolean enableScoreBar = true;
    public static boolean enableCaptureRing = true;

    /** v1.2.2 同步：HUD 位置大小（0 偏移=水平居中/顶部，0 宽度=自动半屏） */
    public static int scoreBarX = 0;
    public static int scoreBarY = 24;
    public static int scoreBarWidth = 220;
    public static int radarY = 4;
    public static int radarWidth = 0;


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
            scoreBarX = config.get(d, "scoreBarX", 0, "score bar X offset (0=center)").getInt();
            scoreBarY = config.get(d, "scoreBarY", 24, "score bar Y").getInt();
            scoreBarWidth = config.get(d, "scoreBarWidth", 220, "score bar width").getInt();
            radarY = config.get(d, "radarY", 4, "radar bar Y").getInt();
            radarWidth = config.get(d, "radarWidth", 0, "radar width (0=auto half screen)").getInt();
        } catch (Exception e) {
        } finally {
            if (config.hasChanged()) config.save();
        }
    }

    /** v1.2.2 同步：HUD 配置页保存后立即落盘生效 */
    public static void save() {
        try {
            if (config == null) return;
            config.load();
            String d = "display";
            config.get(d, "enableRadar", true).set(enableRadar);
            config.get(d, "enableFloatingName", true).set(enableFloatingName);
            config.get(d, "enableRangeCircle", true).set(enableRangeCircle);
            config.get(d, "enableScoreBar", true).set(enableScoreBar);
            config.get(d, "enableCaptureRing", true).set(enableCaptureRing);
            config.get(d, "scoreBarX", 0).set(scoreBarX);
            config.get(d, "scoreBarY", 24).set(scoreBarY);
            config.get(d, "scoreBarWidth", 220).set(scoreBarWidth);
            config.get(d, "radarY", 4).set(radarY);
            config.get(d, "radarWidth", 0).set(radarWidth);
            config.save();
        } catch (Exception e) {
        }
    }
}
