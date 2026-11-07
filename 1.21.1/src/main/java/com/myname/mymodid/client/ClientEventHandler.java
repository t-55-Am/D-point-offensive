package com.myname.mymodid.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import com.myname.mymodid.MyMod;
import com.myname.mymodid.config.MyModConfig;
import com.myname.mymodid.network.CPayloads.MarkRequestPayload;
import com.myname.mymodid.network.Network;
import com.myname.mymodid.score.CapturePointData;
import com.myname.mymodid.score.ScoreData;
import com.myname.mymodid.score.TeamConfig;
import com.myname.mymodid.score.TeamData;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.platform.InputConstants;

public class ClientEventHandler {
    private static boolean lastKeyQuickComm = false;
    private static boolean lastKeyQuickCommConfig = false;
    private static boolean lastKeyChatGui = false;
    private static boolean lastKeyMark = false;
    private static boolean lastKeySlash = false;
    private static long lastMarkTime = 0;
    private static boolean hudDiagLogged = false;
    private static boolean floatingDiagLogged = false;
    private Level lastRenderLevel = null;

    public static int redScore = 10000;
    public static int blueScore = 10000;
    public static Map<String, int[]> playerStats = new java.util.HashMap<>();
    public static boolean pveEnabled = false;
    public static List<String> chatHistory = new ArrayList<>();
    public static final int MAX_CHAT_HISTORY = 100;

    public static final KeyMapping keyQuickComm =
        new KeyMapping("key.quickcomm", InputConstants.Type.KEYSYM, InputConstants.KEY_T, "key.categories.mymodid");
    public static final KeyMapping keyQuickCommConfig =
        new KeyMapping("key.quickcommconfig", InputConstants.Type.KEYSYM, InputConstants.KEY_Y, "key.categories.mymodid");
    public static final KeyMapping keyChatGui =
        new KeyMapping("key.chatgui", InputConstants.Type.KEYSYM, InputConstants.KEY_U, "key.categories.mymodid");
    public static final KeyMapping keyMark =
        new KeyMapping("key.mark", InputConstants.Type.KEYSYM, InputConstants.KEY_M, "key.categories.mymodid");

    public static final List<Mark> marks = new ArrayList<>();

