package com.myname.mymodid.client.gui;

import com.myname.mymodid.network.CPayloads.ChatPayload;
import com.myname.mymodid.network.CPayloads.VoiceRequestPayload;
import com.myname.mymodid.network.Network;
import com.myname.mymodid.client.ClientEventHandler;
import com.myname.mymodid.client.SoundPlayer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ChatGui extends Screen {
    private EditBox input;
    private boolean teamOnly = false;
    private int scrollOffset = 0;
    private static final int VISIBLE_LINES = 12;

    public ChatGui() { super(Component.literal("Chat")); }

    @Override
    protected void init() {
        input = new EditBox(font, 60, height - 26, width - 70, 20, Component.literal("input"));
        input.setMaxLength(120);
        input.setFocused(true);
        setInitialFocus(input);
        addRenderableWidget(input);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        java.util.List<String> history = ClientEventHandler.chatHistory;
        int total = history.size();
        int lineHeight = 12;
        int historyBottom = height - 32;
        int end = total - scrollOffset;
        int start = Math.max(0, end - VISIBLE_LINES);
        int visibleCount = end - start;

        if (visibleCount > 0) {
            int historyTop = historyBottom - visibleCount * lineHeight - 4;
            g.fill(2, historyTop, width - 2, historyBottom, 0x66000000);
            int yy = historyBottom - lineHeight;
            for (int i = end - 1; i >= start; i--) {
                if (i < 0 || i >= total) break;
                g.drawString(font, history.get(i), 6, yy, 0xFFFFFF, false);
                yy -= lineHeight;
            }
        } else {
            String emptyText = Component.translatable("dp.chat.empty").getString();
            g.drawString(font, emptyText, width / 2 - font.width(emptyText) / 2, historyBottom - 20, 0x888888, false);
        }

        if (scrollOffset > 0) {
            String moreText = Component.translatable("dp.chat.more").getString();
            g.drawString(font, moreText, width / 2 - font.width(moreText) / 2, height - 56, 0xAAAAAA, false);
        }

        g.fill(2, height - 28, width - 2, height - 4, 0xCC000000);
        String ch = Component.translatable(teamOnly ? "dp.chat.team" : "dp.chat.all").getString();
        g.drawString(font, "[" + ch + "]", 6, height - 20, teamOnly ? 0x55FF55 : 0xFFFF55, false);
        g.drawString(font, Component.translatable("dp.chat.hint"), 6, height - 42, 0x888888, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, ClientEventHandler.chatHistory.size() - VISIBLE_LINES);
        if (verticalAmount > 0) scrollOffset = Math.min(scrollOffset + 2, maxScroll);
        else scrollOffset = Math.max(0, scrollOffset - 2);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) { teamOnly = !teamOnly; return true; }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            String text = input.getValue().trim();
            if (!text.isEmpty()) {
                Minecraft mc = Minecraft.getInstance();
                if (text.startsWith("/")) {
                    if (mc.player != null) mc.player.connection.sendChat(text);
                } else {
                    Network.sendToServer(new ChatPayload(text, teamOnly));
                    SoundPlayer.play(text);
                    Network.sendToServer(new VoiceRequestPayload(text));
                }
            }
            scrollOffset = 0;
            onClose();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
