package com.myname.mymodid.client.gui;

import com.myname.mymodid.network.CPayloads.ScoreConfigPayload;
import com.myname.mymodid.network.Network;
import com.myname.mymodid.score.ScoreData;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class GuiScoreController extends DpScreen {
    private final BlockPos pos;
    private EditBox initialScoreField, scorePerSecondField;
    private EditBox attackField, speedField, healthField;

    public GuiScoreController(net.minecraft.world.entity.player.Player player, BlockPos pos) {
        super(Component.literal("Score Controller"));
        this.pos = pos;
    }

    private ScoreData data() { return ScoreData.getClient(); }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        clearButtons();

        addButton(cx - 85, cy - 130, 170, 20,
            () -> Component.translatable(data().gameStarted ? "dp.gui.score.end" : "dp.gui.score.start").getString(),
            () -> {
                ScoreData d = data();
                d.gameStarted = !d.gameStarted;
                Network.sendToServer(new ScoreConfigPayload(4, 0, 0, false, 0, 0, 0));
            });

        addButton(cx - 85, cy - 100, 170, 20, this::getModeButtonText, () -> {
            ScoreData d = data();
            d.pveEnabled = !d.pveEnabled;
            Network.sendToServer(new ScoreConfigPayload(2, 0, 0, d.pveEnabled, 0, 0, 0));
            rebuildFields();
        });

        rebuildFields();
    }

    private void rebuildFields() {
        int cx = width / 2;
        int cy = height / 2;
        ScoreData sd = data();
        clearWidgets();
        clearButtons();
        addButton(cx - 85, cy - 130, 170, 20,
            () -> Component.translatable(data().gameStarted ? "dp.gui.score.end" : "dp.gui.score.start").getString(),
            () -> {
                ScoreData d = data();
                d.gameStarted = !d.gameStarted;
                Network.sendToServer(new ScoreConfigPayload(4, 0, 0, false, 0, 0, 0));
            });
        addButton(cx - 85, cy - 100, 170, 20, this::getModeButtonText, () -> {
            ScoreData d = data();
            d.pveEnabled = !d.pveEnabled;
            Network.sendToServer(new ScoreConfigPayload(2, 0, 0, d.pveEnabled, 0, 0, 0));
            rebuildFields();
        });

        if (!sd.pveEnabled) {
            initialScoreField = new EditBox(font, cx - 80, cy - 55, 160, 20, Component.literal("initial"));
            initialScoreField.setMaxLength(10);
            initialScoreField.setValue(String.valueOf(sd.initialScore));
            addRenderableWidget(initialScoreField);
            setInitialFocus(initialScoreField);
            scorePerSecondField = new EditBox(font, cx - 80, cy - 15, 160, 20, Component.literal("perSec"));
            scorePerSecondField.setMaxLength(5);
            scorePerSecondField.setValue(String.valueOf(sd.scorePerSecond));
            addRenderableWidget(scorePerSecondField);
            addButton(cx - 90, cy + 25, 80, 20,
                () -> Component.translatable("dp.gui.score.reset").getString(), () -> {
                Network.sendToServer(new ScoreConfigPayload(1, 0, 0, false, 0, 0, 0));
            });
            addButton(cx + 10, cy + 25, 80, 20,
                () -> Component.translatable("dp.gui.score.save").getString(), () -> {
                try {
                    int init = Integer.parseInt(initialScoreField.getValue());
                    int perSec = Integer.parseInt(scorePerSecondField.getValue());
                    Network.sendToServer(new ScoreConfigPayload(0, init, perSec, false, 0, 0, 0));
                } catch (NumberFormatException ignored) {}
                onClose();
            });
        } else {
            attackField = new EditBox(font, cx - 80, cy - 55, 160, 20, Component.literal("attack"));
            attackField.setMaxLength(6);
            attackField.setValue(String.valueOf(sd.zombieAttack));
            addRenderableWidget(attackField);
            setInitialFocus(attackField);
            speedField = new EditBox(font, cx - 80, cy - 20, 160, 20, Component.literal("speed"));
            speedField.setMaxLength(6);
            speedField.setValue(String.format("%.1f", sd.zombieSpeed * 15.0));
            addRenderableWidget(speedField);
            healthField = new EditBox(font, cx - 80, cy + 15, 160, 20, Component.literal("health"));
            healthField.setMaxLength(6);
            healthField.setValue(String.valueOf(sd.zombieHealth));
            addRenderableWidget(healthField);
            addButton(cx - 90, cy + 55, 80, 20,
                () -> Component.translatable("dp.gui.score.reset").getString(), () -> {
                Network.sendToServer(new ScoreConfigPayload(1, 0, 0, false, 0, 0, 0));
            });
            addButton(cx + 10, cy + 55, 80, 20,
                () -> Component.translatable("dp.gui.score.savepve").getString(), () -> {
                try {
                    double atk = Double.parseDouble(attackField.getValue());
                    double spd = Double.parseDouble(speedField.getValue());
                    double hp = Double.parseDouble(healthField.getValue());
                    Network.sendToServer(new ScoreConfigPayload(3, 0, 0, false, atk, spd, hp));
                } catch (NumberFormatException ignored) {}
                onClose();
            });
        }
    }

    private String getModeButtonText() {
        return Component.translatable(data().pveEnabled ? "dp.gui.switch.pve" : "dp.gui.switch.pvp").getString();
    }

    @Override
    protected void drawContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int cx = width / 2, cy = height / 2;
        ScoreData sd = data();
        boolean pve = sd.pveEnabled;
        if (!pve) {
            Component title = Component.translatable("dp.gui.score.title.pvp");
            g.drawString(font, title, cx - font.width(title) / 2, cy - 145, 0xFFFF55, false);
            g.drawString(font, Component.translatable("dp.gui.score.initial"), cx - 80, cy - 68, 0xFFFFFF, false);
            g.drawString(font, Component.translatable("dp.gui.score.persec"), cx - 80, cy - 28, 0xFFFFFF, false);
        } else {
            Component title = Component.translatable("dp.gui.score.title.pve");
            g.drawString(font, title, cx - font.width(title) / 2, cy - 145, 0x55FFFF, false);
            g.drawString(font, Component.translatable("dp.gui.score.attack"), cx - 80, cy - 68, 0x55FF55, false);
            g.drawString(font, Component.translatable("dp.gui.score.speed"), cx - 80, cy - 33, 0x55FF55, false);
            g.drawString(font, Component.translatable("dp.gui.score.health"), cx - 80, cy + 2, 0x55FF55, false);
        }
        String status = (sd.gameStarted ? "§a" : "§c")
            + Component.translatable(sd.gameStarted ? "dp.gui.score.ongoing" : "dp.gui.score.notstarted").getString();
        String statusText = Component.translatable("dp.gui.score.status").getString() + " " + status;
        g.drawString(font, statusText, cx - font.width(statusText) / 2, cy - 155, 0xFFFFFF, false);
    }
}
