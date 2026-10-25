package com.myname.mymodid.client.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import com.myname.mymodid.MyMod;
import com.myname.mymodid.network.PacketChatMessage;
import java.util.ArrayList;
import java.util.List;

public class QuickCommGui extends GuiScreen {
    private int page = -1;
    private int hovered = -1;
    private static final int RADIUS = 45;
    private static long lastSendTime = 0;
    // 复用列表，避免每帧 new
    private final List<String> cmdCache = new ArrayList<String>();

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        // 配置加载已在 QuickCommConfig 中加了 30 秒缓存
        QuickCommConfig.load();
    }

    private int cx() { return width / 2; }
    private int cy() { return height / 2 + 80; }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        int cx = cx(), cy = cy();
        if (page == -1) {
            hovered = getHovered(mouseX, mouseY, cx, cy, 6);
            drawRadial(cx, cy, 6, QuickCommConfig.catNames, hovered, 0xFFFF55);
        } else {
            cmdCache.clear();
            for (int i = 0; i < QuickCommConfig.CMD_PER_CAT; i++) {
                String cmd = QuickCommConfig.commands[page][i];
                if (cmd != null && !cmd.isEmpty()) cmdCache.add(cmd);
            }
            if (cmdCache.isEmpty()) {
                drawCenteredString(fontRenderer, I18n.format("dp.quickcomm.empty"), cx, cy - 4, 0xAAAAAA);
            } else {
                hovered = getHovered(mouseX, mouseY, cx, cy, cmdCache.size());
                drawRadial(cx, cy, cmdCache.size(), cmdCache.toArray(new String[0]), hovered, 0x55FFFF);
            }
        }
    }

    private int getHovered(int mouseX, int mouseY, int cx, int cy, int count) {
        double dx = mouseX - cx, dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < RADIUS && dist > 5) {
            double angle = Math.toDegrees(Math.atan2(dy, dx));
            double norm = (angle + 90 + 360) % 360;
            int idx = (int) (norm / (360.0 / count));
            if (idx >= count) idx = -1;
            return idx;
        }
        return -1;
    }

    private void drawRadial(int cx, int cy, int count, String[] labels, int hover, int hoverColor) {
        // TODO 1.12.2: 用新 Tessellator API 重写
    }

    private void drawScaledCentered(String text, int x, int y, int maxWidth, int color) {
        int w = fontRenderer.getStringWidth(text);
        if (w <= maxWidth) {
            fontRenderer.drawStringWithShadow(text, x - w / 2, y - 4, color);
        } else {
            float scale = (float) maxWidth / w;
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y - 4, 0);
            GL11.glScalef(scale, scale, 1);
            fontRenderer.drawStringWithShadow(text, -w / 2, 0, color);
            GL11.glPopMatrix();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int btn) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, btn);
        if (btn == 1) {
            if (page == -1) mc.displayGuiScreen(null);
            else page = -1;
        }
    }

    @Override
    protected void keyTyped(char c, int keyCode) throws java.io.IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(null); return; }
        if (page == -1) {
            if (keyCode >= Keyboard.KEY_1 && keyCode <= Keyboard.KEY_6) {
                int idx = keyCode - Keyboard.KEY_1;
                if (idx < QuickCommConfig.CATEGORIES) page = idx;
            }
        } else {
            if (keyCode == Keyboard.KEY_BACK) { page = -1; return; }
            if (keyCode >= Keyboard.KEY_1 && keyCode <= Keyboard.KEY_8) {
                int idx = keyCode - Keyboard.KEY_1, count = 0;
                for (int i = 0; i < QuickCommConfig.CMD_PER_CAT; i++) {
                    String cmd = QuickCommConfig.commands[page][i];
                    if (cmd == null || cmd.isEmpty()) continue;
                    if (count == idx) {
                        if (System.currentTimeMillis() - lastSendTime < 2000) {
                            mc.player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                net.minecraft.util.text.TextFormatting.YELLOW + I18n.format("dp.mark.spam")));
                            mc.displayGuiScreen(null);
                            return;
                        }
                        lastSendTime = System.currentTimeMillis();
                        // 发送聊天消息（轻量，立即发送）
                        MyMod.NETWORK.sendToServer(new PacketChatMessage(cmd, true));
                        // 播放声音（客户端，轻量）
                        com.myname.mymodid.client.SoundPlayer.play(cmd);
                        // 语音数据读取和上传放到异步线程，避免阻塞客户端线程拖死集成服务器
                        final String finalCmd = cmd;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    byte[] vd = com.myname.mymodid.client.SoundPlayer.readVoiceData(finalCmd);
                                    if (vd != null) {
                                        int cs = com.myname.mymodid.network.PacketVoiceUpload.CHUNK_SIZE;
                                        int total = (vd.length + cs - 1) / cs;
                                        for (int ci = 0; ci < total; ci++) {
                                            int s = ci * cs, e = Math.min(s + cs, vd.length);
                                            byte[] chunk = java.util.Arrays.copyOfRange(vd, s, e);
                                            MyMod.NETWORK.sendToServer(new com.myname.mymodid.network.PacketVoiceUpload(finalCmd, ci, total, chunk));
                                            // 分块上传间隔，避免瞬间发大量包导致服务端网络队列堆积
                                            Thread.sleep(20);
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }, "QuickCommVoiceUpload").start();
                        mc.displayGuiScreen(null);
                        return;
                    }
                    count++;
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        cmdCache.clear();
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
