# D-point Offensive

English | [简体中文](./README.md)

A War Thunder-style Minecraft mod: capture points / scoring / team system / voice comms / tactical markers.

- **Supported versions**: Minecraft 1.7.10 (Forge), 1.12.2 (Forge), 1.21.1 (NeoForge)
- **Current version**: v1.0.1 (synchronized across all three)
- **Author**: t_55
- **Textures**: 7tp
- **License**: MIT
- **Languages**: English / Simplified Chinese / Deutsch / Français / Italiano / 日本語 / Русский (7 languages)
- **Download**: [CurseForge](https://www.curseforge.com/minecraft/mc-mods/d-point-offensive) | [mcmod.cn](https://www.mcmod.cn/class/32416.html)

## Features

| Module | Description |
|---|---|
| Capture Point Block | Configurable radius / capture time / name; large-base mode; score drains per second by capture difference |
| Score Controller | PvP (initial score / score drain per second) and PvE (zombie parameters); team deathmatch / multi-team elimination |
| Team Configurator | Create teams freely (default 2, up to 10), free-match team names with team blocks, configure spawn / loadout templates |
| Battle Teleporter | Redstone-triggered: join team + apply loadout template + teleport to spawn; **auto-snapshot inventory before battle, one-click restore** |
| Quick Comm | Press `T` to open, multi-category custom chat commands |
| Settings Page | Press `Y` to open, tabbed: Quick Comm config + HUD config |
| HUD Config | Score bar / compass position, size, toggle all adjustable and saved |
| Voice Comms | Built-in team voice system |
| Shift Tooltip | Hold `Shift` in inventory to see block details |
| Tactical Markers | Press `M` to mark coordinates, HUD shows direction and distance |
| Curios Compatibility | 1.12.2+ reflects against Curios API; auto-skips if Curios not installed |

### Version Matrix

| Feature | 1.7.10 | 1.12.2 | 1.21.1 |
|---|---|---|---|
| Core capture/scoring/teams | ✅ | ✅ | ✅ |
| Pre-battle inventory snapshot & restore | ❌ | ✅ | ✅ |
| Curios accessory compat | ❌ | ✅ | ✅ |
| Frosted-glass modern GUI | ❌ | ❌ | ✅ |
| Orthographic floating name tags | ❌ | ❌ | ✅ |

## Project Structure

```
D-point-offensive/
├── 1.7.10/    # Forge 1.7.10 source (v1.0.1)
├── 1.12.2/    # Forge 1.12.2 source (v1.0.1)
└── 1.21.1/    # NeoForge 1.21.1 source (v1.0.1)
```

## Build

Each version is its own ForgeGradle project:

```bash
# 1.7.10 (JDK 8, Gradle 4.9, ForgeGradle 2.3, reobf)
./gradlew build

# 1.12.2 (JDK 8, Gradle 4.9, ForgeGradle 3)
./gradlew build

# 1.21.1 (JDK 21, NeoForge MDK)
./gradlew build
```

Output jars land in each version's `build/libs/`.

## Notes

- Source code is AI-assisted, not obfuscated.
- Textures by 7tp.
- All three versions iterate in sync.
- Release jars are on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/d-point-offensive) and [mcmod.cn](https://www.mcmod.cn/class/32416.html).
