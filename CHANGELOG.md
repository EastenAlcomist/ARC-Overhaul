# ARC Overhaul changes

[中文](CHANGELOG.zh-CN.md)

## Unreleased

- Fix extra starting towns losing their territory colour on the strategic map. Skipping unused placement slots left gaps in the native settlement ID sequence, and the native territory tracer (`ShapeUtils.cityOwnershipAreas`) walks IDs upwards from 0 and exits permanently at the first ID with no owning cell — so every settlement above that gap never received a territory area and was never painted. Settlement IDs are now assigned contiguously as they are created. When no placement slot is skipped the sequence is identical to vanilla, so maps using the default counts are unaffected.
- Add a **Player starting research points** field (0–10,000,000, default 0 = vanilla) to the ARC starting options. The value is banked into the native *unassigned* research pool (`Empire.unassignedResearchPoints`), which the game pours into the first research the player selects. Granting it through `Empire.researchPoints` does not work: the native set-research command (`CampaignWorld`, bytecode 223–312) overwrites that field with the new tech's partial progress before adding the pool, so the grant was silently discarded on the first click. The range is in the millions because a technology costs `BASE_RESEARCH_COST × RESEARCH_COST_MULTIPLIER × 5600 × 1.4^tier` points (`Tech.baseCost`) — measured in-engine as 1,680,000–2,352,000 for the first tiers; a smaller grant is invisible on the progress bar. The probe asserts the range stays within scale of a real technology. Grantee timing is identical to the existing cash grant (same CREATED callback, same loop) and the grant is logged.
- Keep ARC-added settlements off isolated land. The native spot finder `WorldMap.findMapLocationSpot` only requires the candidate tile to be *not water*, so a single land tile surrounded by sea is a valid result and an added town could be generated on a speck connected to nothing. ARC now wraps that call for human empires and re-rolls until the tile belongs to a connected landmass of at least `max(32, gridSize/4)` tiles; the retry budget is bounded and falls back to the last candidate, so the guard cannot turn generation into a failure. AI empires and default settings keep the untouched native result. Verified with an A/B probe: the unguarded native finder places a settlement on an isolated island for 6 of 16 seeds, and with the guard the same seed lands on real land.
- Validation for this change: game 1.2.15.2, **426 checks pass** with API `0.3.3-dev.21` (the 407 colour-fix checks plus the research, land-guard and scale assertions). Test game 1.2.14 and API dev.25 were not re-run here.
- PR #1 was initially validated using a standalone copy of the native tracer. After merging, add contiguous-ID, city-cache and native territory-tracing regressions: 407 checks each for game 1.2.15.2 / 1.2.14, 814 total, with API `0.3.3-dev.25`. The old dev.3 JAR fails the new assertion as expected. The user confirmed the fix in manual gameplay testing. See [TESTING](TESTING.md) for fixtures, synthetic ownership grids and coverage limits; this does not establish full multiplayer acceptance.

## 0.1.0-dev.3 — 2026-09-26

- Prepare the standalone Git project for collaboration under **ARC Overhaul**, Chinese **ARC 大修**. Gradle project/artifact prefix: `ARC-Overhaul`. Keep MOD ID `arc_overhaul`, packages and save/config contracts unchanged.
- Adopt MIT for owned code/docs; retain Gradle wrapper notices. Include the project license in the MOD JAR.
- Add bilingual contributor guidance, update install/test/research documents, add a PR template and repository/editor rules. Exclude games, local dependencies, fixtures and build outputs.
- Allow external game-library paths in Gradle. Replace private-workspace paths in the Windows integration runner with `--game`, `--acbric-dir`, `--java-home`, `--arc-jar` and `--output-root`. Record input hashes and reject existing run directories.
- No gameplay changes. Build passed with external game libraries; the parameterized runner passed **71 checks for 1.2.15.2 and 71 for 1.2.14**. Baseline API: `0.3.3-dev.21`, Acbric `1be89b2`. No full world/roads/initial-assets/GPU/multiplayer verification. Game 1.2.15.3 is not covered by these results.

Validated MOD JAR SHA256: `e0091f84ec880298589f801f9eaa2f40421566408d55dd2872f1d2a474afdd7a`. API JAR SHA256: `12abec546029568b47d8902b3479056df816d81d1b3a7b76269154e9be7b1fcb`. Local summary: `build/runtime-tests/repo-init/summary.json`; logs and copied game inputs are not distributed. Rebuilding may change ZIP metadata and its hash.

## 0.1.0-dev.2 — 2026-09-26

Implement per-human conquest starting cities, towns and cash, bilingual setup/MOD-details controls, frozen campaign rules, early saved layout restoration and legacy Starting Cities conflict. AI count/cash rules remain vanilla. Initial validation: 71 targeted checks per game build; full gameplay acceptance remains separate. See [research](RESEARCH.md) and [testing](TESTING.md).
