package com.myname.mymodid.util;

import java.util.ArrayList;
import java.util.List;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class DelayedTask {
    private static final List<Runnable> pending = new ArrayList<>();

    public static void run(Runnable task) {
        synchronized (pending) {
            pending.add(task);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        List<Runnable> toRun;
        synchronized (pending) {
            if (pending.isEmpty()) return;
            toRun = new ArrayList<>(pending);
            pending.clear();
        }
        for (Runnable r : toRun) {
            try { r.run(); } catch (Throwable t) { t.printStackTrace(); }
        }
    }
}
