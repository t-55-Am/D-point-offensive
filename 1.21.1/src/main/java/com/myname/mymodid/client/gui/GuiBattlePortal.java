package com.myname.mymodid.client.gui;

import com.myname.mymodid.network.CPayloads.BattleConfigPayload;
import com.myname.mymodid.network.CPayloads.SaveBackpackPayload;
import com.myname.mymodid.network.CPayloads.TeamRequestPayload;
import com.myname.mymodid.network.Network;
import com.myname.mymodid.score.TeamConfig;
import com.myname.mymodid.score.TeamData;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class GuiBattlePortal extends DpScreen {
    private final int x, y, z;
    private int currentIndex = 0;
    private String editName = "";
    private int editColor = 0;
    private int editSide = 0;
    private int editMax = 10;
    private int[] editSpawn = {0, 64, 0};

    private EditBox nameField, maxField, spawnXField, spawnYField, spawnZField;

    public GuiBattlePortal(net.minecraft.world.entity.player.Player player, BlockPos pos) {
        super(Component.literal("Team Configurator"));
        this.x = pos.getX(); this.y = pos.getY(); this.z = pos.getZ();
    }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;
        int lx = cx - 145;
        locateCurrent();
        clearButtons();

        nameField = new EditBox(font, lx, cy - 44, 130, 16, Component.literal("name"));
        nameField.setMaxLength(16);
        addRenderableWidget(nameField);
        maxField = new EditBox(font, lx + 160, cy - 44, 100, 16, Component.literal("max"));
        maxField.setMaxLength(3);
        addRenderableWidget(maxField);
        spawnXField = new EditBox(font, lx, cy - 22, 130, 16, Component.literal("sx"));
        spawnXField.setMaxLength(6);
        addRenderableWidget(spawnXField);
        spawnYField = new EditBox(font, lx, cy, 130, 16, Component.literal("sy"));
        spawnYField.setMaxLength(6);
        addRenderableWidget(spawnYField);
        spawnZField = new EditBox(font, lx, cy + 22, 130, 16, Component.literal("sz"));
        spawnZField.setMaxLength(6);
        addRenderableWidget(spawnZField);

        addButton(lx, cy - 84, 260, 18, this::teamLabel, () -> {
            if (!TeamData.clientTeams.isEmpty()) {
                currentIndex = (currentIndex + 1) % TeamData.clientTeams.size();
                TeamConfig nc = TeamData.clientTeams.get(currentIndex);
                editName = nc.name;
                editColor = nc.color;
                editSide = nc.side;
                editMax = nc.maxPlayers;
                editSpawn = new int[]{nc.spawnX, nc.spawnY, nc.spawnZ};
                refreshFields();
            }
        });

        addButton(lx, cy - 62, 260, 18, this::colorLabel, () -> {
            editColor = (editColor + 1) % TeamConfig.PALETTE.length;
        });

        addButton(lx, cy + 48, 125, 18,
            () -> Component.translatable("gui.battle.saveop").getString(), () -> {
            TeamConfig c = currentTeam();
            if (c != null) Network.sendToServer(new SaveBackpackPayload(c.name));
        });

        addButton(lx + 135, cy + 48, 125, 18,
            () -> Component.translatable("gui.battle.save").getString(), () -> {
            TeamConfig c = currentTeam();
            if (c != null) {
                saveFields();
                Network.sendToServer(new BattleConfigPayload(0, c.name, editName, editColor, editSide,
                    editMax, editSpawn[0], editSpawn[1], editSpawn[2],
                    editSpawn[0] != 0 || editSpawn[1] != 0 || editSpawn[2] != 0));
            }
        });

        addButton(lx, cy + 70, 125, 18,
            () -> Component.translatable("gui.battle.add").getString(), () -> {
            String nn = nameField.getValue().trim();
            if (!nn.isEmpty()) {
                Network.sendToServer(new BattleConfigPayload(1, nn, "", editColor, 0, 10, 0, 64, 0, false));
            }
        });

        addButton(lx + 135, cy + 70, 125, 18,
            () -> Component.translatable("gui.battle.remove").getString(), () -> {
            TeamConfig c = currentTeam();
            if (c != null) {
                Network.sendToServer(new BattleConfigPayload(2, c.name, "", 0, 0, 10, 0, 64, 0, false));
            }
        });

        refreshFields();
        Network.sendToServer(new TeamRequestPayload());
    }

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
        if (c == null) return Component.translatable("gui.teleporter.none").getString();
        return ChatFormatting.RESET.toString() + String.format("§%c%s", hexChar(TeamConfig.PALETTE[c.color]),
            Component.translatable("gui.battle.team", c.name).getString());
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
        return Component.translatable("gui.battle.color").getString() + " " + colorName(editColor);
    }

    private String colorName(int idx) {
        int i = ((idx % TeamConfig.PALETTE.length) + TeamConfig.PALETTE.length) % TeamConfig.PALETTE.length;
        return Component.translatable(TeamConfig.PALETTE_KEYS[i]).getString();
    }

    private void refreshFields() {
        TeamConfig c = currentTeam();
        if (c == null) {
            nameField.setValue("");
            maxField.setValue("");
            spawnXField.setValue("");
            spawnYField.setValue("");
            spawnZField.setValue("");
            return;
        }
        nameField.setValue(c.name);
        maxField.setValue(String.valueOf(c.maxPlayers));
        spawnXField.setValue(String.valueOf(c.spawnX));
        spawnYField.setValue(String.valueOf(c.spawnY));
        spawnZField.setValue(String.valueOf(c.spawnZ));
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    public void onTeamsSynced() {
        locateCurrent();
        refreshFields();
    }

    private void saveFields() {
        editName = nameField.getValue().trim();
        editMax = parseIntSafe(maxField.getValue(), editMax);
        editSpawn[0] = parseIntSafe(spawnXField.getValue(), editSpawn[0]);
        editSpawn[1] = parseIntSafe(spawnYField.getValue(), editSpawn[1]);
        editSpawn[2] = parseIntSafe(spawnZField.getValue(), editSpawn[2]);
    }

    @Override
    protected void drawContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int cx = width / 2, cy = height / 2;
        int lx = cx - 145;
        String title = ChatFormatting.GOLD + Component.translatable("gui.battle.title").getString();
        g.drawString(font, title, cx - font.width(title) / 2, cy - 102, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("gui.battle.teamName"), lx, cy - 34, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("gui.battle.max"), lx + 160, cy - 34, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("gui.battle.spawnx"), lx, cy - 12, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("gui.battle.spawny"), lx, cy + 10, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("gui.battle.spawnz"), lx, cy + 32, 0xFFFFFF, false);
    }
}
