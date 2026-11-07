package com.myname.mymodid.client.gui;

import com.myname.mymodid.network.CPayloads.BackupRestorePayload;
import com.myname.mymodid.network.CPayloads.TeamRequestPayload;
import com.myname.mymodid.network.CPayloads.TeamSelectPayload;
import com.myname.mymodid.network.CPayloads.TeleporterConfigPayload;
import com.myname.mymodid.network.Network;
import com.myname.mymodid.score.TeamConfig;
import com.myname.mymodid.score.TeamData;
import com.myname.mymodid.tileentity.TileEntityTeamTeleporter;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

public class GuiTeamTeleporter extends DpScreen {
    private final int x, y, z;
    private int currentIndex = -1;
    private String selected = "";
    private boolean enableSwap = true;
    private boolean enableTp = true;

    public GuiTeamTeleporter(net.minecraft.world.entity.player.Player player, BlockPos pos) {
        super(Component.literal("Team Teleporter"));
        this.x = pos.getX(); this.y = pos.getY(); this.z = pos.getZ();
        BlockEntity te = Minecraft.getInstance().level.getBlockEntity(pos);
        if (te instanceof TileEntityTeamTeleporter t) {
            selected = t.selectedTeam;
            enableSwap = t.enableSwap;
            enableTp = t.enableTp;
        }
    }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;
        clearButtons();
        addButton(cx - 90, cy - 60, 180, 18, this::teamLabel, () -> {
            if (TeamData.clientTeams.isEmpty()) return;
            currentIndex = -1;
            for (int i = 0; i < TeamData.clientTeams.size(); i++) {
                if (TeamData.clientTeams.get(i).name.equals(selected)) { currentIndex = i; break; }
            }
            currentIndex = (currentIndex + 1) % TeamData.clientTeams.size();
            selected = TeamData.clientTeams.get(currentIndex).name;
            Network.sendToServer(new TeamSelectPayload(x, y, z, selected));
        });
        addButton(cx - 90, cy - 34, 180, 18, this::swapLabel, () -> {
            enableSwap = !enableSwap;
            Network.sendToServer(new TeleporterConfigPayload(x, y, z, enableSwap, enableTp));
        });
        addButton(cx - 90, cy - 12, 180, 18, this::tpLabel, () -> {
            enableTp = !enableTp;
            Network.sendToServer(new TeleporterConfigPayload(x, y, z, enableSwap, enableTp));
        });
        addButton(cx - 90, cy + 14, 180, 18,
            () -> Component.translatable("gui.teleporter.backup").getString(), () -> {
            Network.sendToServer(new BackupRestorePayload());
        });
        Network.sendToServer(new TeamRequestPayload());
    }

    private String teamLabel() {
        TeamConfig c = TeamData.clientFind(selected);
        if (c == null) return Component.translatable("gui.teleporter.none").getString();
        return ChatFormatting.RESET.toString() + String.format("\u00a7%c%s", hexChar(TeamConfig.PALETTE[c.color]),
            Component.translatable("gui.teleporter.select", c.name).getString());
    }

    private String swapLabel() {
        return Component.translatable(enableSwap ? "gui.teleporter.swapon" : "gui.teleporter.swapoff").getString();
    }

    private String tpLabel() {
        return Component.translatable(enableTp ? "gui.teleporter.tpon" : "gui.teleporter.tpoff").getString();
    }

    private static char hexChar(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        if (r > 180 && g < 100 && b < 100) return 'c';
        if (b > 160 && r < 120 && g < 160) return '9';
        if (g > 140 && r < 120) return 'a';
        if (r > 180 && g > 140 && b < 100) return 'e';
        return 'f';
    }

    public void onTeamsSynced() {
        if (TeamData.clientFind(selected) == null) selected = "";
    }

    @Override
    protected void drawContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int cx = width / 2;
        String title = ChatFormatting.GOLD + Component.translatable("gui.teleporter.title").getString();
        g.drawString(font, title, cx - font.width(title) / 2, 22, 0xFFFFFF, false);
        String hint = Component.translatable("gui.teleporter.hint").getString();
        g.drawString(font, hint, cx - font.width(hint) / 2, 60, 0xA0A0A0, false);
    }
}
