package com.myname.mymodid.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.inventory.ContainerBattlePortal;
import com.myname.mymodid.network.PacketBattleConfig;
import com.myname.mymodid.network.PacketSaveBackpack;
import com.myname.mymodid.network.PacketTeamRequest;
import com.myname.mymodid.score.TeamConfig;
import com.myname.mymodid.score.TeamData;

/**
 * 队伍配置器 GUI（1.7.10 v1.1.9 布局同步）：所有元素以背景框(guiLeft/guiTop)对齐，避免错乱。
 * 管理全局队伍列表（≤10，默认 2 队）：切换队伍、改名、颜色、阵营、人数上限、传送点、背包模板。
 */
public class GuiBattlePortal extends GuiContainer {
    private int x, y, z;
    private int currentIndex = 0;
    private String editName = "";
    private int editColor = 0;
    private int editSide = 0;
    private int editMax = 10;
    private int[] editSpawn = {0, 64, 0};

    private GuiTextField nameField;
    private GuiTextField maxField;
    private GuiTextField spawnXField;
    private GuiTextField spawnYField;
    private GuiTextField spawnZField;

    public GuiBattlePortal(int x, int y, int z) {
        super(new ContainerBattlePortal());
        this.xSize = 320;
        this.ySize = 190;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        locateCurrent();
        int bx = guiLeft;
        int by = guiTop;
        nameField = new GuiTextField(0, fontRenderer, bx + 80, by + 46, 92, 16);
        nameField.setMaxStringLength(16);
        maxField = new GuiTextField(1, fontRenderer, bx + 240, by + 46, 64, 16);
        maxField.setMaxStringLength(3);
        spawnXField = new GuiTextField(2, fontRenderer, bx + 80, by + 68, 92, 16);
        spawnXField.setMaxStringLength(6);
        spawnYField = new GuiTextField(3, fontRenderer, bx + 80, by + 96, 92, 16);
        spawnYField.setMaxStringLength(6);
        spawnZField = new GuiTextField(4, fontRenderer, bx + 80, by + 124, 92, 16);
        spawnZField.setMaxStringLength(6);
        buttonList.add(new GuiButton(0, bx + 80, by + 4, 160, 18, teamLabel()));
        buttonList.add(new GuiButton(1, bx + 16, by + 26, 288, 18, colorLabel()));
        buttonList.add(new GuiButton(3, bx + 16, by + 146, 140, 18, I18n.format("gui.battle.saveop")));
        buttonList.add(new GuiButton(4, bx + 164, by + 146, 140, 18, I18n.format("gui.battle.save")));
        buttonList.add(new GuiButton(5, bx + 16, by + 168, 140, 18, I18n.format("gui.battle.add")));
        buttonList.add(new GuiButton(6, bx + 164, by + 168, 140, 18, I18n.format("gui.battle.remove")));
        refreshFields();
        MyMod.NETWORK.sendToServer(new PacketTeamRequest());
    }

    /** 定位当前编辑队伍：editName 失效时回退到索引 0 */
    private void locateCurrent() {
        if (TeamData.clientTeams.isEmpty()) {
            currentIndex = -1;
            return;
        }
        for (int i = 0; i < TeamData.clientTeams.size(); i++) {
            if (TeamData.clientTeams.get(i).name.equals(editName)) {
                currentIndex = i;
                return;
            }
        }
        currentIndex = 0;
        TeamConfig c = TeamData.clientTeams.get(0);
        editName = c.name;
        editColor = c.color;
        editSide = c.side;
        editMax = c.maxPlayers;
        editSpawn = new int[]{c.spawnX, c.spawnY, c.spawnZ};
    }

    private TeamConfig currentTeam() {
        if (currentIndex < 0 || currentIndex >= TeamData.clientTeams.size()) return null;
        return TeamData.clientTeams.get(currentIndex);
    }

    private String teamLabel() {
        TeamConfig c = currentTeam();
        if (c == null) return I18n.format("gui.teleporter.none");
        int col = TeamConfig.PALETTE[c.color];
        return String.format("\u00a7%c%s", hexChar(col), String.format(I18n.format("gui.battle.team"), c.name));
    }

    private static char hexChar(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        if (r > 180 && g < 100 && b < 100) return 'c';
        if (b > 160 && r < 120 && g < 160) return '9';
        if (g > 140 && r < 120) return 'a';
        if (r > 180 && g > 140 && b < 100) return 'e';
        return 'f';
    }

    private String colorLabel() {
        return I18n.format("gui.battle.color") + " " + colorName(editColor);
    }

    private String colorName(int idx) {
        int i = ((idx % TeamConfig.PALETTE.length) + TeamConfig.PALETTE.length) % TeamConfig.PALETTE.length;
        return I18n.format(TeamConfig.PALETTE_KEYS[i]);
    }

