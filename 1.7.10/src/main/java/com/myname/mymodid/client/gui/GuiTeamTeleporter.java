package com.myname.mymodid.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.tileentity.TileEntity;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.inventory.ContainerBattlePortal;
import com.myname.mymodid.network.PacketTeamRequest;
import com.myname.mymodid.network.PacketTeamSelect;
import com.myname.mymodid.network.PacketTeleporterConfig;
import com.myname.mymodid.score.TeamConfig;
import com.myname.mymodid.score.TeamData;
import com.myname.mymodid.tileentity.TileEntityTeamTeleporter;

/**
 * 战场传送器 GUI（v1.1.10）：
 * 选中队伍（写入 TE NBT 保存）+ 换装/传送两个独立开关（同样保存）。
 * 红石信号激活时按开关设置生效。
 */
public class GuiTeamTeleporter extends GuiContainer {
    private int x, y, z;
    private int currentIndex = -1;
    private String selected = "";
    private boolean enableSwap = true;
    private boolean enableTp = true;

    public GuiTeamTeleporter(int x, int y, int z) {
        super(new ContainerBattlePortal());
        this.xSize = 260;
        this.ySize = 150;
        this.x = x;
        this.y = y;
        this.z = z;
        TileEntity te = Minecraft.getMinecraft().theWorld.getTileEntity(x, y, z);
        if (te instanceof TileEntityTeamTeleporter) {
            TileEntityTeamTeleporter t = (TileEntityTeamTeleporter) te;
            selected = t.selectedTeam;
            enableSwap = t.enableSwap;
            enableTp = t.enableTp;
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        int bx = guiLeft;
        int by = guiTop;
        buttonList.add(new GuiButton(0, bx + 40, by + 24, 180, 18, teamLabel()));
        buttonList.add(new GuiButton(1, bx + 40, by + 50, 180, 18, swapLabel()));
        buttonList.add(new GuiButton(2, bx + 40, by + 72, 180, 18, tpLabel()));
        MyMod.NETWORK.sendToServer(new PacketTeamRequest());
    }

    private String teamLabel() {
        TeamConfig c = TeamData.clientFind(selected);
        if (c == null) return StatCollector.translateToLocal("gui.teleporter.none");
        return String.format("\u00a7%c%s", hexChar(TeamConfig.PALETTE[c.color]),
            String.format(StatCollector.translateToLocal("gui.teleporter.select"), c.name));
    }

    private String swapLabel() {
        return StatCollector.translateToLocal(enableSwap ? "gui.teleporter.swapon" : "gui.teleporter.swapoff");
    }

    private String tpLabel() {
        return StatCollector.translateToLocal(enableTp ? "gui.teleporter.tpon" : "gui.teleporter.tpoff");
    }

    private static char hexChar(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        if (r > 180 && g < 100 && b < 100) return 'c';
        if (b > 160 && r < 120 && g < 160) return '9';
        if (g > 140 && r < 120) return 'a';
        if (r > 180 && g > 140 && b < 100) return 'e';
        return 'f';
    }

    /** PacketTeamSync 同步回来后刷新（由 PacketTeamSync.Handler 调用） */
    public void onTeamsSynced() {
        // 校正 selected 对应索引；队伍被删后回退到空
        if (TeamData.clientFind(selected) == null) selected = "";
        if (!buttonList.isEmpty()) {
            ((GuiButton) buttonList.get(0)).displayString = teamLabel();
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawRect(guiLeft - 10, guiTop - 10, guiLeft + xSize + 10, guiTop + ySize + 10, 0xC0101010);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        int bx = guiLeft;
        int by = guiTop;
        drawCenteredString(fontRendererObj, EnumChatFormatting.GOLD + StatCollector.translateToLocal("gui.teleporter.title"), bx + 130, by - 14, 0xFFFFFF);
        drawCenteredString(fontRendererObj, StatCollector.translateToLocal("gui.teleporter.hint"), bx + 130, by + 118, 0xA0A0A0);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            if (TeamData.clientTeams.isEmpty()) return;
            currentIndex = -1;
            for (int i = 0; i < TeamData.clientTeams.size(); i++) {
                if (TeamData.clientTeams.get(i).name.equals(selected)) {
                    currentIndex = i;
                    break;
                }
            }
            currentIndex = (currentIndex + 1) % TeamData.clientTeams.size();
            selected = TeamData.clientTeams.get(currentIndex).name;
            button.displayString = teamLabel();
            MyMod.NETWORK.sendToServer(new PacketTeamSelect(x, y, z, selected));
        } else if (button.id == 1) {
            enableSwap = !enableSwap;
            button.displayString = swapLabel();
            MyMod.NETWORK.sendToServer(new PacketTeleporterConfig(x, y, z, enableSwap, enableTp));
        } else if (button.id == 2) {
            enableTp = !enableTp;
            button.displayString = tpLabel();
            MyMod.NETWORK.sendToServer(new PacketTeleporterConfig(x, y, z, enableSwap, enableTp));
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
