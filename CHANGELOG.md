# ARC Overhaul changes

[中文](CHANGELOG.zh-CN.md)

## Unreleased

- Fix extra starting towns losing their territory colour on the strategic map. Skipping unused placement slots left gaps in the native settlement ID sequence, and the native territory tracer (`ShapeUtils.cityOwnershipAreas`) walks IDs upwards from 0 and exits permanently at the first ID with no owning cell — so every settlement above that gap never received a territory area and was never painted. Settlement IDs are now assigned contiguously as they are created. When no placement slot is skipped the sequence is identical to vanilla, so maps using the default counts are unaffected.
- Validation for this change: reasoned plus reproduced against a standalone copy of the native tracer; **not** compiled or run against game 1.2.15.2 / 1.2.14 because no matching `0.3.3-dev.21` framework build was available. Details in the pull request.

## 0.1.0-dev.3 — 2026-09-26

- Prepare the standalone Git project for collaboration under **ARC Overhaul**, Chinese **ARC 大修**. Gradle project/artifact prefix: `ARC-Overhaul`. Keep MOD ID `arc_overhaul`, packages and save/config contracts unchanged.
- Adopt MIT for owned code/docs; retain Gradle wrapper notices. Include the project license in the MOD JAR.
- Add bilingual contributor guidance, update install/test/research documents, add a PR template and repository/editor rules. Exclude games, local dependencies, fixtures and build outputs.
- Allow external game-library paths in Gradle. Replace private-workspace paths in the Windows integration runner with `--game`, `--acbric-dir`, `--java-home`, `--arc-jar` and `--output-root`. Record input hashes and reject existing run directories.
- No gameplay changes. Build passed with external game libraries; the parameterized runner passed **71 checks for 1.2.15.2 and 71 for 1.2.14**. Baseline API: `0.3.3-dev.21`, Acbric `1be89b2`. No full world/roads/initial-assets/GPU/multiplayer verification. Game 1.2.15.3 is not covered by these results.

Validated MOD JAR SHA256: `e0091f84ec880298589f801f9eaa2f40421566408d55dd2872f1d2a474afdd7a`. API JAR SHA256: `12abec546029568b47d8902b3479056df816d81d1b3a7b76269154e9be7b1fcb`. Local summary: `build/runtime-tests/repo-init/summary.json`; logs and copied game inputs are not distributed. Rebuilding may change ZIP metadata and its hash.

## 0.1.0-dev.2 — 2026-09-26

Implement per-human conquest starting cities, towns and cash, bilingual setup/MOD-details controls, frozen campaign rules, early saved layout restoration and legacy Starting Cities conflict. AI count/cash rules remain vanilla. Initial validation: 71 targeted checks per game build; full gameplay acceptance remains separate. See [research](RESEARCH.md) and [testing](TESTING.md).
