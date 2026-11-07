package com.myname.mymodid.client.gui;

import com.myname.mymodid.config.MyModConfig;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class QuickCommConfigGui extends DpScreen {
    private int tab = 0;
    private int currentCat = 0;
    private EditBox nameField;
    private EditBox[] cmdFields = new EditBox[QuickCommConfig.CMD_PER_CAT];
    private DpBtn prevBtn, nextBtn;
    private DpBtn tabQuick, tabHud;

    private EditBox hudScoreY, hudScoreX, hudScoreW, hudRadarY, hudRadarW, hudNameScaleBox;

    public QuickCommConfigGui() { super(Component.literal("Quick Comm Config")); }

    @Override
    protected void init() {
        clearWidgets();
        clearButtons();
        int cx = width / 2;
        tabQuick = addButton(cx - 160, 6, 155, 18,
            () -> Component.translatable("qc.tab.quick").getString(), () -> { saveAll(); tab = 0; init(); });
        tabHud = addButton(cx - 5, 6, 165, 18,
            () -> Component.translatable("qc.tab.hud").getString(), () -> { saveAll(); tab = 1; init(); });
        if (tab == 0) { initQuickPage(cx); } else { initHudPage(cx); }
    }

    private void initQuickPage(int cx) {
        nameField = new EditBox(font, cx - 100, 46, 200, 18, Component.literal("name"));
        nameField.setMaxLength(20);
        addRenderableWidget(nameField);
        setInitialFocus(nameField);
        for (int i = 0; i < QuickCommConfig.CMD_PER_CAT; i++) {
            cmdFields[i] = new EditBox(font, cx - 150, 78 + i * 18, 300, 16, Component.literal("cmd" + i));
            cmdFields[i].setMaxLength(40);
            addRenderableWidget(cmdFields[i]);
        }
        prevBtn = addButton(cx - 160, height - 24, 60, 18, () -> "←", () -> {
            if (currentCat > 0) { saveCat(); currentCat--; loadCat(); }
        });
        nextBtn = addButton(cx - 90, height - 24, 60, 18, () -> "→", () -> {
            if (currentCat < QuickCommConfig.CATEGORIES - 1) { saveCat(); currentCat++; loadCat(); }
        });
        addButton(cx + 30, height - 24, 80, 18,
            () -> Component.translatable("qc.save").getString(), () -> { saveCat(); QuickCommConfig.save(); });
        loadCat();
    }

    private void initHudPage(int cx) {
        addButton(cx - 160, 55, 150, 20,
            () -> Component.translatable("qc.hud.scorebar").getString() + ": " + onoff(MyModConfig.hudScoreBar),
            () -> MyModConfig.hudScoreBar = !MyModConfig.hudScoreBar);
        addButton(cx + 10, 55, 150, 20,
            () -> Component.translatable("qc.hud.radar").getString() + ": " + onoff(MyModConfig.hudRadar),
            () -> MyModConfig.hudRadar = !MyModConfig.hudRadar);
        addButton(cx - 160, 80, 150, 20,
            () -> Component.translatable("qc.hud.ring").getString() + ": " + onoff(MyModConfig.hudRing),
            () -> MyModConfig.hudRing = !MyModConfig.hudRing);
        addButton(cx + 10, 80, 150, 20,
            () -> Component.translatable("qc.hud.range").getString() + ": " + onoff(MyModConfig.hudRange),
            () -> MyModConfig.hudRange = !MyModConfig.hudRange);
        addButton(cx - 160, 105, 150, 20,
            () -> Component.translatable("qc.hud.floating").getString() + ": " + onoff(MyModConfig.hudName),
            () -> MyModConfig.hudName = !MyModConfig.hudName);

        hudScoreY = new EditBox(font, cx - 60, 135, 60, 18, Component.literal("scoreY"));
        hudScoreY.setValue(String.valueOf(MyModConfig.scoreBarY));
        setInitialFocus(hudScoreY);
        hudScoreX = new EditBox(font, cx + 30, 135, 60, 18, Component.literal("scoreX"));
        hudScoreX.setValue(String.valueOf(MyModConfig.scoreBarX));
        hudScoreW = new EditBox(font, cx - 60, 165, 60, 18, Component.literal("scoreW"));
        hudScoreW.setValue(String.valueOf(MyModConfig.scoreBarWidth));
        hudRadarY = new EditBox(font, cx + 30, 165, 60, 18, Component.literal("radarY"));
        hudRadarY.setValue(String.valueOf(MyModConfig.radarY));
        hudRadarW = new EditBox(font, cx - 60, 195, 60, 18, Component.literal("radarW"));
        hudRadarW.setValue(String.valueOf(MyModConfig.radarWidth));
        hudNameScaleBox = new EditBox(font, cx + 30, 195, 60, 18, Component.literal("nameScale"));
        hudNameScaleBox.setValue(String.valueOf(MyModConfig.hudNameScale));
        addRenderableWidget(hudScoreY);
        addRenderableWidget(hudScoreX);
        addRenderableWidget(hudScoreW);
        addRenderableWidget(hudRadarY);
        addRenderableWidget(hudRadarW);
        addRenderableWidget(hudNameScaleBox);
        addButton(cx - 40, height - 24, 80, 18,
            () -> Component.translatable("qc.save").getString(), this::saveHud);
    }

    private String onoff(boolean v) {
        return v ? Component.translatable("qc.on").getString() : Component.translatable("qc.off").getString();
    }

    private void loadCat() {
        nameField.setValue(QuickCommConfig.catNames[currentCat]);
        for (int i = 0; i < QuickCommConfig.CMD_PER_CAT; i++) {
            cmdFields[i].setValue(QuickCommConfig.commands[currentCat][i]);
        }
    }

    private void saveCat() {
        QuickCommConfig.catNames[currentCat] = nameField.getValue();
        for (int i = 0; i < QuickCommConfig.CMD_PER_CAT; i++) {
            QuickCommConfig.commands[currentCat][i] = cmdFields[i].getValue();
        }
    }

    private void saveHud() {
        try { MyModConfig.scoreBarY = Integer.parseInt(hudScoreY.getValue()); } catch (Exception ignored) {}
        try { MyModConfig.scoreBarX = Integer.parseInt(hudScoreX.getValue()); } catch (Exception ignored) {}
        try { MyModConfig.scoreBarWidth = Integer.parseInt(hudScoreW.getValue()); } catch (Exception ignored) {}
        try { MyModConfig.radarY = Integer.parseInt(hudRadarY.getValue()); } catch (Exception ignored) {}
        try { MyModConfig.radarWidth = Integer.parseInt(hudRadarW.getValue()); } catch (Exception ignored) {}
        try { MyModConfig.hudNameScale = Math.max(0.4, Math.min(2.0, Double.parseDouble(hudNameScaleBox.getValue()))); } catch (Exception ignored) {}
        MyModConfig.saveHud();
    }

    private void saveAll() {
        if (tab == 0) { saveCat(); QuickCommConfig.save(); }
        else { saveHud(); }
    }

    @Override
    protected void drawContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int cx = width / 2;
        tabQuick.enabled = tab != 0;
        tabHud.enabled = tab != 1;
        if (tab == 0) {
            String quickTitle = Component.translatable("qc.title.quick").getString() + " - " + (currentCat + 1) + "/" + QuickCommConfig.CATEGORIES;
            g.drawString(font, quickTitle, cx - font.width(quickTitle) / 2, 30, 0xFFFF55, false);
            g.drawString(font, Component.translatable("qc.catname").getString() + ":", cx - 100, 36, 0xFFFFFF, false);
            g.drawString(font, Component.translatable("qc.cmds").getString() + ":", cx - 150, 66, 0x55FF55, false);
            for (int i = 0; i < QuickCommConfig.CMD_PER_CAT; i++) {
                g.drawString(font, (i + 1) + ".", cx - 165, 80 + i * 18, 0xAAAAAA, false);
            }
            if (prevBtn != null) prevBtn.enabled = currentCat > 0;
            if (nextBtn != null) nextBtn.enabled = currentCat < QuickCommConfig.CATEGORIES - 1;
        } else {
            String hudTitle = Component.translatable("qc.title.hud").getString();
            g.drawString(font, hudTitle, cx - font.width(hudTitle) / 2, 40, 0x55FFFF, false);
            g.drawString(font, Component.translatable("qc.hud.scorey").getString() + ":", cx - 150, 138, 0xFFFFFF, false);
            g.drawString(font, Component.translatable("qc.hud.scorex").getString() + ":", cx - 40, 138, 0xFFFFFF, false);
            g.drawString(font, Component.translatable("qc.hud.scorew").getString() + ":", cx - 150, 168, 0xFFFFFF, false);
            g.drawString(font, Component.translatable("qc.hud.radary").getString() + ":", cx - 40, 168, 0xFFFFFF, false);
            g.drawString(font, Component.translatable("qc.hud.radarw").getString() + ":", cx - 150, 198, 0xFFFFFF, false);
            g.drawString(font, Component.translatable("qc.hud.namescale").getString() + ":", cx - 40, 198, 0xFFFFFF, false);
            g.drawString(font, Component.translatable("qc.hud.hint").getString(), cx, 225, 0xAAAAAA, false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_Y) {
            saveAll(); onClose(); return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