    public static final Map<Long, float[]> projected = new java.util.HashMap<>();
    public static final Map<Mark, float[]> projectedMarks = new java.util.HashMap<>();

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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && name != null) {
            double dx = hx - mc.player.getX();
            double dz = hz - mc.player.getZ();
            double bearing = Math.toDegrees(Math.atan2(dx, -dz));
            if (bearing < 0) bearing += 360;
            int br = (int) Math.round(bearing);
            String msg = ChatFormatting.YELLOW + "[" + name + "] " +
                ChatFormatting.WHITE + Component.translatable("dp.mark.coord").getString() + " " + x + "," + y + "," + z +
                ChatFormatting.GOLD + "  " + Component.translatable("dp.mark.bearing").getString() + " " + br + "°";
            mc.player.displayClientMessage(Component.literal(msg), false);
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
    public void onItemTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.isEmpty()) return;
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem bi)) return;
        var b = bi.getBlock();
        if (b != MyMod.CAPTURE_POINT.get() && b != MyMod.SCORE_CONTROLLER.get() && b != MyMod.BATTLE_PORTAL.get()) return;

        var tips = event.getToolTip();
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            tips.add(Component.literal("§7═══════════════════════════════════════════════════════════"));
            if (b == MyMod.CAPTURE_POINT.get()) {
                tips.add(Component.literal("§e▼ 占点方块（核心据点）§r"));
                tips.add(Component.literal("§7● 创造模式右键打开配置界面：§r"));
                tips.add(Component.literal("§7  半径 / 占领时间 / 点位名称§r"));
                tips.add(Component.literal("§7  PvE 丧尸刷新参数（间隔/每批/上下限）§r"));
                tips.add(Component.literal("§7● 手持特殊物品右键快速切换：§r"));
                tips.add(Component.literal("§a  绿宝石§r§7=大基地开/关§r"));
                tips.add(Component.literal("§a  染料§r§7=切换占领阵营（PvE 切人类/丧尸）§r"));
                tips.add(Component.literal("§a  红石§r§7=复活模式开/关§r"));
                tips.add(Component.literal("§7● 大基地被占领时对方分数直接清零§r"));
                tips.add(Component.literal("§7● 每秒按占领差扣分，分数先归零方战败§r"));
            } else if (b == MyMod.SCORE_CONTROLLER.get()) {
                tips.add(Component.literal("§e▼ 分数控制器（对局中枢）§r"));
                tips.add(Component.literal("§7● 右键打开对局配置界面§r"));
                tips.add(Component.literal("§7  PvP：初始分数 / 每秒每点扣分 / 重置§r"));
                tips.add(Component.literal("§7  PvE：丧尸攻击力 / 速度 / 血量§r"));
                tips.add(Component.literal("§a● 必须点击「开始游戏」掉分循环才生效§r"));
                tips.add(Component.literal("§7● 可切换 PvP / PvE 丧尸模式§r"));
            } else if (b == MyMod.BATTLE_PORTAL.get()) {
                tips.add(Component.literal("§e▼ 战场入口（红蓝队传送门）§r"));
                tips.add(Component.literal("§7● 右键打开配置界面§r"));
                tips.add(Component.literal("§7  红/蓝队名称、人数上限§r"));
                tips.add(Component.literal("§7  出生点坐标、OP 预设背包模板§r"));
                tips.add(Component.literal("§7● 红石信号触发：把本队伍玩家传送到出生点§r"));
                tips.add(Component.literal("§7● 需配合计分板队伍使用（/scoreboard teams）§r"));
            }
        } else {
            tips.add(Component.literal("§7按住 §eShift§7 查看详细介绍"));
        }
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        GuiGraphics g = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        if (!hudDiagLogged) {
            hudDiagLogged = true;
            System.out.println("[DP] onRenderGui triggered: hudScoreBar=" + MyModConfig.hudScoreBar
                + " hudRadar=" + MyModConfig.hudRadar + " hudName=" + MyModConfig.hudName
                + " gameStarted=" + ScoreData.getClient().gameStarted);
        }
        checkWorldReset(mc);
        ScoreData sd = ScoreData.getClient();
        pveEnabled = sd.pveEnabled;
        if (!sd.pveEnabled && sd.gameStarted && MyModConfig.hudScoreBar) {
            renderScoreBar(mc, g);
        }
        if (MyModConfig.hudRadar) renderRadar(mc, g);
        if (isKeyDown(GLFW.GLFW_KEY_TAB)) {
            renderTabStats(mc, g);
        }
        if (MyModConfig.hudRing) {
            for (CapturePointData.Entry e : getClientPoints(mc).values()) {
                double dx = player.getX() - (e.x + 0.5);
                double dy = player.getY() - (e.y + 0.5);
                double dz = player.getZ() - (e.z + 0.5);
                if (dx * dx + dy * dy + dz * dz <= (e.radius + 1) * (e.radius + 1)) {
                    renderCaptureRingEntry(mc, g, e);
                }
            }
        }
        if (MyModConfig.hudName) {
            renderFloatingNames2D(mc, g, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
        renderMarks2D(mc, g, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        g.flush();
    }

    private void checkWorldReset(Minecraft mc) {
        Level cur = mc.level;
        if (cur != lastRenderLevel) {
            lastRenderLevel = cur;
            CapturePointData.getClient().clearAll();
        }
    }

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        checkWorldReset(mc);
        projectFrame(event);
        if (MyModConfig.hudRange) {
            Map<Long, CapturePointData.Entry> pts = getClientPoints(mc);
            for (CapturePointData.Entry e : pts.values()) {
                renderRangeCircleEntry(mc, e, event);
            }
            for (TileEntityCapturePoint cp : TileEntityCapturePoint.REGISTRY) {
                if (cp.getLevel() != mc.level) continue;
                long k = CapturePointData.key(cp.getBlockPos().getX(), cp.getBlockPos().getY(), cp.getBlockPos().getZ());
                if (pts.containsKey(k)) continue;
                renderRangeCircle(mc, cp, event);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        boolean hasGui = mc.screen != null;

        boolean mk = keyMark.isDown();
        if (!hasGui && mk && !lastKeyMark) {
            long now = System.currentTimeMillis();
            if (now - lastMarkTime < 2000) {
                mc.player.displayClientMessage(Component.literal(
                    ChatFormatting.YELLOW + Component.translatable("dp.mark.spam").getString()), false);
            } else {
                lastMarkTime = now;
                Vec3 start = mc.player.getEyePosition(1.0F);
                Vec3 look = mc.player.getLookAngle();
                Vec3 end = start.add(look.x * 2000, look.y * 2000, look.z * 2000);

                BlockHitResult blockHit = mc.level.clip(new ClipContext(
                    start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
                double blockDist = (blockHit == null || blockHit.getType() == HitResult.Type.MISS)
                    ? Double.MAX_VALUE : blockHit.getLocation().distanceTo(start);

                EntityHitResult entityHit = null;
                double entityDist = Double.MAX_VALUE;
                AABB rayBox = new AABB(start, end).inflate(1.0, 1.0, 1.0);
                List<Entity> entities = mc.level.getEntities(mc.player, rayBox,
                    e -> e.canBeCollidedWith());
                for (Entity o : entities) {
                    Vec3 hit = o.getBoundingBox().clip(start, end).orElse(null);
                    if (hit != null) {
                        double d = hit.distanceTo(start);
                        if (d < entityDist) { entityDist = d; entityHit = new EntityHitResult(o, hit); }
                    }
                }
                HitResult finalHit = (blockDist <= entityDist) ? blockHit : entityHit;
                if (finalHit != null && finalHit.getType() != HitResult.Type.MISS) {
                    int bx, by, bz;
                    if (finalHit.getType() == HitResult.Type.BLOCK) {
                        BlockPos bp = ((BlockHitResult) finalHit).getBlockPos();
                        bx = bp.getX(); by = bp.getY(); bz = bp.getZ();
                    } else {
                        Entity e = ((EntityHitResult) finalHit).getEntity();
                        bx = (int) Math.floor(e.getX());
                        by = (int) Math.floor(e.getY());
                        bz = (int) Math.floor(e.getZ());
                    }
                    Network.sendToServer(new MarkRequestPayload(bx, by, bz,
                        (float) finalHit.getLocation().x,
                        (float) finalHit.getLocation().y,
                        (float) finalHit.getLocation().z));
                }
            }
        }
        lastKeyMark = mk;

        boolean slash = isKeyDown(GLFW.GLFW_KEY_SLASH);
        if (slash && !lastKeySlash && mc.screen == null) {
            mc.setScreen(new ChatScreen("/"));
        }
        lastKeySlash = slash;

        boolean qc = keyQuickComm.isDown();
        boolean qcc = keyQuickCommConfig.isDown();
        boolean cg = keyChatGui.isDown();
        if (!hasGui) {
            if (qc && !lastKeyQuickComm) mc.setScreen(new com.myname.mymodid.client.gui.QuickCommGui());
            if (qcc && !lastKeyQuickCommConfig) mc.setScreen(new com.myname.mymodid.client.gui.QuickCommConfigGui());
            if (cg && !lastKeyChatGui) mc.setScreen(new com.myname.mymodid.client.gui.ChatGui());
        }
        lastKeyQuickComm = qc;
        lastKeyQuickCommConfig = qcc;
        lastKeyChatGui = cg;
    }

    private void renderScoreBar(Minecraft mc, GuiGraphics g) {
        ScoreData sd = ScoreData.getClient();
        if (sd != null && sd.mode == 1) {
            renderMultiScore(mc, g, sd);
            return;
        }
        int barWidth = Math.max(40, com.myname.mymodid.config.MyModConfig.scoreBarWidth);
        int barHeight = 12;
        int x = (g.guiWidth() - barWidth) / 2 + com.myname.mymodid.config.MyModConfig.scoreBarX;
        int y = com.myname.mymodid.config.MyModConfig.scoreBarY;
        g.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, 0x90000000);
        int half = barWidth / 2;
        int redW = (int) ((long) redScore * half / 10000);
        int blueW = (int) ((long) blueScore * half / 10000);
        redW = Math.max(0, Math.min(half, redW));
        blueW = Math.max(0, Math.min(half, blueW));
        g.fill(x, y, x + half, y + barHeight, 0x40FF0000);
        g.fill(x, y, x + redW, y + barHeight, 0xFFFF0000);
        g.fill(x + half, y, x + barWidth, y + barHeight, 0x400000FF);
        g.fill(x + barWidth - blueW, y, x + barWidth, y + barHeight, 0xFF0000FF);
        g.fill(x + half - 1, y - 2, x + half + 1, y + barHeight + 2, 0xFFFFFFFF);
        g.drawString(mc.font, String.valueOf(redScore), x + 5, y + 2, 0xFFFFFF, true);
        String bs = String.valueOf(blueScore);
        g.drawString(mc.font, bs, x + barWidth - mc.font.width(bs) - 5, y + 2, 0xFFFFFF, true);
    }

    private void renderMultiScore(Minecraft mc, GuiGraphics g, ScoreData sd) {
        if (sd.teamScores == null || sd.teamScores.isEmpty()) return;
        int w = Math.max(40, com.myname.mymodid.config.MyModConfig.scoreBarWidth);
        int rowH = 12;
        java.util.List<java.util.Map.Entry<String, Integer>> list =
            new java.util.ArrayList<java.util.Map.Entry<String, Integer>>(sd.teamScores.entrySet());
        java.util.Collections.sort(list, (a, b) -> {
            int cmp = (b.getValue() == null ? 0 : b.getValue()) - (a.getValue() == null ? 0 : a.getValue());
            if (cmp != 0) return cmp;
            return a.getKey().compareTo(b.getKey());
        });
        int rows = list.size();
        int h = rows * rowH + 6;
        int x = (g.guiWidth() - w) / 2 + com.myname.mymodid.config.MyModConfig.scoreBarX;
        int y = com.myname.mymodid.config.MyModConfig.scoreBarY;
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0x90000000);
        for (int i = 0; i < rows; i++) {
            java.util.Map.Entry<String, Integer> e = list.get(i);
            int col = teamColor(e.getKey(), null);
            g.drawString(mc.font, e.getKey(), x + 4, y + 3 + i * rowH, col, true);
            String score = String.valueOf(e.getValue());
            g.drawString(mc.font, score, x + w - 4 - mc.font.width(score), y + 3 + i * rowH, 0xFFFFFF, true);
        }
    }

    private void renderTabStats(Minecraft mc, GuiGraphics g) {
        if (mc.level == null) return;
        var fr = mc.font;
        int pw = g.guiWidth();
        int panelY = g.guiHeight() / 6;
        int rowH = 14;
        List<String> allNames = new ArrayList<>();
        for (Player p : mc.level.players()) {
            String n = p.getScoreboardName();
            if (!allNames.contains(n)) allNames.add(n);
        }
        for (String n : playerStats.keySet()) {
            if (!allNames.contains(n)) allNames.add(n);
        }
        Comparator<String> byKill = (a, b) -> {
            int[] sa = playerStats.get(a);
            int[] sb2 = playerStats.get(b);
            return (sb2 == null ? 0 : sb2[0]) - (sa == null ? 0 : sa[0]);
        };
        allNames.sort(byKill);
        if (pveEnabled) {
            int panelW = 300;
            int panelX = (pw - panelW) / 2;
            int h = 18 + allNames.size() * rowH;
            g.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + h + 2, 0xFF000000);
            g.fill(panelX, panelY, panelX + panelW, panelY + h, 0xB0000000);
            g.drawString(fr, Component.translatable("dp.tab.name"), panelX + 4, panelY + 4, 0xFFFF55, true);
            g.drawString(fr, Component.translatable("dp.tab.kill"), panelX + 90, panelY + 4, 0x55FF55, true);
            g.drawString(fr, Component.translatable("dp.tab.assist"), panelX + 130, panelY + 4, 0xFFFF55, true);
            g.drawString(fr, Component.translatable("dp.tab.capture"), panelX + 170, panelY + 4, 0x55FFFF, true);
            g.drawString(fr, Component.translatable("dp.tab.death"), panelX + 210, panelY + 4, 0xFF5555, true);
            int yy = panelY + 18;
            for (String name : allNames) {
                int[] s = playerStats.get(name);
                if (s == null) s = new int[]{0, 0, 0, 0};
                g.drawString(fr, name, panelX + 4, yy, nameColor(mc, name, null), true);
                g.drawString(fr, String.valueOf(s[0]), panelX + 90, yy, 0x55FF55, true);
                g.drawString(fr, String.valueOf(s[1]), panelX + 130, yy, 0xFFFF55, true);
                g.drawString(fr, String.valueOf(s[2]), panelX + 170, yy, 0x55FFFF, true);
                g.drawString(fr, String.valueOf(s[3]), panelX + 210, yy, 0xFF5555, true);
                yy += rowH;
            }
        } else {
            var sb = mc.level.getScoreboard();
            List<String> reds = new ArrayList<>();
            List<String> blues = new ArrayList<>();
            List<String> others = new ArrayList<>();
            for (String name : allNames) {
                PlayerTeam t = sb.getPlayersTeam(name);
                String tn = (t == null) ? "" : t.getName();
                String lower = tn.toLowerCase();
                if (lower.contains("red") || tn.contains("红")) reds.add(name);
                else if (lower.contains("blue") || tn.contains("蓝")) blues.add(name);
                else others.add(name);
            }
            reds.sort(byKill);
            blues.sort(byKill);
            others.sort(byKill);
            int panelW = 420;
            int panelX = (pw - panelW) / 2;
            int midX = panelX + 210;
            int teamRows = Math.max(reds.size(), blues.size());
            int teamH = 18 + teamRows * rowH;
            int otherH = others.isEmpty() ? 0 : 18 + others.size() * rowH;
            int totalH = teamH + (others.isEmpty() ? 0 : 4 + otherH);
            g.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + totalH + 2, 0xFF000000);
            g.fill(panelX, panelY, panelX + panelW, panelY + totalH, 0xB0000000);
            g.fill(midX, panelY, midX + 1, panelY + teamH, 0xFFFFFFFF);
            g.drawString(fr, Component.translatable("dp.tab.redteam"), panelX + 4, panelY + 4, 0xFF5555, true);
            g.drawString(fr, Component.translatable("dp.tab.kill"), panelX + 60, panelY + 4, 0x55FF55, true);
            g.drawString(fr, Component.translatable("dp.tab.assist"), panelX + 95, panelY + 4, 0xFFFF55, true);
            g.drawString(fr, Component.translatable("dp.tab.capture"), panelX + 130, panelY + 4, 0x55FFFF, true);
            g.drawString(fr, Component.translatable("dp.tab.death"), panelX + 165, panelY + 4, 0xFF5555, true);
            g.drawString(fr, Component.translatable("dp.tab.blueteam"), midX + 4, panelY + 4, 0x5555FF, true);
            g.drawString(fr, Component.translatable("dp.tab.kill"), midX + 60, panelY + 4, 0x55FF55, true);
            g.drawString(fr, Component.translatable("dp.tab.assist"), midX + 95, panelY + 4, 0xFFFF55, true);
            g.drawString(fr, Component.translatable("dp.tab.capture"), midX + 130, panelY + 4, 0x55FFFF, true);
            g.drawString(fr, Component.translatable("dp.tab.death"), midX + 165, panelY + 4, 0xFF5555, true);
            int yy = panelY + 18;
            for (int i = 0; i < teamRows; i++) {
                if (i < reds.size()) {
                    String name = reds.get(i);
                    int[] s = playerStats.get(name);
                    if (s == null) s = new int[]{0, 0, 0, 0};
                    g.drawString(fr, name, panelX + 4, yy, nameColor(mc, name, "red"), true);
                    g.drawString(fr, String.valueOf(s[0]), panelX + 60, yy, 0x55FF55, true);
                    g.drawString(fr, String.valueOf(s[1]), panelX + 95, yy, 0xFFFF55, true);
                    g.drawString(fr, String.valueOf(s[2]), panelX + 130, yy, 0x55FFFF, true);
                    g.drawString(fr, String.valueOf(s[3]), panelX + 165, yy, 0xFF5555, true);
                }
                if (i < blues.size()) {
                    String name = blues.get(i);
                    int[] s = playerStats.get(name);
                    if (s == null) s = new int[]{0, 0, 0, 0};
                    g.drawString(fr, name, midX + 4, yy, nameColor(mc, name, "blue"), true);
                    g.drawString(fr, String.valueOf(s[0]), midX + 60, yy, 0x55FF55, true);
                    g.drawString(fr, String.valueOf(s[1]), midX + 95, yy, 0xFFFF55, true);
                    g.drawString(fr, String.valueOf(s[2]), midX + 130, yy, 0x55FFFF, true);
                    g.drawString(fr, String.valueOf(s[3]), midX + 165, yy, 0xFF5555, true);
                }
                yy += rowH;
            }
            if (!others.isEmpty()) {
                int sepY = panelY + teamH + 2;
                g.fill(panelX, sepY, panelX + panelW, sepY + 1, 0xFFFFFFFF);
                int otherY = sepY + 4;
                g.drawString(fr, Component.translatable("dp.tab.noteam"), panelX + 4, otherY, 0xAAAAAA, true);
                int oy = otherY + 18;
                for (String name : others) {
                    g.drawString(fr, name, panelX + 4, oy, nameColor(mc, name, null), true);
                    oy += rowH;
                }
            }
        }
    }

    private int nameColor(Minecraft mc, String name, String forceTeam) {
        Player ep = null;
        if (mc.level != null) {
            for (Player p : mc.level.players()) {
                if (p.getScoreboardName().equals(name)) { ep = p; break; }
            }
        }
        if (ep == null || !ep.isAlive()) return 0xAAAAAA;
        if ("red".equals(forceTeam)) return 0xFF5555;
        if ("blue".equals(forceTeam)) return 0x5555FF;
        PlayerTeam t = mc.level.getScoreboard().getPlayersTeam(name);
        if (t != null) {
            TeamConfig cfg = TeamData.clientFind(t.getName());
            if (cfg != null) {
                int c = cfg.color;
                if (c >= 0 && c < TeamConfig.PALETTE.length) {
                    return 0xFF000000 | TeamConfig.PALETTE[c];
                }
            }
        }
        return 0xFFFFFF;
    }

    private void renderRadar(Minecraft mc, GuiGraphics g) {
        Player p = mc.player;
        if (p == null) return;
        Map<Long, CapturePointData.Entry> pts = getClientPoints(mc);
        int barH = 12;
        int barY = com.myname.mymodid.config.MyModConfig.radarY;
        int barW = com.myname.mymodid.config.MyModConfig.radarWidth > 0
            ? com.myname.mymodid.config.MyModConfig.radarWidth : g.guiWidth() / 2;
        int barX = (g.guiWidth() - barW) / 2;
        int mid = barX + barW / 2;
        g.fill(barX, barY, barX + barW, barY + barH, 0x90000000);
        float front = norm(p.getYRot() + 180f);
        int startDeg = ((int) front - 90) / 10 * 10;
        for (int a = startDeg; a <= front + 90; a += 10) {
            float aa = norm(a);
            float rel = norm(aa - front);
            if (rel > 180) rel -= 360;
            if (rel < -90 || rel > 90) continue;
            int x = mid + (int) (rel / 90.0 * (barW / 2.0));
            int mod = ((int) (aa % 360 + 360)) % 360;
            if (mod == 0 || mod == 90 || mod == 180 || mod == 270) {
                g.fill(x, barY - 4, x + 1, barY + barH + 4, 0xFFFFFFFF);
                String label = mod == 0 ? "N" : mod == 90 ? "E" : mod == 180 ? "S" : "W";
                g.drawString(mc.font, label, x - mc.font.width(label) / 2, barY + barH + 2, 0xFFFFFF, true);
            } else {
                g.fill(x, barY - 2, x + 1, barY + barH + 2, 0x88FFFFFF);
                String dl = String.valueOf(mod);
                g.drawString(mc.font, dl, x - mc.font.width(dl) / 2, barY + barH + 2, 0xAAAAAA, true);
            }
        }
        g.fill(mid - 2, barY - 3, mid + 2, barY + barH + 3, 0xFFFFFFFF);
        for (CapturePointData.Entry e : pts.values()) {
            double dx = e.x + 0.5 - p.getX();
            double dz = e.z + 0.5 - p.getZ();
            double bearing = Math.toDegrees(Math.atan2(dx, -dz));
            float rel = norm((float) bearing - front);
            if (rel < -90 || rel > 90) continue;
            int x = mid + (int) (rel / 90.0 * (barW / 2.0));
            int color = argb(teamColor(e.ownerTeam, e.capturingTeam));
            BlockEntity te = mc.level.getBlockEntity(new BlockPos(e.x, e.y, e.z));
            boolean isMain = (te instanceof TileEntityCapturePoint) && ((TileEntityCapturePoint) te).isMainBase();
            if (isMain) {
                g.fill(x - 5, barY, x + 5, barY + barH, 0xFFFFFF00);
                g.fill(x - 4, barY + 1, x + 4, barY + barH - 1, color);
            } else {
                g.fill(x - 3, barY + 1, x + 3, barY + barH - 1, color);
            }
            String nm = (e.name == null || e.name.isEmpty())
                ? Component.translatable("dp.capture.point").getString() : e.name;
            g.drawString(mc.font, nm, x - mc.font.width(nm) / 2, barY + barH + 4, color, true);
        }
    }

    private void renderCaptureRingEntry(Minecraft mc, GuiGraphics g, CapturePointData.Entry e) {
        int cx = g.guiWidth() / 2;
        int cy = g.guiHeight() / 2 + 70;
        int outerR = 14, innerR = 10;
        double progress = Math.max(0, Math.min(1, e.progress / 100.0));
        float[] c = rgb(teamColor(e.ownerTeam, e.capturingTeam));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        drawRingStrip(g, cx, cy, outerR, innerR, 0, 2 * Math.PI, 0.1f, 0.1f, 0.1f, 0.8f);
        if (progress > 0.001) {
            drawRingStrip(g, cx, cy, outerR, innerR, -Math.PI / 2,
                -Math.PI / 2 + 2 * Math.PI * progress, c[0], c[1], c[2], 1f);
        }
        RenderSystem.disableBlend();
        String capturing = e.capturingTeam;
        String owner = e.ownerTeam;
        String pct = (int) (progress * 100) + "%";
        if (capturing != null && !capturing.isEmpty()) {
            g.drawString(mc.font, pct, cx - mc.font.width(pct) / 2, cy - 4, 0xFFFFFF, true);
            g.drawString(mc.font, capturing, cx - mc.font.width(capturing) / 2, cy + 8, argb(teamColor(owner, capturing)), true);
        } else if (owner != null && !owner.isEmpty()) {
            g.drawString(mc.font, owner, cx - mc.font.width(owner) / 2, cy - 3, argb(teamColor(owner, capturing)), true);
        } else {
            g.drawString(mc.font, pct, cx - mc.font.width(pct) / 2, cy - 4, 0xFFFFFF, true);
        }
    }

    private void drawRingStrip(GuiGraphics g, int cx, int cy, int outerR, int innerR,
                               double startA, double endA, float r, float gr, float b, float a) {
        int segs = Math.max(4, (int) Math.ceil(Math.abs(endA - startA) / (2 * Math.PI) * 48));
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        org.joml.Matrix4f pose = g.pose().last().pose();
        int ri = (int) (r * 255), gi = (int) (gr * 255), bi = (int) (b * 255), ai = (int) (a * 255);
        for (int i = 0; i < segs; i++) {
            double a0 = startA + (endA - startA) * i / segs;
            double a1 = startA + (endA - startA) * (i + 1) / segs;
            vc.addVertex(pose, (float) (cx + Math.cos(a0) * outerR), (float) (cy + Math.sin(a0) * outerR), 0).setColor(ri, gi, bi, ai);
            vc.addVertex(pose, (float) (cx + Math.cos(a0) * innerR), (float) (cy + Math.sin(a0) * innerR), 0).setColor(ri, gi, bi, ai);
            vc.addVertex(pose, (float) (cx + Math.cos(a1) * innerR), (float) (cy + Math.sin(a1) * innerR), 0).setColor(ri, gi, bi, ai);
            vc.addVertex(pose, (float) (cx + Math.cos(a1) * outerR), (float) (cy + Math.sin(a1) * outerR), 0).setColor(ri, gi, bi, ai);
        }
    }

    private void addRingBand(BufferBuilder bb, double cx, double cy, double cz, double ro, double ri,
                             float r, float gr, float b, float a) {
        for (int i = 0; i <= 64; i++) {
            double ang = Math.PI * 2 * i / 64;
            bb.addVertex((float) (cx + Math.cos(ang) * ro), (float) cy, (float) (cz + Math.sin(ang) * ro))
                .setColor((int) (r * 255), (int) (gr * 255), (int) (b * 255), (int) (a * 255));
            bb.addVertex((float) (cx + Math.cos(ang) * ri), (float) cy, (float) (cz + Math.sin(ang) * ri))
                .setColor((int) (r * 255), (int) (gr * 255), (int) (b * 255), (int) (a * 255));
        }
    }

    private void renderRangeCircle(Minecraft mc, TileEntityCapturePoint point, RenderLevelStageEvent event) {
        Player p = mc.player;
        if (p == null) return;
        BlockPos pos = point.getBlockPos();
        double ddx = pos.getX() + 0.5 - p.getX();
        double ddz = pos.getZ() + 0.5 - p.getZ();
        if (ddx * ddx + ddz * ddz > 64 * 64) return;
        Camera cam = event.getCamera();
        Vec3 camPos = cam.getPosition();
        String owner = point.getOwnerTeam();
        String capturing = point.getCapturingTeam();
        for (CapturePointData.Entry e : getClientPoints(mc).values()) {
            if (e.x == pos.getX() && e.y == pos.getY() && e.z == pos.getZ()) {
                if (e.ownerTeam != null && !e.ownerTeam.isEmpty()) owner = e.ownerTeam;
                if (e.capturingTeam != null && !e.capturingTeam.isEmpty()) capturing = e.capturingTeam;
                break;
            }
        }
        float[] c = rgb(teamColor(owner, capturing));
        boolean isMain = point.isMainBase();
        int r = point.getRadius();
        double cx = pos.getX() + 0.5 - camPos.x;
        double cy = pos.getY() + 0.06 - camPos.y;
        double cz = pos.getZ() + 0.5 - camPos.z;

        var pose = event.getPoseStack();
        pose.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        if (isMain) {
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            addRingBand(bb, cx, cy, cz, r + 2 + 0.2, r + 2 - 0.2, 1f, 0.84f, 0f, 0.95f);
            BufferUploader.drawWithShader(bb.build());
        }
        {
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            addRingBand(bb, cx, cy, cz, r + 0.2, r - 0.2, c[0], c[1], c[2], 0.9f);
            BufferUploader.drawWithShader(bb.build());
        }
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        pose.popPose();
    }

    private void renderRangeCircleEntry(Minecraft mc, CapturePointData.Entry e, RenderLevelStageEvent event) {
        Player p = mc.player;
        if (p == null) return;
        double ddx = e.x + 0.5 - p.getX();
        double ddz = e.z + 0.5 - p.getZ();
        if (ddx * ddx + ddz * ddz > 64 * 64) return;
        Camera cam = event.getCamera();
        Vec3 camPos = cam.getPosition();
        float[] c = rgb(teamColor(e.ownerTeam, e.capturingTeam));
        boolean isMain = false;
        BlockEntity te = mc.level.getBlockEntity(new BlockPos(e.x, e.y, e.z));
        if (te instanceof TileEntityCapturePoint tcp) isMain = tcp.isMainBase();
        int r = Math.max(1, e.radius);
        double cx = e.x + 0.5 - camPos.x;
        double cy = e.y + 0.06 - camPos.y;
        double cz = e.z + 0.5 - camPos.z;

        var pose = event.getPoseStack();
        pose.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        if (isMain) {
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            addRingBand(bb, cx, cy, cz, r + 2 + 0.2, r + 2 - 0.2, 1f, 0.84f, 0f, 0.95f);
            BufferUploader.drawWithShader(bb.build());
        }
        {
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            addRingBand(bb, cx, cy, cz, r + 0.2, r - 0.2, c[0], c[1], c[2], 0.9f);
            BufferUploader.drawWithShader(bb.build());
        }
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        pose.popPose();
    }

    private void renderFloatingNames2D(Minecraft mc, GuiGraphics g, float partialTick) {
        if (mc.player == null || mc.level == null) return;
        if (!floatingDiagLogged) {
            floatingDiagLogged = true;
            System.out.println("[DP] floating names: entries=" + getClientPoints(mc).size()
                + " hudName=" + MyModConfig.hudName);
        }
        for (CapturePointData.Entry e : getClientPoints(mc).values()) {
            renderFloatingName2D(mc, g, e, partialTick);
        }
    }

    private void renderFloatingName2D(Minecraft mc, GuiGraphics g, CapturePointData.Entry e, float partialTick) {
        double wx = e.x + 0.5, wy = e.y + 3.5, wz = e.z + 0.5;
        Player p0 = mc.player;
        if (p0 != null) {
            Vec3 to = new Vec3(wx - p0.getX(), wy - (p0.getY() + p0.getEyeHeight()), wz - p0.getZ()).normalize();
            Vec3 look = p0.getLookAngle();
            if (to.dot(look) < 0.1) return;
        }
        float[] pr = projected.get(BlockPos.asLong(e.x, e.y, e.z));
        if (pr == null) return;
        float sx = pr[0], sy = pr[1];
        String name = (e.name == null || e.name.isEmpty())
            ? Component.translatable("dp.capture.point").getString() : e.name;
        double progress = Math.max(0, Math.min(1, e.progress / 100.0));
        BlockEntity te = mc.level.getBlockEntity(new BlockPos(e.x, e.y, e.z));
        boolean isMain = (te instanceof TileEntityCapturePoint) && ((TileEntityCapturePoint) te).isMainBase();
        String owner = e.ownerTeam;
        String capturing = e.capturingTeam;
        if (te instanceof TileEntityCapturePoint cp) {
            if (owner == null || owner.isEmpty()) owner = cp.getOwnerTeam();
            if (capturing == null || capturing.isEmpty()) capturing = cp.getCapturingTeam();
        }
        int color = argb(teamColor(owner, capturing));
        float[] c = rgb(teamColor(owner, capturing));
        int nameW = mc.font.width(name);
        int outerR = Math.max(10, nameW / 2 + 8);
        int innerR = Math.max(6, outerR - 5);
        double dist = 30.0;
        if (p0 != null) {
            double dx = wx - p0.getX(), dy = wy - (p0.getY() + p0.getEyeHeight()), dz = wz - p0.getZ();
            dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        double maxDist = mc.options.getEffectiveRenderDistance() * 16.0;
        if (dist > maxDist) return;
        float fade = 1.0f;
        double fadeStart = maxDist * 0.35;
        if (dist > fadeStart) fade = (float) Math.max(0.0, 1.0 - (dist - fadeStart) / (maxDist - fadeStart));
        double s = MyModConfig.hudNameScale;

        g.pose().pushPose();
        g.pose().translate(sx, sy, 0);
        g.pose().scale((float) s, (float) s, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (isMain) {
            drawRingStrip(g, 0, 0, outerR + 3, outerR + 1, 0, 2 * Math.PI, 1f, 0.84f, 0f, 0.9f * fade);
        }
        drawRingStrip(g, 0, 0, outerR, innerR, 0, 2 * Math.PI, c[0], c[1], c[2], 0.3f * fade);
        if (progress > 0.001) {
            drawRingStrip(g, 0, 0, outerR, innerR,
                -Math.PI / 2, -Math.PI / 2 + 2 * Math.PI * progress, c[0], c[1], c[2], 1f * fade);
        }
        RenderSystem.disableBlend();
        int textColor = ((int) (fade * 255.0f) << 24) | 0xFFFFFF;
        g.drawString(mc.font, name, -nameW / 2, -4, textColor, true);
        g.pose().popPose();
    }

    private void renderMarks2D(Minecraft mc, GuiGraphics g, float partialTick) {
        if (marks.isEmpty()) return;
        long now = System.currentTimeMillis();
        Iterator<Mark> it = marks.iterator();
        while (it.hasNext()) {
            Mark m = it.next();
            if (now - m.createdAt > 5000) { it.remove(); continue; }
            float[] pr = projectedMarks.get(m);
            if (pr == null) continue;
            float sx = pr[0], sy = pr[1];
            int s = 14;
            drawTriangle(g, (int) sx, (int) sy, s, 0.3f, 1.0f, 0.0f, 0.0f);
            drawTriangle(g, (int) sx, (int) sy, s - 4, 1.0f, 1.0f, 0.0f, 0.0f);
            if (m.playerName != null) {
                int w = mc.font.width(m.playerName);
                g.drawString(mc.font, m.playerName, (int) sx - w / 2, (int) sy + s + 8, 0xFFFF5555, true);
            }
        }
    }

    private void drawTriangle(GuiGraphics g, int cx, int cy, int size, float a, float r, float gr, float b) {
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        org.joml.Matrix4f pose = g.pose().last().pose();
        int ai = (int) (a * 255), ri = (int) (r * 255), gi = (int) (gr * 255), bi = (int) (b * 255);
        vc.addVertex(pose, cx - size, cy - size + 4, 0).setColor(ri, gi, bi, ai);
        vc.addVertex(pose, cx + size, cy - size + 4, 0).setColor(ri, gi, bi, ai);
        vc.addVertex(pose, cx, cy + size, 0).setColor(ri, gi, bi, ai);
        vc.addVertex(pose, cx, cy + size, 0).setColor(ri, gi, bi, ai);
    }

    private void projectFrame(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        projected.clear();
        projectedMarks.clear();
        Matrix4f projM = event.getProjectionMatrix();
        Matrix4f modelM = event.getModelViewMatrix();
        Vec3 camPos = event.getCamera().getPosition();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        for (CapturePointData.Entry e : getClientPoints(mc).values()) {
            float[] s = worldToScreen(e.x + 0.5, e.y + 3.5, e.z + 0.5, camPos, modelM, projM, sw, sh);
            if (s != null) projected.put(BlockPos.asLong(e.x, e.y, e.z), s);
        }
        synchronized (marks) {
            for (Mark m : marks) {
                float[] s = worldToScreen(m.hx, m.hy, m.hz, camPos, modelM, projM, sw, sh);
                if (s != null) projectedMarks.put(m, s);
            }
        }
    }

    private float[] worldToScreen(double wx, double wy, double wz, Vec3 camPos, Matrix4f modelM, Matrix4f projM, int sw, int sh) {
        org.joml.Vector4f v = new org.joml.Vector4f(
            (float) (wx - camPos.x), (float) (wy - camPos.y), (float) (wz - camPos.z), 1.0f);
        v.mul(modelM);
        v.mul(projM);
        if (v.w() <= 0.05f) return null;
        float ndcX = v.x() / v.w();
        float ndcY = v.y() / v.w();
        return new float[]{ (float) ((ndcX * 0.5f + 0.5f) * sw), (float) ((0.5f - ndcY * 0.5f) * sh) };
    }

    private float[] project(Minecraft mc, double wx, double wy, double wz, float partialTick) {
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition();
        Vec3 rel = new Vec3(wx - camPos.x, wy - camPos.y, wz - camPos.z);
        org.joml.Vector3f look = cam.getLookVector();
        org.joml.Vector3f up = cam.getUpVector();
        double ll = Math.sqrt(look.x() * (double) look.x() + look.y() * (double) look.y() + look.z() * (double) look.z());
        if (ll < 1e-6) return null;
        org.joml.Vector3f fwd = new org.joml.Vector3f(look).mul((float) (1.0 / ll));
        org.joml.Vector3f upN = new org.joml.Vector3f(up).normalize();
        org.joml.Vector3f right = new org.joml.Vector3f(fwd).cross(upN, new org.joml.Vector3f()).normalize();
        org.joml.Vector3f upv = new org.joml.Vector3f(right).cross(fwd, new org.joml.Vector3f()).normalize();
        double z = rel.x * fwd.x() + rel.y * fwd.y() + rel.z * fwd.z();
        if (z <= 0.01) return null;
        double x = rel.x * right.x() + rel.y * right.y() + rel.z * right.z();
        double y = rel.x * upv.x() + rel.y * upv.y() + rel.z * upv.z();
        double fov = mc.options.fov().get();
        double mod = 1.0;
        Player p = mc.player;
        if (p != null) {
            if (p.isSprinting()) fov += 10.0;
            if (p.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                int lvl = p.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 1;
                mod *= 1.0 + 0.1 * lvl;
            }
            if (p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                int lvl = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() + 1;
                mod *= 1.0 - 0.1 * lvl;
            }
        }
        fov *= mod;
        double halfH = Math.tan(Math.toRadians(fov) / 2.0) * z;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        float sx = (float) (sw / 2.0 + x / halfH * (sh / 2.0));
        float sy = (float) (sh / 2.0 - y / halfH * (sh / 2.0));
        return new float[]{sx, sy};
    }

    private boolean isKeyDown(int glfwKey) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), glfwKey);
    }

    private float norm(float a) {
        while (a > 180) a -= 360;
        while (a < -180) a += 360;
        return a;
    }

    private int teamColor(String owner, String capturing) {
        String t = (capturing != null && !capturing.isEmpty()) ? capturing : owner;
        if (t == null || t.isEmpty()) return 0xFF000000 | TeamConfig.PALETTE[TeamConfig.PALETTE.length - 1];
        TeamConfig cfg = TeamData.clientFind(t);
        if (cfg != null) {
            int c = cfg.color;
            if (c < 0 || c >= TeamConfig.PALETTE.length) c = TeamConfig.PALETTE.length - 1;
            return 0xFF000000 | TeamConfig.PALETTE[c];
        }
        return 0xFF000000 | TeamConfig.PALETTE[TeamConfig.PALETTE.length - 1];
    }

    private float[] rgb(int color) {
        return new float[]{((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f};
    }

    private int argb(int color) {
        return color;
    }

    private Map<Long, CapturePointData.Entry> getClientPoints(Minecraft mc) {
        Map<Long, CapturePointData.Entry> pts = CapturePointData.getClient().getAll();
        if (!pts.isEmpty()) return pts;
        Map<Long, CapturePointData.Entry> fallback = new java.util.HashMap<>();
        for (TileEntityCapturePoint cp : TileEntityCapturePoint.REGISTRY) {
            if (cp.getLevel() != mc.level) continue;
            CapturePointData.Entry e = new CapturePointData.Entry();
            e.x = cp.getBlockPos().getX();
            e.y = cp.getBlockPos().getY();
            e.z = cp.getBlockPos().getZ();
            e.name = Component.translatable("dp.capture.point").getString();
            e.ownerTeam = cp.getOwnerTeam();
            e.capturingTeam = cp.getCapturingTeam();
            e.progress = cp.getCaptureProgress();
            e.radius = cp.getRadius();
            fallback.put(CapturePointData.key(e.x, e.y, e.z), e);
        }
        return fallback;
    }
}
