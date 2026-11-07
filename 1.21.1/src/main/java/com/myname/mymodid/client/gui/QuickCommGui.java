package com.myname.mymodid.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.myname.mymodid.client.SoundPlayer;
import com.myname.mymodid.network.CPayloads.ChatPayload;
import com.myname.mymodid.network.CPayloads.VoiceUploadPayload;
import com.myname.mymodid.network.Network;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class QuickCommGui extends Screen {
    private int page = -1;
    private int hovered = -1;
    private static final int RADIUS = 45;
    private static long lastSendTime = 0;
    private final List<String> cmdCache = new ArrayList<>();

    public QuickCommGui() { super(Component.literal("Quick Comm")); }

    @Override
    protected void init() { QuickCommConfig.load(); }

    private int cx() { return width / 2; }
    private int cy() { return height / 2 + 80; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = cx(), cy = cy();
        if (page == -1) {
            hovered = getHovered(mouseX, mouseY, cx, cy, 6);
            drawRadial(g, cx, cy, 6, QuickCommConfig.catNames, hovered, 0xFFFF55);
        } else {
            cmdCache.clear();
            for (int i = 0; i < QuickCommConfig.CMD_PER_CAT; i++) {
                String cmd = QuickCommConfig.commands[page][i];
                if (cmd != null && !cmd.isEmpty()) cmdCache.add(cmd);
            }
            if (cmdCache.isEmpty()) {
                String emptyText = Component.translatable("dp.quickcomm.empty").getString();
                g.drawString(font, emptyText, cx - font.width(emptyText) / 2, cy - 4, 0xAAAAAA, false);
            } else {
                hovered = getHovered(mouseX, mouseY, cx, cy, cmdCache.size());
                drawRadial(g, cx, cy, cmdCache.size(), cmdCache.toArray(new String[0]), hovered, 0x55FFFF);
            }
        }
    }

    private int getHovered(double mouseX, double mouseY, int cx, int cy, int count) {
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

    private void drawRadial(GuiGraphics g, int cx, int cy, int count, String[] labels, int hover, int hoverColor) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        double seg = 360.0 / count;
        for (int i = 0; i < count; i++) {
            float s = (float) Math.toRadians(-90 + i * seg);
            float e = (float) Math.toRadians(-90 + (i + 1) * seg);
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            if (i == hover) bb.addVertex(cx, cy, 200).setColor(76, 153, 255, 140);
            else bb.addVertex(cx, cy, 200).setColor(38, 38, 38, 204);
            for (int k = 0; k <= 12; k++) {
                float a = s + (e - s) * k / 12;
                bb.addVertex((float) (cx + Math.cos(a) * RADIUS), (float) (cy + Math.sin(a) * RADIUS), 200)
                    .setColor(38, 38, 38, 204);
            }
            BufferUploader.drawWithShader(bb.build());
        }
        BufferBuilder ring = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= 48; i++) {
            double a = Math.PI * 2 * i / 48;
            ring.addVertex((float) (cx + Math.cos(a) * RADIUS), (float) (cy + Math.sin(a) * RADIUS), 200).setColor(178, 178, 178, 230);
        }
        BufferUploader.drawWithShader(ring.build());
        BufferBuilder inner = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= 32; i++) {
            double a = Math.PI * 2 * i / 32;
            inner.addVertex((float) (cx + Math.cos(a) * 8), (float) (cy + Math.sin(a) * 8), 200).setColor(128, 128, 128, 180);
        }
        BufferUploader.drawWithShader(inner.build());
        RenderSystem.disableBlend();
        for (int i = 0; i < count; i++) {
            double mid = Math.toRadians(-90 + i * seg + seg / 2);
            int tx = cx + (int) (Math.cos(mid) * RADIUS * 0.62);
            int ty = cy + (int) (Math.sin(mid) * RADIUS * 0.62);
            int col = (i == hover) ? hoverColor : 0xFFFFFF;
            drawScaledCentered(g, labels[i], tx, ty, (int) (RADIUS * 0.8), col);
        }
    }

    private void drawScaledCentered(GuiGraphics g, String text, int x, int y, int maxWidth, int color) {
        int w = font.width(text);
        if (w <= maxWidth) {
            g.drawString(font, text, x - w / 2, y - 4, color, false);
        } else {
            float scale = (float) maxWidth / w;
            var pose = g.pose();
            pose.pushPose();
            pose.translate(x, y - 4, 0);
            pose.scale(scale, scale, 1);
            g.drawString(font, text, -w / 2, 0, color, false);
            pose.popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (button == 1) {
            if (page == -1) onClose();
            else page = -1;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if (page == -1) {
            if (keyCode >= org.lwjgl.glfw.GLFW.GLFW_KEY_1 && keyCode <= org.lwjgl.glfw.GLFW.GLFW_KEY_6) {
                int idx = keyCode - org.lwjgl.glfw.GLFW.GLFW_KEY_1;
                if (idx < QuickCommConfig.CATEGORIES) page = idx;
                return true;
            }
        } else {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) { page = -1; return true; }
            if (keyCode >= org.lwjgl.glfw.GLFW.GLFW_KEY_1 && keyCode <= org.lwjgl.glfw.GLFW.GLFW_KEY_8) {
                int idx = keyCode - org.lwjgl.glfw.GLFW.GLFW_KEY_1, count = 0;
                for (int i = 0; i < QuickCommConfig.CMD_PER_CAT; i++) {
                    String cmd = QuickCommConfig.commands[page][i];
                    if (cmd == null || cmd.isEmpty()) continue;
                    if (count == idx) {
                        if (System.currentTimeMillis() - lastSendTime < 2000) {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.player != null) {
                                mc.player.displayClientMessage(Component.literal(
                                    ChatFormatting.YELLOW + Component.translatable("dp.mark.spam").getString()), false);
                            }
                            onClose();
                            return true;
                        }
                        lastSendTime = System.currentTimeMillis();
                        Network.sendToServer(new ChatPayload(cmd, true));
                        SoundPlayer.play(cmd);
                        final String finalCmd = cmd;
                        new Thread(() -> {
                            try {
                                byte[] vd = SoundPlayer.readVoiceData(finalCmd);
                                if (vd != null) {
                                    int cs = 30000;
                                    int total = (vd.length + cs - 1) / cs;
                                    for (int ci = 0; ci < total; ci++) {
                                        int s = ci * cs, e = Math.min(s + cs, vd.length);
                                        byte[] chunk = java.util.Arrays.copyOfRange(vd, s, e);
                                        Network.sendToServer(new VoiceUploadPayload(finalCmd, ci, total, chunk));
                                        Thread.sleep(20);
                                    }
                                }
                            } catch (Exception ex) { ex.printStackTrace(); }
                        }, "QuickCommVoiceUpload").start();
                        onClose();
                        return true;
                    }
                    count++;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
