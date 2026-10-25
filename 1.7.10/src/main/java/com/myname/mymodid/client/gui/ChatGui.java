package com.myname.mymodid.client.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import com.myname.mymodid.MyMod;
import com.myname.mymodid.network.PacketChatMessage;
import com.myname.mymodid.client.ClientEventHandler;
import java.util.List;

public class ChatGui extends GuiScreen {
    private GuiTextField input;
    private boolean teamOnly = false;
    private int scrollOffset = 0;
    private static final int VISIBLE_LINES = 12;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        input = new GuiTextField(fontRendererObj, 60, height - 26, width - 70, 20);
        input.setFocused(true);
        input.setMaxStringLength(120);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        List<String> history = ClientEventHandler.chatHistory;
        int total = history.size();
        int lineHeight = 12;
        int historyBottom = height - 32;
        int end = total - scrollOffset;
        int start = Math.max(0, end - VISIBLE_LINES);
        int visibleCount = end - start;

        if (visibleCount > 0) {
            int historyTop = historyBottom - visibleCount * lineHeight - 4;
            drawRect(2, historyTop, width - 2, historyBottom, 0x66000000);
            int yy = historyBottom - lineHeight;
            for (int i = end - 1; i >= start; i--) {
                if (i < 0 || i >= total) break;
                fontRendererObj.drawStringWithShadow(history.get(i), 6, yy, 0xFFFFFF);
                yy -= lineHeight;
            }
        } else {
            drawCenteredString(fontRendererObj, StatCollector.translateToLocal("dp.chat.empty"), width / 2, historyBottom - 20, 0x888888);
        }

        if (scrollOffset > 0)
            drawCenteredString(fontRendererObj, StatCollector.translateToLocal("dp.chat.more"), width / 2, height - 56, 0xAAAAAA);

        drawRect(2, height - 28, width - 2, height - 4, 0xCC000000);
        String ch = teamOnly ? StatCollector.translateToLocal("dp.chat.team") : StatCollector.translateToLocal("dp.chat.all");
        drawString(fontRendererObj, "[" + ch + "]", 6, height - 20, teamOnly ? 0x55FF55 : 0xFFFF55);
        input.drawTextBox();
        drawString(fontRendererObj, StatCollector.translateToLocal("dp.chat.hint"), 6, height - 42, 0x888888);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int maxScroll = Math.max(0, ClientEventHandler.chatHistory.size() - VISIBLE_LINES);
            if (wheel > 0) scrollOffset = Math.min(scrollOffset + 2, maxScroll);
            else scrollOffset = Math.max(0, scrollOffset - 2);
        }
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        if (keyCode == Keyboard.KEY_TAB) { teamOnly = !teamOnly; return; }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            String text = input.getText().trim();
            if (!text.isEmpty()) {
                if (text.startsWith("/")) {
                    mc.thePlayer.sendChatMessage(text);
                } else {
                    MyMod.NETWORK.sendToServer(new PacketChatMessage(text, teamOnly));
                    com.myname.mymodid.client.SoundPlayer.play(text);
                    MyMod.NETWORK.sendToServer(new com.myname.mymodid.network.PacketVoiceRequest(text));
                }
            }
            scrollOffset = 0;
            mc.displayGuiScreen(null);
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(null); return; }
        input.textboxKeyTyped(c, keyCode);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) {
        super.mouseClicked(mx, my, btn);
        input.mouseClicked(mx, my, btn);
    }

    @Override
    public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
