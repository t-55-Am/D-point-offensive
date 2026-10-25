package com.myname.mymodid.score;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 玩家战斗统计（服务端内存）：名称 -> {击杀, 助攻, 占点, 死亡}
public class PlayerStats {
    public static final Map<String, int[]> stats = new HashMap<String, int[]>();

    public static int[] get(String name) {
        int[] a = stats.get(name);
        if (a == null) {
            a = new int[]{0, 0, 0, 0};
            stats.put(name, a);
        }
        return a;
    }

    public static void addKill(String name) {
        get(name)[0]++;
    }

    public static void addAssist(String name) {
        get(name)[1]++;
    }

    public static void addCap(String name) {
        get(name)[2]++;
    }
    public static void addDeath(String name) {
        get(name)[3]++;
    }

    // 击杀榜前 3：返回 {名称, 击杀数}
    public static List<String[]> top3() {
        List<Map.Entry<String, int[]>> list = new ArrayList<Map.Entry<String, int[]>>(stats.entrySet());
        list.sort(new java.util.Comparator<Map.Entry<String, int[]>>() {
            public int compare(Map.Entry<String, int[]> a, Map.Entry<String, int[]> b) {
                return Integer.compare(b.getValue()[0], a.getValue()[0]);
            }
        });
        List<String[]> res = new ArrayList<String[]>();
        for (int i = 0; i < Math.min(3, list.size()); i++) {
            res.add(new String[]{list.get(i).getKey(), String.valueOf(list.get(i).getValue()[0])});
        }
        return res;
    }

    public static void clear() {
        stats.clear();
    }
}
