package com.myname.mymodid.score;

import java.util.HashMap;
import java.util.Map;

public class MarkData {
    // key: "x,y,z" -> 玩家名
    private static final Map<String, String> marks = new HashMap<String, String>();

    public static boolean toggle(int x, int y, int z, String playerName, String teamName) {
        String key = x + "," + y + "," + z;
        if (marks.containsKey(key)) {
            marks.remove(key);
            return true; // 移除
        } else {
            marks.put(key, playerName);
            return false; // 添加
        }
    }
}
