# ARC Overhaul starting-options test guide

[中文](TESTING.zh-CN.md) · 0.1.0-dev.3 · 2026-09-26

## Install and open

1. Exit the game. Use API dev.21 or later compatible. Disable Starting Cities and restart, or keep its JAR outside `mods/`. Install only one ARC version.
2. Copy `build/libs/ARC-Overhaul-0.1.0-dev.3.jar` to the running copy's `game/mods/`. No installed copy was overwritten.
3. Start a new single-player conquest setup. Scroll its settings list to the bottom and select **ARC Overhaul: Player starting options**, or use **MOD list → ARC Overhaul → Details**.
4. Edit, Apply, close, then use the native Start button. `-1` preserves that field's vanilla value; fields are independent.

## Manual checks

| Scenario | Expected result |
|---|---|
| All -1 | Vanilla one city, map-default towns and cash |
| Cities 3, towns 3, cash 12345 | Exactly those player counts/cash on entering the map; AI follows vanilla counts |
| Cities 2, towns 8 | Every extra player town has territory colour; check AI territory as well |
| Cities 1, towns 0, cash 0 | One city, no towns/cash, no ritual-selection bounds error |
| Cash only, counts -1 | Counts unchanged; initial asset budget is not increased |
| Cities changed, towns -1 | Additional cities plus the map-default number of towns |
| Zero cities, letters, fractions, out-of-range | Rejected in the field or on Apply; previous values remain |
| Cancel / Esc / X after editing | Confirmed discard preserves previous values; Enter does not start a campaign through the editor |
| Small window, scaling, scrolling, focus changes | Visible entry clickable, hidden entry not clickable; fields and close controls work |
| Change game language and reopen | English/Chinese titles, descriptions and controls |
| Custom campaign A, then default campaign B | B uses vanilla counts without global map-setting contamination |
| Spend money/change territory, save/reload A | Actual state restored without starting grants or repeated conversion on strategy-screen entry |
| Restart | Applied preferences persist; damaged files not silently overwritten |
| Insufficient space | Actionable failure, no mismatched partial campaign saved as successful |

Start with a medium map and the first three scenarios, then test minimum/maximum sizes. Record seed, difficulty and values. Changed parameters alter random draws; AI positions and assets need not match another run exactly.

## Saves and multiplayer

Use new test campaigns. Old saves without ARC rules produce `RULE_MISSING`; no automatic conversion occurs. Skip old-save adoption experiments for now. Keep ARC installed for ARC campaigns.

Full multiplayer remains unverified. Optional testing requires matching ARC/API and identical values set through MOD Details before joining. Differences should block preparation; matching rules do not prove full compatibility. Matching JARs do not synchronize configuration. There is no lobby editor.

## Automated coverage and diagnostics

After merging PR #1, game 1.2.15.2 / 1.2.14 each pass **407 checks**, **814 total**, using Acbric API `0.3.3-dev.25` and JDK 21. Local results: `build/runtime-tests/pr1-contiguous-final/summary.json`. The original dev.3 result of 71 checks per build is historical.

The 33 added layouts cover two/four empires, different human positions, extra/reduced/zero towns, maximum counts, defaults/cash-only/explicit vanilla counts, and native AI placement failure. They check counts, types, unique contiguous IDs, city-cache identity, default placement/RNG equivalence, and directly invoke native `ShapeUtils.cityOwnershipAreas` to check that every settlement receives an area. Running the same test against the pre-fix dev.3 JAR fails the new contiguous-ID assertion as expected (local `pr1-negative-old-mod` evidence).

Real Fabric transformations, native placement, CREATED, disk saves and binary state reconstruction run with test-only arms/background/land fixtures. Territory tracing uses separated single-cell ownership fixtures retaining actual settlement IDs, not full territory influence generation. Automated coverage does not include full terrain, roads, initial assets, GPU or multiplayer sessions.

On 2026-09-26 the user confirmed that PR #1's fix passed manual gameplay testing. No version, seed or scenario matrix was supplied; record this as manual confirmation of the territory-colour fix, not completion of every manual or multiplayer case.

Keep native user-data `log.txt` (usually under `AirshipsGame`) and the latest `game/logs/acbric/` startup directory when reporting errors. Include game version, seed, map size, all three values, enabled MODs and steps. Historical maintainer evidence is private; you can reproduce the checks with the committed test source and your own game inputs below.

## Run isolated checks (Windows)

Use Python 3.10+, JDK 21 (`JAVA_HOME` or `--java-home`) and a built ARC Overhaul JAR. See [dependency setup](CONTRIBUTING.md). Each game input must contain `libs/asplit-A.zip`, `libs/asplit-B.zip`, supporting library JARs, and `data/fontmetrics` / `data/lang`. Existing isolated layouts with `game/data` are also accepted. The example paths must be replaced with your local paths.

```powershell
python tests/run_runtime.py --tag first-check --acbric-dir "D:/Development/Acbric" --game "steam=D:/Games/Airships"
# Repeat --game to check a second owned game build in the same run:
python tests/run_runtime.py --tag both-builds --game "steam=D:/Games/Airships" --game "older=D:/Games/Airships-older"
```

Output defaults to `build/runtime-tests/<tag>/`: `summary.json` records counts and MOD/API/game input hashes; each labelled directory has `runtime.log`. Each tag must be new. `--output-root` changes the destination; it must be separate from game inputs. `--arc-jar` selects the artifact if multiple `ARC-Overhaul-*.jar` versions exist in `build/libs`. Check the version separately: labels are chosen by the caller, not detected version numbers.

The harness copies libraries and minimal resources into a fresh directory, isolates `APPDATA`, `user.home` and native user data, and runs only its probe main. It does not open a normal campaign. Test output contains copied game files; never commit or redistribute it. The source checkout needs no maintainer-specific workspace folders. Only the two previously checked game builds have recorded results; a new version requires its own checks and manual validation.
