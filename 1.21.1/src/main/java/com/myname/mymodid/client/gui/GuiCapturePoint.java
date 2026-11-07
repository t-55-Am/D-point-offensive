package com.myname.mymodid.client.gui;

import com.myname.mymodid.client.ClientEventHandler;
import com.myname.mymodid.network.CPayloads.CapturePointConfigPayload;
import com.myname.mymodid.network.CPayloads.CapturePointFlagsPayload;
import com.myname.mymodid.network.Network;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

public class GuiCapturePoint extends DpScreen {
    private final TileEntityCapturePoint te;
    private final int x, y, z;
    private EditBox radiusField, timeField, nameField;
    private EditBox pveIntervalField, pveCountField, pveMinField, pveMaxField;
    private DpBtn ownerBtn;

    public GuiCapturePoint(net.minecraft.world.entity.player.Player player, BlockPos pos) {
        super(Component.literal("Capture Point"));
        this.x = pos.getX(); this.y = pos.getY(); this.z = pos.getZ();
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
        this.te = (be instanceof TileEntityCapturePoint) ? (TileEntityCapturePoint) be : null;
    }

    @Override
    protected void init() {
        if (te == null) return;
        int cx = width / 2 - 150;
        int cy = height / 2 - 105;
        clearButtons();

        radiusField = new EditBox(font, cx + 80, cy + 30, 60, 18, Component.literal("radius"));
        radiusField.setMaxLength(3);
        radiusField.setValue(String.valueOf(te.getRadius()));
        timeField = new EditBox(font, cx + 80, cy + 58, 60, 18, Component.literal("time"));
        timeField.setMaxLength(4);
        timeField.setValue(String.valueOf(te.getCaptureTime()));
        nameField = new EditBox(font, cx + 80, cy + 86, 60, 18, Component.literal("name"));
        nameField.setMaxLength(20);
        nameField.setValue(te.getPointName());
        pveIntervalField = new EditBox(font, cx + 220, cy + 30, 60, 18, Component.literal("interval"));
        pveIntervalField.setMaxLength(4);
        pveIntervalField.setValue(String.valueOf(te.getPveSpawnInterval()));
        pveCountField = new EditBox(font, cx + 220, cy + 58, 60, 18, Component.literal("count"));
        pveCountField.setMaxLength(3);
        pveCountField.setValue(String.valueOf(te.getPveSpawnCount()));
        pveMinField = new EditBox(font, cx + 220, cy + 86, 60, 18, Component.literal("min"));
        pveMinField.setMaxLength(3);
        pveMinField.setValue(String.valueOf(te.getPveMinCount()));
        pveMaxField = new EditBox(font, cx + 220, cy + 114, 60, 18, Component.literal("max"));
        pveMaxField.setMaxLength(3);
        pveMaxField.setValue(String.valueOf(te.getPveMaxCount()));

        addRenderableWidget(radiusField);
        setInitialFocus(radiusField);
        addRenderableWidget(timeField);
        addRenderableWidget(nameField);
        addRenderableWidget(pveIntervalField);
        addRenderableWidget(pveCountField);
        addRenderableWidget(pveMinField);
        addRenderableWidget(pveMaxField);

        addButton(cx + 110, cy + 145, 80, 20,
            () -> Component.translatable("dp.gui.capture.save").getString(), this::actionSave);
        addButton(cx + 110, cy + 170, 80, 20, this::baseLabel, () -> {
            boolean v = !te.isMainBase();
            te.setMainBase(v);
            Network.sendToServer(new CapturePointFlagsPayload(x, y, z, true, v, false, false, false));
            ownerBtn.visible = v;
        });
        addButton(cx + 20, cy + 170, 80, 20, this::reviveLabel, () -> {
            boolean v = !te.isReviveMode();
            te.setReviveMode(v);
            Network.sendToServer(new CapturePointFlagsPayload(x, y, z, false, false, true, v, false));
        });
        ownerBtn = addButton(cx + 200, cy + 170, 80, 20, this::ownerLabel, () -> {
            if (!te.isMainBase()) return;
            Network.sendToServer(new CapturePointFlagsPayload(x, y, z, false, false, false, false, true));
            te.setOwnerTeam(cycleOwner(te.getOwnerTeam()));
        });
        ownerBtn.visible = te.isMainBase();
    }

    private void actionSave() {
        try {
            int r = Integer.parseInt(radiusField.getValue().trim());
            int t = Integer.parseInt(timeField.getValue().trim());
            String name = nameField.getValue().trim();
            int pi = Integer.parseInt(pveIntervalField.getValue().trim());
            int pc = Integer.parseInt(pveCountField.getValue().trim());
            int pmin = Integer.parseInt(pveMinField.getValue().trim());
            int pmax = Integer.parseInt(pveMaxField.getValue().trim());
            Network.sendToServer(new CapturePointConfigPayload(x, y, z, r, t, name, pi, pc, pmin, pmax));
        } catch (NumberFormatException ignored) {}
        onClose();
    }

    private String baseLabel() {
        return Component.translatable("dp.gui.capture.base").getString() + " "
            + Component.translatable(te.isMainBase() ? "dp.gui.on" : "dp.gui.off").getString();
    }

    private String reviveLabel() {
        return Component.translatable("dp.gui.capture.revive").getString() + " "
            + Component.translatable(te.isReviveMode() ? "dp.gui.on" : "dp.gui.off").getString();
    }

    private String ownerLabel() {
        String owner = te.getOwnerTeam();
        String disp;
        if (ClientEventHandler.pveEnabled) {
            disp = Component.translatable("PvE".equals(owner) ? "dp.gui.capture.zombie" : "dp.gui.capture.human").getString();
        } else {
            boolean isRed = owner != null && (owner.toLowerCase().contains("red") || owner.contains("红"));
            disp = Component.translatable(isRed ? "dp.gui.capture.redteam" : "dp.gui.capture.blueteam").getString();
        }
        return Component.translatable("dp.gui.capture.baseowner").getString() + " " + disp;
    }

    private String cycleOwner(String cur) {
        if (ClientEventHandler.pveEnabled) {
            return "PVEHUMAN".equals(cur) ? "PvE" : "PVEHUMAN";
        }
        boolean isRed = cur != null && (cur.toLowerCase().contains("red") || cur.contains("红"));
        return isRed ? "Blue" : "Red";
    }

    @Override
    protected void drawContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (te == null) return;
        int cx = width / 2 - 150;
        int cy = height / 2 - 105;
        g.drawString(font, Component.translatable("dp.gui.capture.title"), cx + 10, cy + 10, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("dp.gui.capture.radius"), cx + 15, cy + 35, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("dp.gui.capture.time"), cx + 15, cy + 63, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("dp.gui.capture.name"), cx + 15, cy + 91, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("dp.gui.score.interval"), cx + 155, cy + 35, 0x55FF55, false);
        g.drawString(font, Component.translatable("dp.gui.score.count"), cx + 155, cy + 63, 0x55FF55, false);
        g.drawString(font, Component.translatable("dp.gui.score.min"), cx + 155, cy + 91, 0x55FF55, false);
        g.drawString(font, Component.translatable("dp.gui.score.max"), cx + 155, cy + 119, 0x55FF55, false);
        g.fill(cx + 148, cy + 25, cx + 149, cy + 135, 0x55FFFFFF);
        String owner = te.getOwnerTeam() != null ? te.getOwnerTeam()
            : Component.translatable("dp.gui.capture.neutral").getString();
        g.drawString(font, Component.translatable("dp.gui.capture.owner").getString() + " " + owner,
            cx + 15, cy + 125, 0xAAAAFF, false);
    }
}
