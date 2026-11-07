package com.myname.mymodid.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class DpScreen extends Screen {

    protected static class DpBtn {
        public int x, y, w, h;
        public boolean visible = true;
        public boolean enabled = true;
        public final Supplier<String> label;
        public final Runnable onClick;
        public DpBtn(int x, int y, int w, int h, Supplier<String> label, Runnable onClick) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.label = label; this.onClick = onClick;
        }
    }

    private final List<DpBtn> buttons = new ArrayList<>();

    protected DpScreen(Component title) { super(title); }

    protected DpBtn addButton(int x, int y, int w, int h, Supplier<String> label, Runnable onClick) {
        DpBtn b = new DpBtn(x, y, w, h, label, onClick);
        buttons.add(b);
        return b;
    }

    protected void clearButtons() { buttons.clear(); }

    protected abstract void drawContents(GuiGraphics g, int mouseX, int mouseY, float partialTick);

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        drawContents(g, mouseX, mouseY, partialTick);
        for (DpBtn b : buttons) if (b.visible) drawOwnButton(g, b, mouseX, mouseY);
    }

    private void drawOwnButton(GuiGraphics g, DpBtn b, int mx, int my) {
        boolean hover = b.enabled && mx >= b.x && mx < b.x + b.w && my >= b.y && my < b.y + b.h;
        int bg = !b.enabled ? 0xFF4A4A4A : hover ? 0xFF8C8C8C : 0xFF6A6A6A;
        g.fill(b.x, b.y, b.x + b.w, b.y + b.h, bg);
        g.fill(b.x, b.y, b.x + b.w, b.y + 1, !b.enabled ? 0xFF666666 : 0xFFBFBFBF);
        g.fill(b.x, b.y + b.h - 1, b.x + b.w, b.y + b.h, 0xFF373737);
        String s = b.label.get();
        int txtColor = !b.enabled ? 0xFF888888 : 0xFFFFFFFF;
        g.drawString(font, s, b.x + (b.w - font.width(s)) / 2, b.y + (b.h - font.lineHeight) / 2 + 1, txtColor, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (DpBtn b : buttons) {
                if (b.visible && b.enabled && mx >= b.x && mx < b.x + b.w && my >= b.y && my < b.y + b.h) {
                    b.onClick.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