    private void refreshFields() {
        TeamConfig c = currentTeam();
        if (c == null) {
            nameField.setText("");
            maxField.setText("");
            spawnXField.setText("");
            spawnYField.setText("");
            spawnZField.setText("");
            return;
        }
        nameField.setText(c.name);
        maxField.setText(String.valueOf(c.maxPlayers));
        spawnXField.setText(String.valueOf(c.spawnX));
        spawnYField.setText(String.valueOf(c.spawnY));
        spawnZField.setText(String.valueOf(c.spawnZ));
    }

    private int parseIntSafe(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** PacketTeamSync 同步回来后刷新（由 PacketTeamSync.Handler 调用） */
    public void onTeamsSynced() {
        locateCurrent();
        refreshFields();
        if (buttonList.size() >= 2) {
            ((GuiButton) buttonList.get(0)).displayString = teamLabel();
            ((GuiButton) buttonList.get(1)).displayString = colorLabel();
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
        drawCenteredString(fontRenderer, TextFormatting.GOLD + I18n.format("gui.battle.title"), bx + 160, by - 14, 0xFFFFFF);
        drawString(fontRenderer, I18n.format("gui.battle.teamName"), bx + 16, by + 48, 0xFFFFFF);
        drawString(fontRenderer, I18n.format("gui.battle.max"), bx + 176, by + 48, 0xFFFFFF);
        drawString(fontRenderer, I18n.format("gui.battle.spawnx"), bx + 16, by + 70, 0xFFFFFF);
        drawString(fontRenderer, I18n.format("gui.battle.spawny"), bx + 16, by + 98, 0xFFFFFF);
        drawString(fontRenderer, I18n.format("gui.battle.spawnz"), bx + 16, by + 126, 0xFFFFFF);
        nameField.drawTextBox();
        maxField.drawTextBox();
        spawnXField.drawTextBox();
        spawnYField.drawTextBox();
        spawnZField.drawTextBox();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 文字已在 drawScreen 用绝对坐标绘制
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        TeamConfig c = currentTeam();
        if (button.id == 0) {
            if (!TeamData.clientTeams.isEmpty()) {
                currentIndex = (currentIndex + 1) % TeamData.clientTeams.size();
                TeamConfig nc = currentTeam();
                editName = nc.name;
                editColor = nc.color;
                editSide = nc.side;
                editMax = nc.maxPlayers;
                editSpawn = new int[]{nc.spawnX, nc.spawnY, nc.spawnZ};
                button.displayString = teamLabel();
                refreshFields();
            }
        } else if (button.id == 1) {
            editColor = (editColor + 1) % TeamConfig.PALETTE.length;
            button.displayString = colorLabel();
        } else if (button.id == 3) {
            if (c != null) MyMod.NETWORK.sendToServer(new PacketSaveBackpack(c.name));
        } else if (button.id == 4) {
            if (c != null) {
                saveFields();
                MyMod.NETWORK.sendToServer(new PacketBattleConfig(0, c.name, editName, editColor, editSide,
                    editMax, editSpawn[0], editSpawn[1], editSpawn[2],
                    editSpawn[0] != 0 || editSpawn[1] != 0 || editSpawn[2] != 0));
            }
        } else if (button.id == 5) {
            String nn = nameField.getText().trim();
            if (!nn.isEmpty()) {
                MyMod.NETWORK.sendToServer(new PacketBattleConfig(1, nn, "", editColor, 0, 10, 0, 64, 0, false));
            }
        } else if (button.id == 6) {
            if (c != null) {
                MyMod.NETWORK.sendToServer(new PacketBattleConfig(2, c.name, "", 0, 0, 10, 0, 64, 0, false));
            }
        }
    }

    private void saveFields() {
        editName = nameField.getText().trim();
        editMax = parseIntSafe(maxField.getText(), editMax);
        editSpawn[0] = parseIntSafe(spawnXField.getText(), editSpawn[0]);
        editSpawn[1] = parseIntSafe(spawnYField.getText(), editSpawn[1]);
        editSpawn[2] = parseIntSafe(spawnZField.getText(), editSpawn[2]);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (nameField.isFocused()) nameField.textboxKeyTyped(typedChar, keyCode);
        else if (maxField.isFocused()) maxField.textboxKeyTyped(typedChar, keyCode);
        else if (spawnXField.isFocused()) spawnXField.textboxKeyTyped(typedChar, keyCode);
        else if (spawnYField.isFocused()) spawnYField.textboxKeyTyped(typedChar, keyCode);
        else if (spawnZField.isFocused()) spawnZField.textboxKeyTyped(typedChar, keyCode);
        else super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        nameField.mouseClicked(mouseX, mouseY, mouseButton);
        maxField.mouseClicked(mouseX, mouseY, mouseButton);
        spawnXField.mouseClicked(mouseX, mouseY, mouseButton);
        spawnYField.mouseClicked(mouseX, mouseY, mouseButton);
        spawnZField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
