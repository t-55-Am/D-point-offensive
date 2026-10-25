package com.myname.mymodid.client.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Properties;

public class QuickCommConfig {
    public static final int CATEGORIES = 6;
    public static final int CMD_PER_CAT = 8;
    private static final File CONFIG_FILE = new File("config/mymodid_quickcomm.cfg");
    public static String[] catNames = new String[CATEGORIES];
    public static String[][] commands = new String[CATEGORIES][CMD_PER_CAT];
    private static boolean loaded = false;
    private static long lastLoadTime = 0;
    private static final long LOAD_INTERVAL = 30000; // 30秒内不重复加载，反复打开GUI时不读盘

    static {
        String[] defaults = {"进攻指令", "防守指令", "请求支援", "敌情通报", "队伍交流", "其他"};
        for (int i = 0; i < CATEGORIES; i++) {
            catNames[i] = defaults[i];
            for (int j = 0; j < CMD_PER_CAT; j++) commands[i][j] = "";
        }
        commands[0][0] = "全体进攻A点";
        commands[0][1] = "载具支援";
        commands[1][0] = "固守据点";
        commands[2][0] = "请求医疗";
    }

    public static void load() {
        // 缓存机制：5秒内不重复从磁盘加载，避免反复打开GUI时频繁IO
        long now = System.currentTimeMillis();
        if (loaded && (now - lastLoadTime) < LOAD_INTERVAL) return;
        loaded = true;
        lastLoadTime = now;

        if (!CONFIG_FILE.exists()) { save(); return; }
        try {
            Properties prop = new Properties();
            FileInputStream fis = new FileInputStream(CONFIG_FILE);
            prop.load(new InputStreamReader(fis, "UTF-8"));
            fis.close();
            for (int i = 0; i < CATEGORIES; i++) {
                String n = prop.getProperty("cat." + i + ".name");
                if (n != null) catNames[i] = n;
                for (int j = 0; j < CMD_PER_CAT; j++) {
                    String c = prop.getProperty("cat." + i + ".cmd." + j);
                    if (c != null) commands[i][j] = c;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            Properties prop = new Properties();
            for (int i = 0; i < CATEGORIES; i++) {
                prop.setProperty("cat." + i + ".name", catNames[i]);
                for (int j = 0; j < CMD_PER_CAT; j++) {
                    prop.setProperty("cat." + i + ".cmd." + j, commands[i][j]);
                }
            }
            FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
            prop.store(new OutputStreamWriter(fos, "UTF-8"), "MyMod Quick Comm Config");
            fos.close();
            lastLoadTime = System.currentTimeMillis();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
