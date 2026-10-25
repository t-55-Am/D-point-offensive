package com.myname.mymodid.client.gui;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.inventory.ContainerScoreController;
import com.myname.mymodid.network.PacketScoreConfig;
import com.myname.mymodid.score.ScoreData;
import com.myname.mymodid.tileentity.TileEntityScoreController;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

/**
 * 分数控制器 GUI（v1.1.11）：框内布局重做；新增对抗模式按钮（双队对抗 / 多队伍）。
 */
public class GuiScoreController extends GuiContainer {
    private final TileEntityScoreController tile;
    private GuiTextField initialScoreField;
    private GuiTextField scorePerSecondField;
    private GuiTextField attackField;
    private GuiTextField speedField;
    private GuiTextField healthField;
    private GuiButton btnToggleMode;
    private GuiButton btnGameControl;
    private GuiButton btnMode;

    public GuiScoreController(TileEntityScoreController tile) {
        super(new ContainerScoreController(Minecraft.getMinecraft().thePlayer.inventory));
        this.tile = tile;
        this.xSize = 216;
        this.ySize = 210;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        int bx = guiLeft;
        int by = guiTop;
        ScoreData sd = ScoreData.get(tile.getWorldObj());

        btnGameControl = new GuiButton(200, bx + 23, by + 4, 170, 20,
            sd.gameStarted ? StatCollector.translateToLocal("dp.gui.score.end") : StatCollector.translateToLocal("dp.gui.score.start"));
        buttonList.add(btnGameControl);

        btnMode = new GuiButton(101, bx + 23, by + 28, 170, 20, getModeText(sd));
        buttonList.add(btnMode);

        btnToggleMode = new GuiButton(100, bx + 23, by + 52, 170, 20, getPveText(sd));
        buttonList.add(btnToggleMode);

        boolean pve = sd.pveEnabled;
        if (!pve) {
            initialScoreField = new GuiTextField(fontRendererObj, bx + 28, by + 88, 160, 20);
            initialScoreField.setMaxStringLength(10);
            initialScoreField.setText(String.valueOf(sd.initialScore));
            scorePerSecondField = new GuiTextField(fontRendererObj, bx + 28, by + 122, 160, 20);
            scorePerSecondField.setMaxStringLength(5);
            scorePerSecondField.setText(String.valueOf(sd.scorePerSecond));
            buttonList.add(new GuiButton(0, bx + 18, by + 150, 80, 20, StatCollector.translateToLocal("dp.gui.score.reset")));
            buttonList.add(new GuiButton(1, bx + 118, by + 150, 80, 20, StatCollector.translateToLocal("dp.gui.score.save")));
        } else {
            attackField = new GuiTextField(fontRendererObj, bx + 28, by + 88, 160, 20);
            attackField.setMaxStringLength(6);
            attackField.setText(String.valueOf(sd.zombieAttack));
            speedField = new GuiTextField(fontRendererObj, bx + 28, by + 122, 160, 20);
            speedField.setMaxStringLength(6);
            speedField.setText(String.format("%.1f", sd.zombieSpeed * 15.0));
            healthField = new GuiTextField(fontRendererObj, bx + 28, by + 156, 160, 20);
            healthField.setMaxStringLength(6);
            healthField.setText(String.valueOf(sd.zombieHealth));
            buttonList.add(new GuiButton(0, bx + 18, by + 184, 80, 20, StatCollector.translateToLocal("dp.gui.score.reset")));
            buttonList.add(new GuiButton(3, bx + 118, by + 184, 80, 20, StatCollector.translateToLocal("dp.gui.score.savepve")));
        }
    }

    private String getModeText(ScoreData sd) {
        return String.format(StatCollector.translateToLocal("dp.gui.score.mode"),
            sd.mode == 1 ? StatCollector.translateToLocal("dp.gui.score.mode.multi") : StatCollector.translateToLocal("dp.gui.score.mode.duo"));
    }

