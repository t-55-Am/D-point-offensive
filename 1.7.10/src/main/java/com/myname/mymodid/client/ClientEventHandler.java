package com.myname.mymodid.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import com.myname.mymodid.MyMod;
import com.myname.mymodid.config.MyModConfig;
import com.myname.mymodid.score.CapturePointData;
import com.myname.mymodid.score.ScoreData;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ClientEventHandler {
    private static boolean lastKeyQuickComm = false;
    private static boolean lastKeyQuickCommConfig = false;
    private static boolean lastKeyChatGui = false;
    private static boolean lastKeyMark = false;
    private static boolean lastKeySlash = false;
    private static long lastMarkTime = 0;

    public static int redScore = 10000;
    public static int blueScore = 10000;
    public static int mode = 0;
    public static Map<String, int[]> playerStats = new java.util.HashMap<String, int[]>();
    public static boolean pveEnabled = false;
    public static List<String> chatHistory = new ArrayList<String>();
    public static final int MAX_CHAT_HISTORY = 100;

    public static final KeyBinding keyQuickComm =
        new KeyBinding("key.quickcomm", Keyboard.KEY_T, "key.categories.mymodid");
    public static final KeyBinding keyQuickCommConfig =
        new KeyBinding("key.quickcommconfig", Keyboard.KEY_Y, "key.categories.mymodid");
    public static final KeyBinding keyChatGui =
        new KeyBinding("key.chatgui", Keyboard.KEY_U, "key.categories.mymodid");
    public static final KeyBinding keyMark =
        new KeyBinding("key.mark", Keyboard.KEY_M, "key.categories.mymodid");

    // ===== 战术标记 =====
    public static final List<Mark> marks = new ArrayList<Mark>();

    public static class Mark {
        public int x, y, z;
        public float hx, hy, hz;
        public String playerName;
        public long createdAt;

        public Mark(int x, int y, int z, float hx, float hy, float hz, String playerName) {
            this.x = x; this.y = y; this.z = z;
            this.hx = hx; this.hy = hy; this.hz = hz;
            this.playerName = playerName;
            this.createdAt = System.currentTimeMillis();
        }
    }

    public static void addMark(int x, int y, int z, float hx, float hy, float hz, String name) {
        marks.add(new Mark(x, y, z, hx, hy, hz, name));
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && name != null) {
            double dx = (double) hx - mc.thePlayer.posX;
            double dz = (double) hz - mc.thePlayer.posZ;
            double bearing = Math.toDegrees(Math.atan2(dx, -dz));
            if (bearing < 0) bearing += 360;
            int br = (int) Math.round(bearing);
            String msg = EnumChatFormatting.YELLOW + "[" + name + "] " +
                EnumChatFormatting.WHITE + StatCollector.translateToLocal("dp.mark.coord") + " " + x + "," + y + "," + z +
                EnumChatFormatting.GOLD + "  " + StatCollector.translateToLocal("dp.mark.bearing") + " " + br + "°";
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(msg));
            chatHistory.add(msg);
            if (chatHistory.size() > MAX_CHAT_HISTORY) chatHistory.remove(0);
        }
    }

    public static void removeMark(int x, int y, int z) {
        Iterator<Mark> it = marks.iterator();
        while (it.hasNext()) {
            Mark m = it.next();
            if (m.x == x && m.y == y && m.z == z) { it.remove(); break; }
        }
    }

    @SubscribeEvent
    public void onTabList(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.PLAYER_LIST
            && Keyboard.isKeyDown(15)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.theWorld == null) return;
        ScoreData sd = ScoreData.get(mc.theWorld);
        pveEnabled = sd.pveEnabled;
        if (!sd.pveEnabled && sd.gameStarted && MyModConfig.enableScoreBar) {
            renderScoreBar(mc, event.resolution);
        }
        if (MyModConfig.enableRadar) renderRadar(mc, event.resolution);
        if (Keyboard.isKeyDown(15)) {
            renderTabStats(mc, event.resolution);
        }
        if (MyModConfig.enableCaptureRing) {
            for (Object te : mc.theWorld.loadedTileEntityList) {
                if (te instanceof TileEntityCapturePoint) {
                    TileEntityCapturePoint point = (TileEntityCapturePoint) te;
                    int r = point.getRadius();
                    double dx = player.posX - (point.xCoord + 0.5);
                    double dy = player.posY - (point.yCoord + 0.5);
                    double dz = player.posZ - (point.zCoord + 0.5);
                    if (dx * dx + dy * dy + dz * dz <= (r + 1) * (r + 1)) {
                        renderCaptureRing(mc, event.resolution, point);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (MyModConfig.enableRangeCircle) {
            for (Object te : mc.theWorld.loadedTileEntityList) {
                if (te instanceof TileEntityCapturePoint) {
                    renderRangeCircle(mc, (TileEntityCapturePoint) te, event.partialTicks);
                }
            }
        }
        if (MyModConfig.enableFloatingName) {
            renderFloatingNames(mc, event.partialTicks);
        }
        renderMarks(mc, event.partialTicks);
    }

    @SubscribeEvent
    public void onClientTick(cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if (event.phase != cpw.mods.fml.common.gameevent.TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        boolean hasGui = mc.currentScreen != null;

        // 战术标记：射线同时检测方块和实体，取最近的命中点
        boolean mk = Keyboard.isKeyDown(keyMark.getKeyCode());
        if (!hasGui && mk && !lastKeyMark) {
            long now = System.currentTimeMillis();
            if (now - lastMarkTime < 2000) {
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                    EnumChatFormatting.YELLOW + StatCollector.translateToLocal("dp.mark.spam")));
            } else {
                lastMarkTime = now;
                Vec3 start = Vec3.createVectorHelper(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY + mc.thePlayer.getEyeHeight(),
                    mc.thePlayer.posZ);
                Vec3 look = mc.thePlayer.getLook(1.0f);
                Vec3 end = start.addVector(look.xCoord * 2000, look.yCoord * 2000, look.zCoord * 2000);
                // 方块命中
                MovingObjectPosition blockHit = mc.theWorld.rayTraceBlocks(start, end);
                double blockDist = (blockHit == null) ? Double.MAX_VALUE : blockHit.hitVec.distanceTo(start);
                // 实体命中
                MovingObjectPosition entityHit = null;
                double entityDist = Double.MAX_VALUE;
                AxisAlignedBB rayBox = AxisAlignedBB.getBoundingBox(
                    start.xCoord, start.yCoord, start.zCoord,
                    end.xCoord, end.yCoord, end.zCoord).expand(1.0, 1.0, 1.0);
                List entities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.thePlayer, rayBox);
                for (Object o : entities) {
                    if (o instanceof Entity && ((Entity) o).canBeCollidedWith()) {
                        MovingObjectPosition mop = ((Entity) o).boundingBox.calculateIntercept(start, end);
                        if (mop != null) {
                            mop.typeOfHit = MovingObjectPosition.MovingObjectType.ENTITY;
                            mop.entityHit = (Entity) o;
                            double d = mop.hitVec.distanceTo(start);
                            if (d < entityDist) { entityDist = d; entityHit = mop; }
                        }
                    }
                }
                // 取最近的命中
                MovingObjectPosition finalHit = (blockDist <= entityDist) ? blockHit : entityHit;
                if (finalHit != null && finalHit.hitVec != null) {
                    int bx, by, bz;
                    if (finalHit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                        bx = finalHit.blockX; by = finalHit.blockY; bz = finalHit.blockZ;
                    } else {
                        bx = (int) Math.floor(finalHit.entityHit.posX);
                        by = (int) Math.floor(finalHit.entityHit.posY);
                        bz = (int) Math.floor(finalHit.entityHit.posZ);
                    }
                    MyMod.NETWORK.sendToServer(new com.myname.mymodid.network.PacketMarkRequest(
                        bx, by, bz,
                        (float) finalHit.hitVec.xCoord,
                        (float) finalHit.hitVec.yCoord,
                        (float) finalHit.hitVec.zCoord));
                }
            }
        }
        lastKeyMark = mk;

        // 按 / 打开指令输入框
        boolean slash = Keyboard.isKeyDown(Keyboard.KEY_SLASH);
        if (slash && !lastKeySlash && mc.currentScreen == null) {
            mc.displayGuiScreen(new net.minecraft.client.gui.GuiChat("/"));
        }
        lastKeySlash = slash;

        // 快捷交流：有GUI时不响应
        boolean qc = Keyboard.isKeyDown(keyQuickComm.getKeyCode());
        boolean qcc = Keyboard.isKeyDown(keyQuickCommConfig.getKeyCode());
        boolean cg = Keyboard.isKeyDown(keyChatGui.getKeyCode());
        if (!hasGui) {
            if (qc && !lastKeyQuickComm) mc.displayGuiScreen(new com.myname.mymodid.client.gui.QuickCommGui());
            if (qcc && !lastKeyQuickCommConfig) mc.displayGuiScreen(new com.myname.mymodid.client.gui.QuickCommConfigGui());
            if (cg && !lastKeyChatGui) mc.displayGuiScreen(new com.myname.mymodid.client.gui.ChatGui());
        }
        lastKeyQuickComm = qc;
        lastKeyQuickCommConfig = qcc;
        lastKeyChatGui = cg;
    }

    private void renderScoreBar(Minecraft mc, ScaledResolution res) {
        ScoreData sd = ScoreData.get(mc.theWorld);
        if (sd != null && sd.mode == 1) {
            renderMultiScore(mc, res, sd);
            return;
        }
        int barWidth = Math.max(40, com.myname.mymodid.config.MyModConfig.scoreBarWidth);
        int barHeight = 12;
        int x = (res.getScaledWidth() - barWidth) / 2 + com.myname.mymodid.config.MyModConfig.scoreBarX;
        int y = com.myname.mymodid.config.MyModConfig.scoreBarY;
        Gui.drawRect(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, 0x90000000);
        int half = barWidth / 2;
        int redW = (int) ((long) redScore * half / 10000);
        int blueW = (int) ((long) blueScore * half / 10000);
        redW = Math.max(0, Math.min(half, redW));
        blueW = Math.max(0, Math.min(half, blueW));
        Gui.drawRect(x, y, x + half, y + barHeight, 0x40FF0000);
        Gui.drawRect(x, y, x + redW, y + barHeight, 0xFFFF0000);
        Gui.drawRect(x + half, y, x + barWidth, y + barHeight, 0x400000FF);
        Gui.drawRect(x + barWidth - blueW, y, x + barWidth, y + barHeight, 0xFF0000FF);
        Gui.drawRect(x + half - 1, y - 2, x + half + 1, y + barHeight + 2, 0xFFFFFFFF);
        mc.fontRenderer.drawStringWithShadow(EnumChatFormatting.RED + "" + redScore, x + 5, y + 2, 0xFFFFFF);
        String bs = "" + blueScore;
        mc.fontRenderer.drawStringWithShadow(EnumChatFormatting.BLUE + bs, x + barWidth - mc.fontRenderer.getStringWidth(bs) - 5, y + 2, 0xFFFFFF);
    }

    /** v1.1.11 多队伍模式 HUD：每队一行（队伍色 + 名字 + 分数），按分数从高到低排列 */
    private void renderMultiScore(Minecraft mc, ScaledResolution res, ScoreData sd) {
        if (sd.teamScores == null || sd.teamScores.isEmpty()) return;
        int w = Math.max(40, com.myname.mymodid.config.MyModConfig.scoreBarWidth);
        int rowH = 12;
        java.util.List<java.util.Map.Entry<String, Integer>> list =
            new java.util.ArrayList<java.util.Map.Entry<String, Integer>>(sd.teamScores.entrySet());
        java.util.Collections.sort(list, new java.util.Comparator<java.util.Map.Entry<String, Integer>>() {
            @Override
            public int compare(java.util.Map.Entry<String, Integer> a, java.util.Map.Entry<String, Integer> b) {
                int cmp = (b.getValue() == null ? 0 : b.getValue()) - (a.getValue() == null ? 0 : a.getValue());
                if (cmp != 0) return cmp;
                return a.getKey().compareTo(b.getKey());
            }
        });
        int rows = list.size();
        int h = rows * rowH + 6;
        int x = (res.getScaledWidth() - w) / 2 + com.myname.mymodid.config.MyModConfig.scoreBarX;
        int y = com.myname.mymodid.config.MyModConfig.scoreBarY;
        Gui.drawRect(x - 2, y - 2, x + w + 2, y + h + 2, 0x90000000);
        for (int i = 0; i < rows; i++) {
            java.util.Map.Entry<String, Integer> e = list.get(i);
            String name = e.getKey();
            int colIdx = teamColor(name, null);
            mc.fontRenderer.drawStringWithShadow(name, x + 4, y + 3 + i * rowH, argb(colIdx));
            String score = String.valueOf(e.getValue());
            mc.fontRenderer.drawStringWithShadow(score, x + w - 4 - mc.fontRenderer.getStringWidth(score), y + 3 + i * rowH, 0xFFFFFF);
        }
    }

    private void renderTabStats(Minecraft mc, ScaledResolution res) {
        if (mc.theWorld == null) return;
        FontRenderer fr = mc.fontRenderer;
        int pw = res.getScaledWidth();
        int panelY = res.getScaledHeight() / 6;
        int rowH = 14;
        // 合并在线玩家和统计数据，保证死亡玩家也显示
        List allNames = new ArrayList();
        for (Object obj : mc.theWorld.playerEntities) {
            if (obj instanceof EntityPlayer) {
                String n = ((EntityPlayer) obj).getCommandSenderName();
                if (!allNames.contains(n)) allNames.add(n);
            }
        }
        for (Object obj : playerStats.keySet()) {
            String n = (String) obj;
            if (!allNames.contains(n)) allNames.add(n);
        }
        Comparator<String> byKill = new Comparator<String>() {
            public int compare(String a, String b) {
                int[] sa = playerStats.get(a);
                int[] sb2 = playerStats.get(b);
                return (sb2 == null ? 0 : sb2[0]) - (sa == null ? 0 : sa[0]);
            }
        };
        Collections.sort(allNames, byKill);
        if (pveEnabled) {
            int panelW = 300;
            int panelX = (pw - panelW) / 2;
            int h = 18 + allNames.size() * rowH;
            Gui.drawRect(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + h + 2, 0xFF000000);
            Gui.drawRect(panelX, panelY, panelX + panelW, panelY + h, 0xB0000000);
            fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.name"), panelX + 4, panelY + 4, 0xFFFF55);
            fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.kill"), panelX + 90, panelY + 4, 0x55FF55);
            fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.assist"), panelX + 130, panelY + 4, 0xFFFF55);
            fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.capture"), panelX + 170, panelY + 4, 0x55FFFF);
            fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.death"), panelX + 210, panelY + 4, 0xFF5555);
            int yy = panelY + 18;
            for (Object o : allNames) {
                String name = (String) o;
                int[] s = playerStats.get(name);
                if (s == null) s = new int[]{0, 0, 0, 0};
                int nc = nameColor(mc, name, null);
                fr.drawStringWithShadow(name, panelX + 4, yy, nc);
                fr.drawStringWithShadow(String.valueOf(s[0]), panelX + 90, yy, 0x55FF55);
                fr.drawStringWithShadow(String.valueOf(s[1]), panelX + 130, yy, 0xFFFF55);
                fr.drawStringWithShadow(String.valueOf(s[2]), panelX + 170, yy, 0x55FFFF);
                fr.drawStringWithShadow(String.valueOf(s[3]), panelX + 210, yy, 0xFF5555);
                yy += rowH;
            }
        } else {
            Scoreboard sb = mc.theWorld.getScoreboard();
            List reds = new ArrayList();
            List blues = new ArrayList();
            List others = new ArrayList();
            for (Object o : allNames) {
                String name = (String) o;
                Team t = sb.getPlayersTeam(name);
                String tn = (t == null) ? "" : t.getRegisteredName();
                String lower = tn.toLowerCase();
                if (lower.contains("red") || tn.contains("红")) reds.add(name);
                else if (lower.contains("blue") || tn.contains("蓝")) blues.add(name);
                else others.add(name);
            }
            Collections.sort(reds, byKill);
            Collections.sort(blues, byKill);
            Collections.sort(others, byKill);
            int panelW = 420;
            int panelX = (pw - panelW) / 2;
            int midX = panelX + 210;
            int teamRows = Math.max(reds.size(), blues.size());
            int teamH = 18 + teamRows * rowH;
            int otherH = others.isEmpty() ? 0 : 18 + others.size() * rowH;
            int totalH = teamH + (others.isEmpty() ? 0 : 4 + otherH);
            Gui.drawRect(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + totalH + 2, 0xFF000000);
            Gui.drawRect(panelX, panelY, panelX + panelW, panelY + totalH, 0xB0000000);
            Gui.drawRect(midX, panelY, midX + 1, panelY + teamH, 0xFFFFFFFF);
            fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.redteam"), panelX + 4, panelY + 4, 0xFF5555);
            fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.kill"), panelX + 60, panelY + 4, 0x55FF55);
            fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.assist"), panelX + 95, panelY + 4, 0xFFFF55);
            fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.capture"), panelX + 130, panelY + 4, 0x55FFFF);
            fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.death"), panelX + 165, panelY + 4, 0xFF5555);
            fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.blueteam"), midX + 4, panelY + 4, 0x5555FF);
            fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.kill"), midX + 60, panelY + 4, 0x55FF55);
            fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.assist"), midX + 95, panelY + 4, 0xFFFF55);
            fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.capture"), midX + 130, panelY + 4, 0x55FFFF);
            fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.death"), midX + 165, panelY + 4, 0xFF5555);
            int yy = panelY + 18;
            for (int i = 0; i < teamRows; i++) {
                if (i < reds.size()) {
                    String name = (String) reds.get(i);
                    int[] s = playerStats.get(name);
                    if (s == null) s = new int[]{0, 0, 0, 0};
                    fr.drawStringWithShadow(name, panelX + 4, yy, nameColor(mc, name, "red"));
                    fr.drawStringWithShadow(String.valueOf(s[0]), panelX + 60, yy, 0x55FF55);
                    fr.drawStringWithShadow(String.valueOf(s[1]), panelX + 95, yy, 0xFFFF55);
                    fr.drawStringWithShadow(String.valueOf(s[2]), panelX + 130, yy, 0x55FFFF);
                    fr.drawStringWithShadow(String.valueOf(s[3]), panelX + 165, yy, 0xFF5555);
                }
                if (i < blues.size()) {
                    String name = (String) blues.get(i);
                    int[] s = playerStats.get(name);
                    if (s == null) s = new int[]{0, 0, 0, 0};
                    fr.drawStringWithShadow(name, midX + 4, yy, nameColor(mc, name, "blue"));
                    fr.drawStringWithShadow(String.valueOf(s[0]), midX + 60, yy, 0x55FF55);
                    fr.drawStringWithShadow(String.valueOf(s[1]), midX + 95, yy, 0xFFFF55);
                    fr.drawStringWithShadow(String.valueOf(s[2]), midX + 130, yy, 0x55FFFF);
                    fr.drawStringWithShadow(String.valueOf(s[3]), midX + 165, yy, 0xFF5555);
                }
                yy += rowH;
            }
            if (!others.isEmpty()) {
                int sepY = panelY + teamH + 2;
                Gui.drawRect(panelX, sepY, panelX + panelW, sepY + 1, 0xFFFFFFFF);
                int otherY = sepY + 4;
                fr.drawStringWithShadow(StatCollector.translateToLocal("dp.tab.noteam"), panelX + 4, otherY, 0xAAAAAA);
                int oy = otherY + 18;
                for (Object o : others) {
                    String name = (String) o;
                    fr.drawStringWithShadow(name, panelX + 4, oy, nameColor(mc, name, null));
                    oy += rowH;
                }
            }
        }
    }

    private int nameColor(Minecraft mc, String name, String forceTeam) {
        EntityPlayer ep = null;
        for (Object obj : mc.theWorld.playerEntities) {
            if (obj instanceof EntityPlayer) {
                EntityPlayer p = (EntityPlayer) obj;
                if (p.getCommandSenderName().equals(name)) { ep = p; break; }
            }
        }
        if (ep == null || ep.isDead || ep.getHealth() <= 0) return 0xAAAAAA;
        if ("red".equals(forceTeam)) return 0xFF5555;
        if ("blue".equals(forceTeam)) return 0x5555FF;
        Team t = mc.theWorld.getScoreboard().getPlayersTeam(name);
        if (t != null) {
            String tn = t.getRegisteredName();
            String lower = tn.toLowerCase();
            if (lower.contains("red") || tn.contains("红")) return 0xFF5555;
            if (lower.contains("blue") || tn.contains("蓝")) return 0x5555FF;
        }
        return 0xFFFFFF;
    }

    private void renderRadar(Minecraft mc, ScaledResolution res) {
        EntityPlayer p = mc.thePlayer;
        if (p == null) return;
        Map<Long, CapturePointData.Entry> pts = getClientPoints(mc);
        int barH = 12;
        int barY = com.myname.mymodid.config.MyModConfig.radarY;
        int barW = com.myname.mymodid.config.MyModConfig.radarWidth > 0 ? com.myname.mymodid.config.MyModConfig.radarWidth : res.getScaledWidth() / 2;
        int barX = (res.getScaledWidth() - barW) / 2;
        int mid = barX + barW / 2;
        Gui.drawRect(barX, barY, barX + barW, barY + barH, 0x90000000);
        float front = norm(p.rotationYaw + 180f);
        int startDeg = ((int) front - 90) / 10 * 10;
        for (int a = startDeg; a <= front + 90; a += 10) {
            float aa = norm(a);
            float rel = norm(aa - front);
            if (rel > 180) rel -= 360;
            if (rel < -90 || rel > 90) continue;
            int x = mid + (int) (rel / 90.0 * (barW / 2.0));
            int mod = ((int) (aa % 360 + 360)) % 360;
            if (mod == 0 || mod == 90 || mod == 180 || mod == 270) {
                Gui.drawRect(x, barY - 4, x + 1, barY + barH + 4, 0xFFFFFFFF);
                String label = mod == 0 ? "N" : mod == 90 ? "E" : mod == 180 ? "S" : "W";
                mc.fontRenderer.drawStringWithShadow(label, x - mc.fontRenderer.getStringWidth(label) / 2, barY + barH + 2, 0xFFFFFF);
            } else {
                Gui.drawRect(x, barY - 2, x + 1, barY + barH + 2, 0x88FFFFFF);
                String dl = String.valueOf(mod);
                mc.fontRenderer.drawStringWithShadow(dl, x - mc.fontRenderer.getStringWidth(dl) / 2, barY + barH + 2, 0xAAAAAA);
            }
        }
        Gui.drawRect(mid - 2, barY - 3, mid + 2, barY + barH + 3, 0xFFFFFFFF);
        for (CapturePointData.Entry e : pts.values()) {
            double dx = e.x + 0.5 - p.posX;
            double dz = e.z + 0.5 - p.posZ;
            double bearing = Math.toDegrees(Math.atan2(dx, -dz));
            float rel = norm((float) bearing - front);
            if (rel < -90 || rel > 90) continue;
            int x = mid + (int) (rel / 90.0 * (barW / 2.0));
            int color = argb(teamColor(e.ownerTeam, e.capturingTeam));
            TileEntity te = mc.theWorld.getTileEntity(e.x, e.y, e.z);
            boolean isMain = (te instanceof TileEntityCapturePoint) && ((TileEntityCapturePoint) te).isMainBase();
            if (isMain) {
                Gui.drawRect(x - 5, barY, x + 5, barY + barH, 0xFFFFFF00);
                Gui.drawRect(x - 4, barY + 1, x + 4, barY + barH - 1, color);
            } else {
                Gui.drawRect(x - 3, barY + 1, x + 3, barY + barH - 1, color);
            }
            String nm = (e.name == null || e.name.isEmpty()) ? StatCollector.translateToLocal("dp.capture.point") : e.name;
            mc.fontRenderer.drawStringWithShadow(nm, x - mc.fontRenderer.getStringWidth(nm) / 2, barY + barH + 4, color);
        }
    }

    private void renderCaptureRing(Minecraft mc, ScaledResolution res, TileEntityCapturePoint point) {
        int cx = res.getScaledWidth() / 2;
        int cy = res.getScaledHeight() / 2 + 70;
        int outerR = 14, innerR = 10;
        double progress = Math.max(0, Math.min(1, point.getCaptureProgress() / 100.0));
        float[] c = rgb(teamColor(point.getOwnerTeam(), point.getCapturingTeam()));
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        Tessellator tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLE_STRIP);
        tess.setColorRGBA_F(0.1f, 0.1f, 0.1f, 0.8f);
        for (int i = 0; i <= 48; i++) {
            double a = Math.PI * 2 * i / 48;
            tess.addVertex(cx + Math.cos(a) * outerR, cy + Math.sin(a) * outerR, 0);
            tess.addVertex(cx + Math.cos(a) * innerR, cy + Math.sin(a) * innerR, 0);
        }
        tess.draw();
        if (progress > 0.001) {
            int segs = Math.max(1, (int) (48 * progress));
            tess.startDrawing(GL11.GL_TRIANGLE_STRIP);
            tess.setColorRGBA_F(c[0], c[1], c[2], 1f);
            for (int i = 0; i <= segs; i++) {
                double a = -Math.PI / 2 + Math.PI * 2 * progress * i / segs;
                tess.addVertex(cx + Math.cos(a) * outerR, cy + Math.sin(a) * outerR, 0);
                tess.addVertex(cx + Math.cos(a) * innerR, cy + Math.sin(a) * innerR, 0);
            }
            tess.draw();
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        String capturing = point.getCapturingTeam(), owner = point.getOwnerTeam();
        String pct = (int) (progress * 100) + "%";
        if (capturing != null && !capturing.isEmpty()) {
            mc.fontRenderer.drawStringWithShadow(pct, cx - mc.fontRenderer.getStringWidth(pct) / 2, cy - 4, 0xFFFFFF);
            mc.fontRenderer.drawStringWithShadow(capturing, cx - mc.fontRenderer.getStringWidth(capturing) / 2, cy + 8, argb(teamColor(owner, capturing)));
        } else if (owner != null && !owner.isEmpty()) {
            mc.fontRenderer.drawStringWithShadow(owner, cx - mc.fontRenderer.getStringWidth(owner) / 2, cy - 3, argb(teamColor(owner, capturing)));
        } else {
            mc.fontRenderer.drawStringWithShadow(pct, cx - mc.fontRenderer.getStringWidth(pct) / 2, cy - 4, 0xFFFFFF);
        }
    }

    private void renderRangeCircle(Minecraft mc, TileEntityCapturePoint point, float partialTicks) {
        EntityPlayer p = mc.thePlayer;
        if (p == null) return;
        double ddx = point.xCoord + 0.5 - p.posX, ddz = point.zCoord + 0.5 - p.posZ;
        if (ddx * ddx + ddz * ddz > 64 * 64) return;
        double px = p.lastTickPosX + (p.posX - p.lastTickPosX) * partialTicks;
        double py = p.lastTickPosY + (p.posY - p.lastTickPosY) * partialTicks;
        double pz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * partialTicks;
        String owner = point.getOwnerTeam();
        String capturing = point.getCapturingTeam();
        for (CapturePointData.Entry e : getClientPoints(mc).values()) {
            if (e.x == point.xCoord && e.y == point.yCoord && e.z == point.zCoord) {
                if (e.ownerTeam != null && !e.ownerTeam.isEmpty()) owner = e.ownerTeam;
                if (e.capturingTeam != null && !e.capturingTeam.isEmpty()) capturing = e.capturingTeam;
                break;
            }
        }
        float[] c = rgb(teamColor(owner, capturing));
        boolean isMain = point.isMainBase();
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glTranslated(-px, -py, -pz);
        int r = point.getRadius();
        double cx = point.xCoord + 0.5, cy = point.yCoord + 0.06, cz = point.zCoord + 0.5;
        Tessellator tess = Tessellator.instance;
        if (isMain) {
            GL11.glLineWidth(8.0F);
            GL11.glColor4f(1f, 0.84f, 0f, 0.95f);
            tess.startDrawing(GL11.GL_LINE_LOOP);
            tess.setColorRGBA_F(1f, 0.84f, 0f, 0.95f);
            for (int i = 0; i < 64; i++) {
                double a = Math.PI * 2 * i / 64;
                tess.addVertex(cx + Math.cos(a) * (r + 2), cy, cz + Math.sin(a) * (r + 2));
            }
            tess.draw();
        }
        GL11.glLineWidth(6.0F);
        GL11.glColor4f(c[0], c[1], c[2], 0.9f);
        tess.startDrawing(GL11.GL_LINE_LOOP);
        tess.setColorRGBA_F(c[0], c[1], c[2], 0.9f);
        for (int i = 0; i < 64; i++) {
            double a = Math.PI * 2 * i / 64;
            tess.addVertex(cx + Math.cos(a) * r, cy, cz + Math.sin(a) * r);
        }
        tess.draw();
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private void renderFloatingNames(Minecraft mc, float partialTicks) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        for (CapturePointData.Entry e : getClientPoints(mc).values()) {
            renderFloatingName(mc, e, partialTicks);
        }
    }

    private void renderFloatingName(Minecraft mc, CapturePointData.Entry e, float partialTicks) {
        EntityPlayer p = mc.thePlayer;
        double px = p.lastTickPosX + (p.posX - p.lastTickPosX) * partialTicks;
        double py = p.lastTickPosY + (p.posY - p.lastTickPosY) * partialTicks;
        double pz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * partialTicks;
        double dx = e.x + 0.5 - px, dy = e.y + 2.6 - py, dz = e.z + 0.5 - pz;
        String name = (e.name == null || e.name.isEmpty()) ? StatCollector.translateToLocal("dp.capture.point") : e.name;
        double progress = Math.max(0, Math.min(1, e.progress / 100.0));
        TileEntity te = mc.theWorld.getTileEntity(e.x, e.y, e.z);
        boolean isMain = (te instanceof TileEntityCapturePoint) && ((TileEntityCapturePoint) te).isMainBase();
        String owner = e.ownerTeam;
        String capturing = e.capturingTeam;
        if (te instanceof TileEntityCapturePoint) {
            TileEntityCapturePoint cp = (TileEntityCapturePoint) te;
            if (owner == null || owner.isEmpty()) owner = cp.getOwnerTeam();
            if (capturing == null || capturing.isEmpty()) capturing = cp.getCapturingTeam();
        }
        int color = argb(teamColor(owner, capturing));
        float[] c = rgb(teamColor(owner, capturing));
        int nameW = mc.fontRenderer.getStringWidth(name);
        int outerR = Math.max(18, nameW / 2 + 12);
        int innerR = Math.max(10, outerR - 6);
        float scale = 0.025f;
        GL11.glPushMatrix();
        GL11.glTranslated(dx, dy, dz);
        GL11.glRotatef(-RenderManager.instance.playerViewY, 0, 1, 0);
        GL11.glRotatef(RenderManager.instance.playerViewX, 1, 0, 0);
        GL11.glScalef(-scale, -scale, scale);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Tessellator tess = Tessellator.instance;
        if (isMain) {
            tess.startDrawing(GL11.GL_TRIANGLE_STRIP);
            tess.setColorRGBA_F(1f, 0.84f, 0f, 0.9f);
            for (int i = 0; i <= 48; i++) {
                double a = Math.PI * 2 * i / 48;
                tess.addVertex(Math.cos(a) * (outerR + 3), Math.sin(a) * (outerR + 3), 0);
                tess.addVertex(Math.cos(a) * (outerR + 1), Math.sin(a) * (outerR + 1), 0);
            }
            tess.draw();
        }
        tess.startDrawing(GL11.GL_TRIANGLE_STRIP);
        tess.setColorRGBA_F(c[0], c[1], c[2], 0.3f);
        for (int i = 0; i <= 48; i++) {
            double a = Math.PI * 2 * i / 48;
            tess.addVertex(Math.cos(a) * outerR, Math.sin(a) * outerR, 0);
            tess.addVertex(Math.cos(a) * innerR, Math.sin(a) * innerR, 0);
        }
        tess.draw();
        tess.startDrawing(GL11.GL_TRIANGLE_FAN);
        tess.setColorRGBA_F(c[0], c[1], c[2], 0.15f);
        tess.addVertex(0, 0, 0);
        for (int i = 0; i <= 48; i++) {
            double a = Math.PI * 2 * i / 48;
            tess.addVertex(Math.cos(a) * innerR, Math.sin(a) * innerR, 0);
        }
        tess.draw();
        if (progress > 0.001) {
            int segs = Math.max(1, (int) (48 * progress));
            tess.startDrawing(GL11.GL_TRIANGLE_STRIP);
            tess.setColorRGBA_F(c[0], c[1], c[2], 1f);
            for (int i = 0; i <= segs; i++) {
                double a = -Math.PI / 2 + Math.PI * 2 * progress * i / segs;
                tess.addVertex(Math.cos(a) * outerR, Math.sin(a) * outerR, 0);
                tess.addVertex(Math.cos(a) * innerR, Math.sin(a) * innerR, 0);
            }
            tess.draw();
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        mc.fontRenderer.drawStringWithShadow(name, -nameW / 2, -4, 0xFFFFFFFF);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void renderMarks(Minecraft mc, float partialTicks) {
        if (marks.isEmpty()) return;
        EntityPlayer p = mc.thePlayer;
        double px = p.lastTickPosX + (p.posX - p.lastTickPosX) * partialTicks;
        double py = p.lastTickPosY + (p.posY - p.lastTickPosY) * partialTicks;
        double pz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * partialTicks;
        GL11.glPushMatrix();
        GL11.glTranslated(-px, -py, -pz);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);
        long now = System.currentTimeMillis();
        Iterator<Mark> it = marks.iterator();
        while (it.hasNext()) {
            Mark m = it.next();
            if (now - m.createdAt > 5000) { it.remove(); continue; }
            float scale = 0.025f;
            GL11.glPushMatrix();
            GL11.glTranslated(m.hx, m.hy, m.hz);
            GL11.glRotatef(-RenderManager.instance.playerViewY, 0, 1, 0);
            GL11.glRotatef(RenderManager.instance.playerViewX, 1, 0, 0);
            GL11.glScalef(-scale, -scale, scale);
            Tessellator tess = Tessellator.instance;
            // 外层红色光晕倒三角
            tess.startDrawing(GL11.GL_TRIANGLES);
            tess.setColorRGBA_F(1.0f, 0.0f, 0.0f, 0.3f);
            tess.addVertex(-14, -10, 0);
            tess.addVertex(14, -10, 0);
            tess.addVertex(0, 14, 0);
            tess.draw();
            // 内层实色红色倒三角
            tess.startDrawing(GL11.GL_TRIANGLES);
            tess.setColorRGBA_F(1.0f, 0.0f, 0.0f, 1.0f);
            tess.addVertex(-10, -7, 0);
            tess.addVertex(10, -7, 0);
            tess.addVertex(0, 10, 0);
            tess.draw();
            // 玩家名称在倒三角上方
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            int w = mc.fontRenderer.getStringWidth(m.playerName);
            mc.fontRenderer.drawStringWithShadow(m.playerName, -w / 2, 22, 0xFFFF5555);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private float norm(float a) {
        while (a > 180) a -= 360;
        while (a < -180) a += 360;
        return a;
    }

    private int teamColor(String owner, String capturing) {
        String t = (capturing != null && !capturing.isEmpty()) ? capturing : owner;
        if (t != null && !t.isEmpty()) {
            // v1.1.8：优先按队伍名查全局队伍色板（自由队伍系统）
            com.myname.mymodid.score.TeamConfig cfg = com.myname.mymodid.score.TeamData.clientFind(t);
            if (cfg != null) return cfg.color;
            // 兜底：队伍不在列表（旧存档/未同步）按关键字猜
            String lower = t.toLowerCase();
            if (lower.contains("red") || t.contains("红") || lower.equals("1") || lower.contains("attack")) return 0;
            if (lower.contains("blue") || t.contains("蓝") || lower.equals("2")) return 1;
        }
        return -1; // 无队伍 → 白色
    }

    private float[] rgb(int color) {
        if (color < 0 || color >= com.myname.mymodid.score.TeamConfig.PALETTE.length) {
            return new float[]{1f, 1f, 1f};
        }
        int c = com.myname.mymodid.score.TeamConfig.PALETTE[color];
        return new float[]{((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f};
    }

    private int argb(int color) {
        if (color < 0 || color >= com.myname.mymodid.score.TeamConfig.PALETTE.length) {
            return 0xFFFFFFFF;
        }
        return 0xFF000000 | com.myname.mymodid.score.TeamConfig.PALETTE[color];
    }

    // v1.1.8：客户端连上服务器立即请求队伍列表（据点环/罗盘颜色需要）
    @cpw.mods.fml.common.eventhandler.SubscribeEvent
    public void onClientConnect(cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent event) {
        try {
            MyMod.NETWORK.sendToServer(new com.myname.mymodid.network.PacketTeamRequest());
        } catch (Exception e) {
            // 绝不崩游戏
        }
    }

    private Map<Long, CapturePointData.Entry> getClientPoints(Minecraft mc) {
        Map<Long, CapturePointData.Entry> pts = CapturePointData.get(mc.theWorld).getAll();
        if (!pts.isEmpty()) return pts;
        Map<Long, CapturePointData.Entry> fallback = new java.util.HashMap<Long, CapturePointData.Entry>();
        for (Object te : mc.theWorld.loadedTileEntityList) {
            if (te instanceof TileEntityCapturePoint) {
                TileEntityCapturePoint cp = (TileEntityCapturePoint) te;
                CapturePointData.Entry e = new CapturePointData.Entry();
                e.x = cp.xCoord; e.y = cp.yCoord; e.z = cp.zCoord;
                e.name = StatCollector.translateToLocal("dp.capture.point");
                e.ownerTeam = cp.getOwnerTeam();
                e.capturingTeam = cp.getCapturingTeam();
                e.progress = cp.getCaptureProgress();
                e.radius = cp.getRadius();
                fallback.put(CapturePointData.key(cp.xCoord, cp.yCoord, cp.zCoord), e);
            }
        }
        return fallback;
    }
}
