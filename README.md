# ARC Overhaul

[中文](README.zh-CN.md)

A standalone gameplay MOD, separate from Acbric. **0.1.0-dev.4** implements starting cities, towns, cash and research points for **human empires only**. AI starting counts and cash rules remain vanilla, although changes in placement and random draws can alter AI positions and asset combinations for the same seed.

## Use

Requires Acbric API **0.3.3-dev.21 or a later compatible version** and Java 21. Exit the game and place `build/libs/ARC-Overhaul-0.1.0-dev.4.jar` in its `game/mods/`. Disable the old `acbric-starting-cities.jar` and restart first; the MODs declare a conflict. Do not install the dev.1 scaffold or fixture JARs.

Open **single-player conquest setup → scroll the settings list to the bottom → ARC Overhaul: Player starting options**, or **MOD list → ARC Overhaul → Details**. Edit, Apply, close, then start a new campaign. English/Chinese follow the game language.

| Field | Custom range | Meaning of `-1` |
|---|---|---|
| Cities | 1–4 | Vanilla: one city |
| Towns | 0–8 | Current map default |
| Cash | 0–1,000,000 | Vanilla balance after difficulty and initial assets |
| Research | 0–10,000,000 | `0` grants nothing (vanilla) |

The first three default to `-1`, research defaults to `0`, and they work independently. Zero cities is rejected on Apply; zero cash is valid. Cash is assigned after initial buildings/ships and does not increase their budget. Counts affect native asset allocation and balance.

Research points are banked into the native *unassigned* research pool (`Empire.unassignedResearchPoints`), which the game pours into the first research you select. Writing them into `Empire.researchPoints` instead does not work: the native "select research" command overwrites that field with the new tech's partial progress (0 for a fresh tech) before adding the pool, so the grant would be silently discarded on the first click. The range is in the millions on purpose — a technology costs `BASE_RESEARCH_COST × RESEARCH_COST_MULTIPLIER × 5600 × 1.4^tier` points (`Tech.baseCost`), about 2,240,000 for the first tiers, so a smaller grant is invisible on the progress bar.

ARC-added settlements are never placed on a speck of land: the native spot finder accepts any non-water tile, so ARC re-rolls until the tile belongs to a connected landmass of at least `max(32, gridSize/4)` tiles. Retries are bounded and fall back to the last candidate, so the guard can never turn generation into a failure. Only human empires, and only when the counts are customised; AI and default settings keep the untouched native result.

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

Output: `build/libs/ARC-Overhaul-0.1.0-dev.4.jar`. Replace the previous ARC JAR instead of installing both. Game, framework and test classes are not bundled. Nothing is automatically installed. `build` checks compilation/packaging; its default `test` has no sources. Run [integration tests](TESTING.md#run-isolated-checks-windows) separately with your own game inputs.

PR #1 fixes missing territory colour for extra towns, with manual confirmation from the user. Both game builds (1.2.15.2 / 1.2.14) pass 407 targeted checks each, 814 total, using API dev.25: real Fabric transformation, native placement/contiguous IDs, territory tracing, one-time cash, configuration and native disk/binary-state persistence. Arms/background/land resources use fixtures and territory tracing uses synthetic ownership grids; full map/roads/initial-assets generation, GPU and multiplayer are not comprehensively verified. See [testing](TESTING.md).

## Layout

- `src/main/java/net/poosh/arc/`: owned source, to be split into feature packages.
- `src/main/resources/fabric.mod.json`: identity and entrypoint.
- `build/`: regenerable outputs.
- [Source investigation and design boundaries](RESEARCH.md): implementation evidence and legacy comparison.

Gameplay lives in `conquest/`; native adapters live in `mixin/`. Keep future gameplay modules separate from the framework.

## Contribute and license

[Contributor guide](CONTRIBUTING.md) · [Changes](CHANGELOG.md) · [MIT License](LICENSE) · [Third-party notices](THIRD_PARTY_NOTICES.md)

The product name is **ARC Overhaul** (Chinese: **ARC 大修**); suggested remote repository name: `ARC-Overhaul`. The stable MOD ID remains `arc_overhaul`. This is an independent fan MOD, not the game or an official expansion. Git contains owned source/docs/test source and build tooling; it excludes game files, local dependencies and runtime/build outputs. No remote is configured by this local setup.
