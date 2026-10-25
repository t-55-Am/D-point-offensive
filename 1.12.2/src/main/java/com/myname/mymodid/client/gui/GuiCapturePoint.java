package com.myname.mymodid.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import com.myname.mymodid.MyMod;
import com.myname.mymodid.client.ClientEventHandler;
import com.myname.mymodid.inventory.ContainerCapturePoint;
import com.myname.mymodid.network.PacketCapturePointConfig;
import com.myname.mymodid.network.PacketCapturePointFlags;
import com.myname.mymodid.tileentity.TileEntityCapturePoint;

public class GuiCapturePoint extends GuiContainer {
    private TileEntityCapturePoint te;
    private GuiTextField radiusField;
    private GuiTextField timeField;
    private GuiTextField nameField;
    private GuiTextField pveIntervalField;
    private GuiTextField pveCountField;
    private GuiTextField pveMinField;
    private GuiTextField pveMaxField;
    private GuiButton btnOwner;
    private GuiButton btnBase;
    private GuiButton btnRevive;

    public GuiCapturePoint(InventoryPlayer inv, TileEntityCapturePoint te) {
        super(new ContainerCapturePoint());
        this.te = te;
        this.xSize = 300;
        this.ySize = 210;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        int cx = (width - xSize) / 2;
        int cy = (height - ySize) / 2;

        // ===== 左列：基础设置 =====
        radiusField = new GuiTextField(0, fontRenderer, cx + 80, cy + 30, 60, 18);
        radiusField.setMaxStringLength(3);
        radiusField.setText(String.valueOf(te.getRadius()));

        timeField = new GuiTextField(0, fontRenderer, cx + 80, cy + 58, 60, 18);
        timeField.setMaxStringLength(4);
        timeField.setText(String.valueOf(te.getCaptureTime()));

        nameField = new GuiTextField(0, fontRenderer, cx + 80, cy + 86, 60, 18);
        nameField.setMaxStringLength(20);
        nameField.setText(te.getPointName());

        // ===== 右列：PvE僵尸参数 =====
        pveIntervalField = new GuiTextField(0, fontRenderer, cx + 220, cy + 30, 60, 18);
        pveIntervalField.setMaxStringLength(4);
        pveIntervalField.setText(String.valueOf(te.getPveSpawnInterval()));

        pveCountField = new GuiTextField(0, fontRenderer, cx + 220, cy + 58, 60, 18);
        pveCountField.setMaxStringLength(3);
        pveCountField.setText(String.valueOf(te.getPveSpawnCount()));

        pveMinField = new GuiTextField(0, fontRenderer, cx + 220, cy + 86, 60, 18);
        pveMinField.setMaxStringLength(3);
        pveMinField.setText(String.valueOf(te.getPveMinCount()));

        pveMaxField = new GuiTextField(0, fontRenderer, cx + 220, cy + 114, 60, 18);
        pveMaxField.setMaxStringLength(3);
        pveMaxField.setText(String.valueOf(te.getPveMaxCount()));

        // ===== 底部按钮（居中） =====
        buttonList.clear();
        buttonList.add(new GuiButton(0, cx + 110, cy + 145, 80, 20, net.minecraft.client.resources.I18n.format("dp.gui.capture.save")));
        btnBase = new GuiButton(1, cx + 110, cy + 170, 80, 20,
            net.minecraft.client.resources.I18n.format("dp.gui.capture.base") + " " + (te.isMainBase() ? net.minecraft.client.resources.I18n.format("dp.gui.on") : net.minecraft.client.resources.I18n.format("dp.gui.off")));
        buttonList.add(btnBase);
        btnRevive = new GuiButton(2, cx + 20, cy + 170, 80, 20,
            net.minecraft.client.resources.I18n.format("dp.gui.capture.revive") + " " + (te.isReviveMode() ? net.minecraft.client.resources.I18n.format("dp.gui.on") : net.minecraft.client.resources.I18n.format("dp.gui.off")));
        buttonList.add(btnRevive);

        // 归属按钮：只有大基地才显示
        btnOwner = new GuiButton(3, cx + 200, cy + 170, 80, 20, ownerLabel());
        btnOwner.visible = te.isMainBase();
        buttonList.add(btnOwner);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int mx, int my) {
        // 取消白色底板，不画任何背景
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        // 标题
        fontRenderer.drawString(net.minecraft.client.resources.I18n.format("dp.gui.capture.title"), 10, 10, 0xFFFFFF);

        // 左列标签
        fontRenderer.drawString(net.minecraft.client.resources.I18n.format("dp.gui.capture.radius"), 15, 35, 0xFFFFFF);
        fontRenderer.drawString(net.minecraft.client.resources.I18n.format("dp.gui.capture.time"), 15, 63, 0xFFFFFF);
        fontRenderer.drawString(net.minecraft.client.resources.I18n.format("dp.gui.capture.name"), 15, 91, 0xFFFFFF);

        // 右列标签（绿色区分PvE参数）
        fontRenderer.drawString(net.minecraft.client.resources.I18n.format("dp.gui.score.interval"), 155, 35, 0x55FF55);
        fontRenderer.drawString(net.minecraft.client.resources.I18n.format("dp.gui.score.count"), 155, 63, 0x55FF55);
        fontRenderer.drawString(net.minecraft.client.resources.I18n.format("dp.gui.score.min"), 155, 91, 0x55FF55);
        fontRenderer.drawString(net.minecraft.client.resources.I18n.format("dp.gui.score.max"), 155, 119, 0x55FF55);

        // 分隔线
        drawRect(148, 25, 149, 135, 0x55FFFFFF);

        // 归属信息
        String owner = te.getOwnerTeam() != null ? te.getOwnerTeam() : net.minecraft.client.resources.I18n.format("dp.gui.capture.neutral");
        fontRenderer.drawString(net.minecraft.client.resources.I18n.format("dp.gui.capture.owner") + " " + owner, 15, 125, 0xAAAAFF);
    }

