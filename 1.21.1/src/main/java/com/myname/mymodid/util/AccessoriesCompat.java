package com.myname.mymodid.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AccessoriesCompat {

    public static List<ItemStack> getAccessories(Player p) {
        List<ItemStack> list = new ArrayList<>();
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method m = api.getMethod("getCuriosInventory", net.minecraft.world.entity.Entity.class);
            Optional<?> opt = (Optional<?>) m.invoke(null, p);
            if (opt == null || !opt.isPresent()) return list;
            Object handler = opt.get();
            Object stacks = handler.getClass().getMethod("getEquippedCurios").invoke(handler);
            int n = (Integer) stacks.getClass().getMethod("getSlots").invoke(stacks);
            for (int i = 0; i < n; i++) {
                ItemStack s = (ItemStack) stacks.getClass().getMethod("getStackInSlot", int.class).invoke(stacks, i);
                list.add(s == null || s.isEmpty() ? null : s.copy());
            }
        } catch (Exception e) {}
        return list;
    }

    public static void setAccessories(Player p, List<ItemStack> acc) {
        if (acc == null || acc.isEmpty()) return;
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method m = api.getMethod("getCuriosInventory", net.minecraft.world.entity.Entity.class);
            Optional<?> opt = (Optional<?>) m.invoke(null, p);
            if (opt == null || !opt.isPresent()) return;
            Object handler = opt.get();
            Object stacks = handler.getClass().getMethod("getEquippedCurios").invoke(handler);
            int n = (Integer) stacks.getClass().getMethod("getSlots").invoke(stacks);
            Method setStack = stacks.getClass().getMethod("setStackInSlot", int.class, ItemStack.class);
            for (int i = 0; i < Math.min(n, acc.size()); i++) {
                ItemStack s = acc.get(i);
                setStack.invoke(stacks, i, s == null || s.isEmpty() ? ItemStack.EMPTY : s.copy());
            }
        } catch (Exception e) {}
    }
}
