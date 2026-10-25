package com.myname.mymodid.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class DelayedTask {
    private static final List<Runnable> pending = new ArrayList<Runnable>();

    public static void run(Runnable task) {
        synchronized (pending) {
            pending.add(task);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        List<Runnable> toRun;
        synchronized (pending) {
            if (pending.isEmpty()) return;
            toRun = new ArrayList<Runnable>(pending);
            pending.clear();
        }
        for (Runnable r : toRun) {
            try {
                r.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