    @Override
    public void drawScreen(int mx, int my, float f) {
        // 半透明黑色背景（替代默认底板）
        drawDefaultBackground();
        super.drawScreen(mx, my, f);
        radiusField.drawTextBox();
        timeField.drawTextBox();
        nameField.drawTextBox();
        pveIntervalField.drawTextBox();
        pveCountField.drawTextBox();
        pveMinField.drawTextBox();
        pveMaxField.drawTextBox();
    }

    @Override
    protected void keyTyped(char c, int key) throws java.io.IOException {
        if (radiusField.isFocused()) radiusField.textboxKeyTyped(c, key);
        else if (timeField.isFocused()) timeField.textboxKeyTyped(c, key);
        else if (nameField.isFocused()) nameField.textboxKeyTyped(c, key);
        else if (pveIntervalField.isFocused()) pveIntervalField.textboxKeyTyped(c, key);
        else if (pveCountField.isFocused()) pveCountField.textboxKeyTyped(c, key);
        else if (pveMinField.isFocused()) pveMinField.textboxKeyTyped(c, key);
        else if (pveMaxField.isFocused()) pveMaxField.textboxKeyTyped(c, key);
        else super.keyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws java.io.IOException {
        super.mouseClicked(mx, my, btn);
        radiusField.mouseClicked(mx, my, btn);
        timeField.mouseClicked(mx, my, btn);
        nameField.mouseClicked(mx, my, btn);
        pveIntervalField.mouseClicked(mx, my, btn);
        pveCountField.mouseClicked(mx, my, btn);
        pveMinField.mouseClicked(mx, my, btn);
        pveMaxField.mouseClicked(mx, my, btn);
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        if (btn.id == 0) {
            try {
                int r = Integer.parseInt(radiusField.getText().trim());
                int t = Integer.parseInt(timeField.getText().trim());
                String name = nameField.getText().trim();
                int pi = Integer.parseInt(pveIntervalField.getText().trim());
                int pc = Integer.parseInt(pveCountField.getText().trim());
                int pmin = Integer.parseInt(pveMinField.getText().trim());
                int pmax = Integer.parseInt(pveMaxField.getText().trim());
                MyMod.NETWORK.sendToServer(new PacketCapturePointConfig(te.getPos().getX(), te.getPos().getY(), te.getPos().getZ(), r, t, name, pi, pc, pmin, pmax));
            } catch (NumberFormatException ignored) {}
            mc.player.closeScreen();
        } else if (btn.id == 1) {
            boolean v = !te.isMainBase();
            te.setMainBase(v);
            MyMod.NETWORK.sendToServer(new PacketCapturePointFlags(te.getPos().getX(), te.getPos().getY(), te.getPos().getZ(), true, v, false, false));
            btnBase.displayString = net.minecraft.client.resources.I18n.format("dp.gui.capture.base") + " " + (v ? net.minecraft.client.resources.I18n.format("dp.gui.on") : net.minecraft.client.resources.I18n.format("dp.gui.off"));
            btnOwner.visible = v;
        } else if (btn.id == 2) {
            boolean v = !te.isReviveMode();
            te.setReviveMode(v);
            MyMod.NETWORK.sendToServer(new PacketCapturePointFlags(te.getPos().getX(), te.getPos().getY(), te.getPos().getZ(), false, false, true, v));
            btnRevive.displayString = net.minecraft.client.resources.I18n.format("dp.gui.capture.revive") + " " + (v ? net.minecraft.client.resources.I18n.format("dp.gui.on") : net.minecraft.client.resources.I18n.format("dp.gui.off"));
        } else if (btn.id == 3) {
            if (!te.isMainBase()) return;
            MyMod.NETWORK.sendToServer(new PacketCapturePointFlags(te.getPos().getX(), te.getPos().getY(), te.getPos().getZ()));
            te.setOwnerTeam(cycleOwner(te.getOwnerTeam()));
            btnOwner.displayString = ownerLabel();
        }
    }

    private String ownerLabel() {
        String owner = te.getOwnerTeam();
        String disp;
        if (ClientEventHandler.pveEnabled) {
            disp = "PvE".equals(owner) ? net.minecraft.client.resources.I18n.format("dp.gui.capture.zombie")
                : net.minecraft.client.resources.I18n.format("dp.gui.capture.human");
        } else {
            boolean isRed = owner != null && (owner.toLowerCase().contains("red") || owner.contains("红"));
            disp = isRed ? net.minecraft.client.resources.I18n.format("dp.gui.capture.redteam")
                : net.minecraft.client.resources.I18n.format("dp.gui.capture.blueteam");
        }
        return net.minecraft.client.resources.I18n.format("dp.gui.capture.baseowner") + " " + disp;
    }

    private String cycleOwner(String cur) {
        if (ClientEventHandler.pveEnabled) {
            return "PVEHUMAN".equals(cur) ? "PvE" : "PVEHUMAN";
        }
        boolean isRed = cur != null && (cur.toLowerCase().contains("red") || cur.contains("红"));
        return isRed ? "Blue" : "Red";
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }
}
