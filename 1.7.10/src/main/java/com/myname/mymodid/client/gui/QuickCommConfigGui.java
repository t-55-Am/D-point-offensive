package com.myname.mymodid.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Keyboard;

import com.myname.mymodid.config.MyModConfig;

/**
 * 按 Y 打开的设置页面（v1.1.15）：一级分类 tab。
 * tab 0 = 快捷交流配置（原功能）；tab 1 = HUD 配置（分数条/罗盘位置大小开关）。
 */
public class QuickCommConfigGui extends GuiScreen {
    private int tab = 0; // 0=快捷交流 1=HUD配置
    private int currentCat = 0;
    private GuiTextField nameField;
    private GuiTextField[] cmdFields = new GuiTextField[QuickCommConfig.CMD_PER_CAT];
    private GuiButton prevBtn, nextBtn, saveBtn;
    private GuiButton tabQuick, tabHud;

    // HUD 配置页输入框
    private GuiTextField hudScoreY, hudScoreX, hudScoreW, hudRadarY, hudRadarW;
    private GuiButton btnScoreBar, btnRadar, btnRing, btnRange, btnName;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        int cx = width / 2;
        tabQuick = new GuiButton(10, cx - 160, 6, 155, 18, StatCollector.translateToLocal("qc.tab.quick"));
        tabHud = new GuiButton(11, cx - 5, 6, 165, 18, StatCollector.translateToLocal("qc.tab.hud"));
        buttonList.add(tabQuick);
        buttonList.add(tabHud);
        if (tab == 0) {
            initQuickPage(cx);
        } else {
            initHudPage(cx);
        }
    }

    private void initQuickPage(int cx) {
        nameField = new GuiTextField(fontRendererObj, cx - 100, 46, 200, 18);
        nameField.setMaxStringLength(20);
        for (int i = 0; i < QuickCommConfig.CMD_PER_CAT; i++) {
            cmdFields[i] = new GuiTextField(fontRendererObj, cx - 150, 78 + i * 18, 300, 16);
            cmdFields[i].setMaxStringLength(40);
        }
        prevBtn = new GuiButton(0, cx - 160, height - 24, 60, 18, "\u2190");
        nextBtn = new GuiButton(1, cx - 90, height - 24, 60, 18, "\u2192");
        saveBtn = new GuiButton(2, cx + 30, height - 24, 80, 18, StatCollector.translateToLocal("qc.save"));
        buttonList.add(prevBtn);
        buttonList.add(nextBtn);
        buttonList.add(saveBtn);
        loadCat();
    }

    private void initHudPage(int cx) {
        // 开关按钮（两列）
        btnScoreBar = new GuiButton(20, cx - 160, 55, 150, 20, StatCollector.translateToLocal("qc.hud.scorebar") + ": " + onoff(MyModConfig.enableScoreBar));
        btnRadar = new GuiButton(21, cx + 10, 55, 150, 20, StatCollector.translateToLocal("qc.hud.radar") + ": " + onoff(MyModConfig.enableRadar));
        btnRing = new GuiButton(22, cx - 160, 80, 150, 20, StatCollector.translateToLocal("qc.hud.ring") + ": " + onoff(MyModConfig.enableCaptureRing));
        btnRange = new GuiButton(23, cx + 10, 80, 150, 20, StatCollector.translateToLocal("qc.hud.range") + ": " + onoff(MyModConfig.enableRangeCircle));
        btnName = new GuiButton(24, cx - 160, 105, 150, 20, StatCollector.translateToLocal("qc.hud.floating") + ": " + onoff(MyModConfig.enableFloatingName));
        buttonList.add(btnScoreBar);
        buttonList.add(btnRadar);
        buttonList.add(btnRing);
        buttonList.add(btnRange);
        buttonList.add(btnName);
        // 数值输入框
        hudScoreY = new GuiTextField(fontRendererObj, cx - 60, 135, 60, 18);
        hudScoreY.setText(String.valueOf(MyModConfig.scoreBarY));
        hudScoreX = new GuiTextField(fontRendererObj, cx + 30, 135, 60, 18);
        hudScoreX.setText(String.valueOf(MyModConfig.scoreBarX));
        hudScoreW = new GuiTextField(fontRendererObj, cx - 60, 165, 60, 18);
        hudScoreW.setText(String.valueOf(MyModConfig.scoreBarWidth));
        hudRadarY = new GuiTextField(fontRendererObj, cx + 30, 165, 60, 18);
        hudRadarY.setText(String.valueOf(MyModConfig.radarY));
        hudRadarW = new GuiTextField(fontRendererObj, cx - 60, 195, 60, 18);
        hudRadarW.setText(String.valueOf(MyModConfig.radarWidth));
        saveBtn = new GuiButton(2, cx - 40, height - 24, 80, 18, StatCollector.translateToLocal("qc.save"));
        buttonList.add(saveBtn);
    }

    private String onoff(boolean v) {
        return v ? StatCollector.translateToLocal("qc.on") : StatCollector.translateToLocal("qc.off");
    }

    private void loadCat() {
        nameField.setText(QuickCommConfig.catNames[currentCat]);
        for (int i = 0; i < QuickCommConfig.CMD_PER_CAT; i++) {
            cmdFields[i].setText(QuickCommConfig.commands[currentCat][i]);
        }
    }

    private void saveCat() {
        QuickCommConfig.catNames[currentCat] = nameField.getText();
        for (int i = 0; i < QuickCommConfig.CMD_PER_CAT; i++) {
            QuickCommConfig.commands[currentCat][i] = cmdFields[i].getText();
        }
    }

    private void saveHud() {
        try { MyModConfig.scoreBarY = Integer.parseInt(hudScoreY.getText()); } catch (Exception e) {}
        try { MyModConfig.scoreBarX = Integer.parseInt(hudScoreX.getText()); } catch (Exception e) {}
        try { MyModConfig.scoreBarWidth = Integer.parseInt(hudScoreW.getText()); } catch (Exception e) {}
        try { MyModConfig.radarY = Integer.parseInt(hudRadarY.getText()); } catch (Exception e) {}
        try { MyModConfig.radarWidth = Integer.parseInt(hudRadarW.getText()); } catch (Exception e) {}
        MyModConfig.save();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 10) { saveAll(); tab = 0; initGui(); }
        else if (button.id == 11) { saveAll(); tab = 1; initGui(); }
        else if (tab == 0) {
            if (button.id == 0 && currentCat > 0) { saveCat(); currentCat--; loadCat(); }
            else if (button.id == 1 && currentCat < QuickCommConfig.CATEGORIES - 1) { saveCat(); currentCat++; loadCat(); }
            else if (button.id == 2) { saveCat(); QuickCommConfig.save(); }
        } else {
            if (button.id == 20) { MyModConfig.enableScoreBar = !MyModConfig.enableScoreBar; button.displayString = StatCollector.translateToLocal("qc.hud.scorebar") + ": " + onoff(MyModConfig.enableScoreBar); }
            else if (button.id == 21) { MyModConfig.enableRadar = !MyModConfig.enableRadar; button.displayString = StatCollector.translateToLocal("qc.hud.radar") + ": " + onoff(MyModConfig.enableRadar); }
            else if (button.id == 22) { MyModConfig.enableCaptureRing = !MyModConfig.enableCaptureRing; button.displayString = StatCollector.translateToLocal("qc.hud.ring") + ": " + onoff(MyModConfig.enableCaptureRing); }
            else if (button.id == 23) { MyModConfig.enableRangeCircle = !MyModConfig.enableRangeCircle; button.displayString = StatCollector.translateToLocal("qc.hud.range") + ": " + onoff(MyModConfig.enableRangeCircle); }
            else if (button.id == 24) { MyModConfig.enableFloatingName = !MyModConfig.enableFloatingName; button.displayString = StatCollector.translateToLocal("qc.hud.floating") + ": " + onoff(MyModConfig.enableFloatingName); }
            else if (button.id == 2) { saveHud(); }
        }
    }

    private void saveAll() {
        if (tab == 0) { saveCat(); QuickCommConfig.save(); }
        else { saveHud(); }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int cx = width / 2;
        tabQuick.enabled = tab != 0;
        tabHud.enabled = tab != 1;
        if (tab == 0) {
            drawCenteredString(fontRendererObj, StatCollector.translateToLocal("qc.title.quick") + " - " + (currentCat + 1) + "/" + QuickCommConfig.CATEGORIES, cx, 30, 0xFFFF55);
            drawString(fontRendererObj, StatCollector.translateToLocal("qc.catname") + ":", cx - 100, 36, 0xFFFFFF);
            nameField.drawTextBox();
            drawString(fontRendererObj, StatCollector.translateToLocal("qc.cmds") + ":", cx - 150, 66, 0x55FF55);
            for (int i = 0; i < QuickCommConfig.CMD_PER_CAT; i++) {
                drawString(fontRendererObj, (i + 1) + ".", cx - 165, 80 + i * 18, 0xAAAAAA);
                cmdFields[i].drawTextBox();
            }
            prevBtn.enabled = currentCat > 0;
            nextBtn.enabled = currentCat < QuickCommConfig.CATEGORIES - 1;
        } else {
            drawCenteredString(fontRendererObj, StatCollector.translateToLocal("qc.title.hud"), cx, 40, 0x55FFFF);
            drawString(fontRendererObj, StatCollector.translateToLocal("qc.hud.scorey") + ":", cx - 150, 138, 0xFFFFFF);
            drawString(fontRendererObj, StatCollector.translateToLocal("qc.hud.scorex") + ":", cx - 40, 138, 0xFFFFFF);
            drawString(fontRendererObj, StatCollector.translateToLocal("qc.hud.scorew") + ":", cx - 150, 168, 0xFFFFFF);
            drawString(fontRendererObj, StatCollector.translateToLocal("qc.hud.radary") + ":", cx - 40, 168, 0xFFFFFF);
            drawString(fontRendererObj, StatCollector.translateToLocal("qc.hud.radarw") + ":", cx - 150, 198, 0xFFFFFF);
            drawString(fontRendererObj, StatCollector.translateToLocal("qc.hud.hint"), cx, 225, 0xAAAAAA);
            hudScoreY.drawTextBox();
            hudScoreX.drawTextBox();
            hudScoreW.drawTextBox();
            hudRadarY.drawTextBox();
            hudRadarW.drawTextBox();
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_Y) {
            saveAll(); mc.displayGuiScreen(null); return;
        }
        if (tab == 0) {
            if (nameField.isFocused()) nameField.textboxKeyTyped(typedChar, keyCode);
            else {
                for (int i = 0; i < QuickCommConfig.CMD_PER_CAT; i++) {
                    if (cmdFields[i].isFocused()) { cmdFields[i].textboxKeyTyped(typedChar, keyCode); break; }
                }
            }
        } else {
            if (hudScoreY.isFocused()) hudScoreY.textboxKeyTyped(typedChar, keyCode);
            else if (hudScoreX.isFocused()) hudScoreX.textboxKeyTyped(typedChar, keyCode);
            else if (hudScoreW.isFocused()) hudScoreW.textboxKeyTyped(typedChar, keyCode);
            else if (hudRadarY.isFocused()) hudRadarY.textboxKeyTyped(typedChar, keyCode);
            else if (hudRadarW.isFocused()) hudRadarW.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (tab == 0) {
            nameField.mouseClicked(mouseX, mouseY, mouseButton);
            for (int i = 0; i < QuickCommConfig.CMD_PER_CAT; i++) cmdFields[i].mouseClicked(mouseX, mouseY, mouseButton);
        } else {
            hudScoreY.mouseClicked(mouseX, mouseY, mouseButton);
            hudScoreX.mouseClicked(mouseX, mouseY, mouseButton);
            hudScoreW.mouseClicked(mouseX, mouseY, mouseButton);
            hudRadarY.mouseClicked(mouseX, mouseY, mouseButton);
            hudRadarW.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }
    @Override
    public boolean doesGuiPauseGame() { return false; }
}
