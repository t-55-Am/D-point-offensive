package com.myname.mymodid.score;

import java.util.HashMap;
import java.util.Map;

public class MarkData {
    private static final Map<String, String> marks = new HashMap<>();

    public static boolean toggle(int x, int y, int z, String playerName) {
        String key = x + "," + y + "," + z;
        if (marks.containsKey(key)) {
            marks.remove(key);
            return true;
        } else {
            marks.put(key, playerName);
            return false;
        }
    }
}
