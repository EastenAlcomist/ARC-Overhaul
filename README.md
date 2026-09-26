# ARC Overhaul

[中文](README.zh-CN.md)

A standalone gameplay MOD, separate from Acbric. **0.1.0-dev.3** implements starting cities, towns and cash for **human empires only**. AI starting counts and cash rules remain vanilla, although changes in placement and random draws can alter AI positions and asset combinations for the same seed.

## Use

Requires Acbric API **0.3.3-dev.21 or a later compatible version** and Java 21. Exit the game and place `build/libs/ARC-Overhaul-0.1.0-dev.3.jar` in its `game/mods/`. Disable the old `acbric-starting-cities.jar` and restart first; the MODs declare a conflict. Do not install the dev.1 scaffold or fixture JARs.

Open **single-player conquest setup → scroll the settings list to the bottom → ARC Overhaul: Player starting options**, or **MOD list → ARC Overhaul → Details**. Edit, Apply, close, then start a new campaign. English/Chinese follow the game language.

| Field | Custom range | Meaning of `-1` |
|---|---|---|
| Cities | 1–4 | Vanilla: one city |
| Towns | 0–8 | Current map default |
| Cash | 0–1,000,000 | Vanilla balance after difficulty and initial assets |

All three default to `-1` and work independently. Zero cities is rejected on Apply; zero cash is valid. Cash is assigned after initial buildings/ships and does not increase their budget. Counts affect native asset allocation and balance.

Count limits are conservative. Insufficient space aborts generation instead of silently producing fewer settlements. More than 128 MiB of additional distance-array capacity is rejected; this is not a total game-memory limit.

## Saves and multiplayer

- Preferences live in `game/config/arc_overhaul/conquest-start.json`. Acbric freezes shared rules for new campaigns; later preference changes do not affect existing campaigns.
- This version targets new campaigns. Old saves without ARC rules fail with `RULE_MISSING`; no automatic conversion occurs. Disable ARC and restart to play old saves. No ARC-specific migration tool is included.
- Keep ARC installed for ARC campaigns. Loading/restoring never redistributes land or grants starting cash. Expanded ID capacity uses the technical `arcStartingLayout` save field.
- Multiplayer uses existing rule checks, not host configuration broadcasting. All peers must set matching values through MOD Details before joining. No lobby editor is added; full multiplayer generation/resume remains unverified. Test single-player first.
- Other world-generation MODs may conflict. Building has not changed the framework or the installed game.

## Build

Use JDK 21 and a matching Acbric API dev.21 build. See [contributor setup](CONTRIBUTING.md) for exact dependencies and local paths. The framework baseline is commit `1be89b2`; it may not yet be available on the upstream default branch.

```powershell
.\gradlew.bat build
# Override the default sibling Acbric checkout / its game libs:
.\gradlew.bat build -PacbricDir="D:/Development/Acbric" -PgameLibDir="D:/Games/Airships/libs"
```

Output: `build/libs/ARC-Overhaul-0.1.0-dev.3.jar`. Replace the previous ARC JAR instead of installing both. Game, framework and test classes are not bundled. Nothing is automatically installed. `build` checks compilation/packaging; its default `test` has no sources. Run [integration tests](TESTING.md#run-isolated-checks-windows) separately with your own game inputs.

Both game builds (1.2.15.2 / 1.2.14) pass 71 targeted checks each, 142 total: real Fabric transformation, native placement/IDs, one-time cash, configuration and native disk/binary-state persistence. Arms, backgrounds and land resources use test fixtures; full map/roads/assets generation, GPU and multiplayer remain unverified. See [testing](TESTING.md).

## Layout

- `src/main/java/net/poosh/arc/`: owned source, to be split into feature packages.
- `src/main/resources/fabric.mod.json`: identity and entrypoint.
- `build/`: regenerable outputs.
- [Source investigation and design boundaries](RESEARCH.md): implementation evidence and legacy comparison.

Gameplay lives in `conquest/`; native adapters live in `mixin/`. Keep future gameplay modules separate from the framework.

## Contribute and license

[Contributor guide](CONTRIBUTING.md) · [Changes](CHANGELOG.md) · [MIT License](LICENSE) · [Third-party notices](THIRD_PARTY_NOTICES.md)

The product name is **ARC Overhaul** (Chinese: **ARC 大修**); suggested remote repository name: `ARC-Overhaul`. The stable MOD ID remains `arc_overhaul`. This is an independent fan MOD, not the game or an official expansion. Git contains owned source/docs/test source and build tooling; it excludes game files, local dependencies and runtime/build outputs. No remote is configured by this local setup.
