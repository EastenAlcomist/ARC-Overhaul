# Phase one: conquest starting options investigation

2026-09-26. dev.2 implements the feature with 71 targeted checks per game build. Full world generation, initial assets, graphical UI and multiplayer remain unverified; signatures and partial tests are not a compatibility guarantee.

## Confirmed semantics

The user explicitly chose starting cities, towns and cash per human empire. AI uses vanilla counts/cash rules. These are not map-wide totals and do not change the number of empires. Random-draw changes can still affect AI positions and assets.

Cash means the spendable balance after initial buildings/ships, not their budget. Each setting independently accepts -1 for vanilla. Custom ranges: cities 1–4, towns 0–8, cash 0–1000000.

## Source locations

Reference: a privately retained decompilation of the Steam 1.2.15.2 build. Approximate lines below belong to that snapshot and will differ with decompiler/version. Game sources and research binaries are not part of this repository; method/class names identify the integration points.

| Source | Approximate lines | Role |
|---|---:|---|
| `asplit-A/com/zarkonnen/airships/GameSetupScreen.java` | 137, 167, 188 | Input, startGame and campaign construction. |
| Same file | 195, 257, 329, 341 | Rendering, settings scroll area, getHeight and draw; account for added controls in scroll height. |
| `asplit-B/com/zarkonnen/airships/WorldGenScreen.java` | 32, 43 | Loading/progress screen and map.doSetup driver, not configuration UI. |
| `asplit-B/com/zarkonnen/airships/WorldMap.java` | 237, 255 | Private generation stages; distance-array capacity derives from empire and town counts. |
| Same file | 285–381 | Creates one empire and one full city per iteration. Initial setupInfos entries identify human empires. |
| Same file | 383–419 | Round-robin town placement. Failed placement may be skipped. |
| Same file | 435–451 | Ritual-site selection indexes a minimum number of towns without a count guard. Zero/few towns need explicit handling. |
| Same file | 660–682, 3389 | Territory distance arrays and city cache use city IDs; adding/removing cities late is unsafe. |
| Same file | 700–710 | Difficulty budget, CitySetup.setup and final cash. |
| Same file | 1417–1418 | setupPlayer immediately precedes first autosave. |
| Same file | 3956, 4170, 4194 | Generation driver, new-world constructor and randomized initial asset budget. |
| `asplit-A/com/zarkonnen/airships/MapSize.java` | 12–22 | Final fields for dimensions/empires/towns; default two towns per empire. |
| `asplit-A/com/zarkonnen/airships/CitySetup.java` | 32–44 | Starting asset allocation depends on full-city/town counts. |
| `asplit-B/com/zarkonnen/airships/StrategicLobbyScreen.java` | 832, 933 | Multiplayer settings message and campaign creation. Single-player UI changes do not implement host-setting broadcast. |

Flow: `GameSetupScreen.startGame → CampaignWorld/WorldMap construction → WorldGenScreen → staged WorldMap.doSetup → setupPlayer → autosave → StrategicScreen`.

## Legacy reference

The relevant reference is `acbric-starting-cities.jar`: ID `acbric_starting_cities`, version 0.3.1, defaults 2 cities/3 towns. SHA256: `4916572246d4781841713ef7601b4e8c08dadf6724ab72de8bd7be151a023165`. A different City Upgrade MOD handles building upgrade chains and is not this feature. Legacy binaries/decompilations remain outside this repository; the implementation here is maintained as independent source.

The old MOD increases global townsPerEmpire to at least towns + cities - 1, affecting AI and subsequent campaigns; it never reduces that capacity. On StrategicScreen construction it converts human towns, excluding ritual sites, and increases income to at least 30 or twice its old value. This occurs after initial assets and first autosave, and may repeat on later screen construction. Fixed-position plus/minus buttons have no upper limit. Optional multiplayer hook names and local static settings do not establish synchronization. Capacity, skipped ritual towns and placement failures mean entered counts are not exact. No cash setting exists.

## Implementation and boundaries

ARC keeps gameplay/configuration in conquest/ and native adapters in mixin/. The framework remains unchanged. An entry at the bottom of the single-player setup scroll area opens framework numeric controls; MOD Details offers the same editor. No lobby editor is added.

Acbric freezes rules before generation. The first doSetup creates a per-map MapSize capacity copy without changing global Loadables, map dimensions or empire count. All native distance arrays, nest IDs and stage sizes use this capacity. WorldMap$3 skips unused AI/player slots and changes the first human slots into cities at the native income call, before land/buildings are generated. City income uses the native function. PR #1 assigns contiguous IDs to newly created settlements at placement return, preventing gaps from terminating native territory tracing. IDs remain vanilla when no slots are skipped; existing saves are not renumbered. WorldMap$4 caps ritual sites to available towns. Failed placement aborts; CREATED rechecks counts and sets cash before autosave. No StrategicScreen/load/restore grants are installed.

Configuration/UI/shared rules reuse public APIs. A technical `arcStartingLayout={version:1,slots:N}` field restores native ID capacity early in WorldMap deserialization, before normal post-construction MOD storage is available; it is not gameplay configuration and never reads local candidates. Native OutPipe/state and disk saves are checked. Old saves missing ARC rules are rejected, not automatically adopted. Multiplayer requires identical settings set beforehand; full sessions remain unverified. No new framework interfaces are introduced. Metadata rejects the legacy Starting Cities MOD.

Test vanilla-default equivalence, count bounds/zero towns, insufficient space, consecutive worlds, exact cash, save/load without repeated application, bilingual UI/scroll/Enter behavior and any multiplayer paths explicitly supported. Do not claim universal compatibility with map sizes or other world-generation MODs.

## Evidence so far

The feature builds offline with JDK 21 / Gradle 8.13. Historical evidence for dev.1/dev.2 is privately retained by the maintainer; the original dev.3 baseline passed 71 checks per game build. After merging PR #1, contiguous-ID/tracing regressions pass 407 checks per build with API dev.25, and the user confirmed the territory-colour fix manually. See [TESTING](TESTING.md) for reproduction, test fixtures, synthetic ownership grids and limits on full terrain/roads/initial-assets/GPU/multiplayer coverage. Results are written to `build/runtime-tests/<tag>/summary.json`.