    private String getPveText(ScoreData sd) {
        return sd.pveEnabled ? StatCollector.translateToLocal("dp.gui.switch.pve") : StatCollector.translateToLocal("dp.gui.switch.pvp");
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        // v1.1.14：加半透明底板，界面不再直接叠在游戏画面上（发黑/看不清）
        drawRect(guiLeft - 12, guiTop - 24, guiLeft + xSize + 12, guiTop + ySize + 12, 0xC0101010);
        drawRect(guiLeft - 12, guiTop - 24, guiLeft + xSize + 12, guiTop - 20, 0xFF555555);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int bx = guiLeft, by = guiTop;
        ScoreData sd = ScoreData.get(tile.getWorldObj());
        boolean pve = sd.pveEnabled;
        if (btnToggleMode != null) btnToggleMode.displayString = getPveText(sd);
        if (btnMode != null) btnMode.displayString = getModeText(sd);
        if (btnGameControl != null) btnGameControl.displayString = sd.gameStarted ? StatCollector.translateToLocal("dp.gui.score.end") : StatCollector.translateToLocal("dp.gui.score.start");

        drawCenteredString(fontRendererObj, EnumChatFormatting.GOLD + StatCollector.translateToLocal("dp.gui.score.title." + (pve ? "pve" : "pvp")), bx + 108, by - 18, 0xFFFF55);
        String status = sd.gameStarted ? "\u00a7a" + StatCollector.translateToLocal("dp.gui.score.ongoing") : "\u00a7c" + StatCollector.translateToLocal("dp.gui.score.notstarted");
        drawCenteredString(fontRendererObj, StatCollector.translateToLocal("dp.gui.score.status") + " " + status, bx + 108, by - 8, 0xFFFFFF);

        if (!pve) {
            drawString(fontRendererObj, StatCollector.translateToLocal("dp.gui.score.initial"), bx + 23, by + 76, 0xFFFFFF);
            drawString(fontRendererObj, StatCollector.translateToLocal("dp.gui.score.persec"), bx + 23, by + 110, 0xFFFFFF);
            if (initialScoreField != null) initialScoreField.drawTextBox();
            if (scorePerSecondField != null) scorePerSecondField.drawTextBox();
        } else {
            drawString(fontRendererObj, StatCollector.translateToLocal("dp.gui.score.attack"), bx + 23, by + 76, 0x55FF55);
            drawString(fontRendererObj, StatCollector.translateToLocal("dp.gui.score.speed"), bx + 23, by + 110, 0x55FF55);
            drawString(fontRendererObj, StatCollector.translateToLocal("dp.gui.score.health"), bx + 23, by + 144, 0x55FF55);
            if (attackField != null) attackField.drawTextBox();
            if (speedField != null) speedField.drawTextBox();
            if (healthField != null) healthField.drawTextBox();
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        ScoreData data = ScoreData.get(tile.getWorldObj());
        if (button.id == 200) {
            data.gameStarted = !data.gameStarted;
            MyMod.NETWORK.sendToServer(new PacketScoreConfig(4, 0, 0, 0));
            initGui();
        } else if (button.id == 101) {
            // v1.1.11：切换对抗模式
            MyMod.NETWORK.sendToServer(new PacketScoreConfig(5, 0, 0, 0));
            initGui();
        } else if (button.id == 100) {
            data.pveEnabled = !data.pveEnabled;
            MyMod.NETWORK.sendToServer(new PacketScoreConfig(true, true));
            initGui();
        } else if (button.id == 0) {
            MyMod.NETWORK.sendToServer(new PacketScoreConfig(true));
        } else if (button.id == 1) {
            try {
                int init = Integer.parseInt(initialScoreField.getText());
                int perSec = Integer.parseInt(scorePerSecondField.getText());
                MyMod.NETWORK.sendToServer(new PacketScoreConfig(init, perSec));
            } catch (NumberFormatException ignored) {}
        } else if (button.id == 3) {
            try {
                double atk = Double.parseDouble(attackField.getText());
                double spd = Double.parseDouble(speedField.getText());
                double hp = Double.parseDouble(healthField.getText());
                MyMod.NETWORK.sendToServer(new PacketScoreConfig(3, atk, spd, hp));
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        boolean pve = ScoreData.get(tile.getWorldObj()).pveEnabled;
        if (!pve) {
            if (initialScoreField != null && initialScoreField.isFocused()) initialScoreField.textboxKeyTyped(typedChar, keyCode);
            else if (scorePerSecondField != null && scorePerSecondField.isFocused()) scorePerSecondField.textboxKeyTyped(typedChar, keyCode);
            else super.keyTyped(typedChar, keyCode);
        } else {
            if (attackField != null && attackField.isFocused()) attackField.textboxKeyTyped(typedChar, keyCode);
            else if (speedField != null && speedField.isFocused()) speedField.textboxKeyTyped(typedChar, keyCode);
            else if (healthField != null && healthField.isFocused()) healthField.textboxKeyTyped(typedChar, keyCode);
            else super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        boolean pve = ScoreData.get(tile.getWorldObj()).pveEnabled;
        if (!pve) {
            if (initialScoreField != null) initialScoreField.mouseClicked(mouseX, mouseY, mouseButton);
            if (scorePerSecondField != null) scorePerSecondField.mouseClicked(mouseX, mouseY, mouseButton);
        } else {
            if (attackField != null) attackField.mouseClicked(mouseX, mouseY, mouseButton);
            if (speedField != null) speedField.mouseClicked(mouseX, mouseY, mouseButton);
            if (healthField != null) healthField.mouseClicked(mouseX, mouseY, mouseButton);
        }
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
