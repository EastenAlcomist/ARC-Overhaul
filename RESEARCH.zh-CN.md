# 第一阶段：征服开局设置源码定位

2026-09-26。dev.2 已实现首个功能，两版游戏各通过 71 项定向检查；完整地图生成、初始资产、GUI 和联机待验收。本报告保留静态定位，不把签名或局部测试视为完整兼容保证。

## 已确认的含义

用户已明确选择：每个人类势力开局拥有的城市、城镇和现金；AI 保持原版数量与现金规则。不是全地图总数，也不改变势力数量。修改地图生成仍会改变随机过程，因此不保证 AI 坐标/资产与原版同种子逐项一致。

金钱采用“初始建筑/舰船配置结束，玩家进入地图时可用的现金”。三项独立使用 -1 表示原版。当前自定义范围：城市 1–4、城镇 0–8、现金 0–1000000。

## 原版调用链与定位

参考为维护者私有保存的 Steam 1.2.15.2 反编译快照。下表行号仅属于该快照，随反编译工具和版本改变；游戏源码与研究二进制不进入仓库，方法/类名用于定位接入点。

| 位置 | 行号附近 | 职责 |
|---|---:|---|
| `asplit-A/com/zarkonnen/airships/GameSetupScreen.java` | 137、167、188 | 开局界面输入、`startGame()` 和创建 `CampaignWorld`；随后切换到 `WorldGenScreen`。 |
| 同文件 | 195、257、329、341 | 绘制开局页、设置滚动区域、`getHeight()`、设置内容 `draw()`。新增控件必须增加滚动高度，不覆盖原生“开始”按钮。 |
| `asplit-B/com/zarkonnen/airships/WorldGenScreen.java` | 32、43 | 只负责生成进度显示；`input()` 驱动 `w.map.doSetup()`。不是开局设置界面。 |
| `asplit-B/com/zarkonnen/airships/WorldMap.java` | 237、255 | 私有 `SetupStage[]`；按 `size.empires * (1 + size.townsPerEmpire)` 分配 `approxCityDist`。 |
| 同文件 | 285–381 | 主城生成阶段，原版每次创建一个势力和一座非城镇城市；`setupInfos` 前几项对应人类玩家。 |
| 同文件 | 383–419 | 城镇阶段，轮流为势力生成城镇；找不到位置时会跳过，配置数量不天然等于实际落地数量。 |
| 同文件 | 435–451 | 收集城镇并选取祭坛，直接按 `max(2, empires/3+1)` 访问列表；允许零城镇或太少城镇需要保护边界。 |
| 同文件 | 660–682、3389 | 领地距离缓存按城市 ID 索引；`getCity()` 会建立城市缓存。不能生成结束后随意加删领地。 |
| 同文件 | 700–710 | “Drawing borders”阶段先添加难度预算，再调用 `CitySetup.setup`，最后追加最终现金。 |
| 同文件 | 1417–1418 | `setupPlayer()` 之后马上首次自动存档；开局最终余额必须早于该存档落定。 |
| 同文件 | 3956、4170、4194 | 阶段驱动、新地图构造、原始预算 `2500 + random(500)`；另加科技阶段预算。 |
| `asplit-A/com/zarkonnen/airships/MapSize.java` | 12–22 | 地图尺寸、势力数、每势力城镇数是 Loadable 的 final 字段；默认城镇数 2。 |
| `asplit-A/com/zarkonnen/airships/CitySetup.java` | 32–44 | 城市/城镇数量影响资产分配预算，不能跳过配置流程只设置余额。 |
| `asplit-B/com/zarkonnen/airships/StrategicLobbyScreen.java` | 832、933 | 联机参数发送和新战役构造；仅加入单人开局 UI 不等于实现房主设置广播。 |

主线：`GameSetupScreen.startGame → CampaignWorld/WorldMap 构造 → WorldGenScreen → WorldMap.doSetup 多阶段 → setupPlayer → 首次自动存档 → StrategicScreen`。

## 旧 MOD 对照

对应参考文件为 `acbric-starting-cities.jar`，MOD ID `acbric_starting_cities`，版本 0.3.1，内置默认 2 城市/3 城镇。SHA256：`4916572246d4781841713ef7601b4e8c08dadf6724ab72de8bd7be151a023165`。另一份 City Upgrade MOD 实现已有城市内的建筑升级链，不属于此开局功能。旧二进制与反编译材料保留在仓库外，本项目实现独立维护。

- `MapSizeMixin` 与开局按钮钩子把全局 `townsPerEmpire` 向上调整到至少 `towns + cities - 1`；不向下降，影响 AI，可能污染下一局。
- `StrategicScreenMixin` 在战略界面构造后把人类城镇转为城市，跳过祭坛，收入至少 30 或翻倍。此时初始建筑/舰船及首次自动存档已经完成；再次进入战略界面可能再次补转换。
- 旧 UI 固定屏幕坐标、加减按钮，无上限，双语文字并列。联机钩子为可选方法名，配置使用本地静态值，没有共享规则广播/一致性保证。
- 旧城镇数并非精确结果：全局容量只增不减，祭坛跳过及原生选址失败也会导致实际数量不符。旧 MOD 没有现金设置。

## 实际接入与边界

1. `conquest/` 保存玩法/配置，`mixin/` 保存适配，框架本体未改。
2. 单人 GameSetupScreen 设置列表底部增加入口及滚动高度；框架设置页编辑三个字段。没有另加大厅界面，MOD 详情也可编辑。
3. Acbric 生成前冻结规则；第一次 doSetup 创建本地图独立 MapSize 容量副本，保留地图名称/大小/势力数。所有使用 townsPerEmpire 的原生距离、巢穴 ID 和生成阶段自然使用同一容量。全局 Loadable 不被修改。
4. WorldMap$3 原生城镇阶段按槽位跳过多余 AI/玩家位置，在原生收入调用处把前几个玩家槽位设置为城市，使用原生城市收入，早于土地/建筑生成。PR #1 改为在原生放置返回时给实际新建的定居点连续编号，避免空 ID 导致原生领土描边提前结束；默认无跳过槽位时保持原生 ID。仅影响新地图，不重排已有存档。
5. WorldMap$4 将祭坛数限制为可用城镇数。空间不足时提前失败；CREATED 再核对人类领地数，然后设置现金，早于首次自动存档。不挂战略界面构造或加载/恢复事件。
6. 配置/UI/共享规则复用公开 API。为在 WorldMap 反序列化早期恢复原生容量，额外保存 `arcStartingLayout={version:1,slots:N}` 技术字段；它不是玩法配置，不读本机候选值。原生 OutPipe/JSON 重建及磁盘保存均有定向检查。
7. 第一版只面向新战役；旧档缺少 ARC 规则由框架拒绝，不自动接纳。联机需各方预先手动设相同值，完整生成与续局待验收。

未新增框架通用接口。旧 Starting Cities 已在 metadata 中声明冲突；其他世界生成 MOD 不保证兼容。详细操作和验收见 README / TESTING 双语文档。

## AI 舰队（ConstructionStrategy）

2026-09-27。游戏里说的「AI 舰队」就是 `ConstructionStrategy`——AI Fleet Creator 工具产出的那个类。它按等级保存 `shipNames`、`landshipNames`、`buildingNames` 与升级序列，还带 `techs`、`charges`/`requiredCharge`、`minDifficultyLevel`/`maxDifficultyLevel` 和 `enabled` 开关。`Loadable.all(ConstructionStrategy.class)` 就是当前实际加载的目录；舰队包自带一个 `ConstructionStrategy/` 数据目录，所以这份列表会随启用的 MOD 变化。

原生在哪里分配：

| 位置 | 说明 |
|---|---|
| `WorldMap$2.run(int, WorldMap)` | 每次调用创建一个势力的 `SetupStage`。它构造 `ArrayList<ConstructionStrategy> strategies = ConstructionStrategy.forCharge(arms.getActiveCharge(), map.difficulty)`，再把 `strategies.get(map.r.nextInt(strategies.size()))` 交给 `Empire` 构造器。 |
| `ConstructionStrategy.forCharge(Charge, DifficultyLevel)` | 对目录做三趟过滤，每趟都要求 `enabled` 与难度区间；第一趟还要求军徽图案匹配（`charges.contains(charge)`，或 `requiredCharge == charge`），第二趟接受 `requiredCharge` 为空的舰队；结果为空时原生 `nextInt(0)` 会直接抛异常。 |
| `Empire.constructionStrategy` | `public final`，在构造器里赋值——势力一旦创建就无法再换舰队。 |
| `AIConstructionUtils`、`CityAI`、`StrategicAI` | 运行期读这个字段决定 AI 造什么、研究什么。 |

`run` 里一共有四个 `ArrayList.get`：城市名（偏移 312）、舰队列表（420）、科技选项（786）、英雄（1340）。它们是逐字节相同的 `INVOKEVIRTUAL java/util/ArrayList.get:(I)Ljava/lang/Object;`，所以 `@Redirect` 必须靠 `ordinal` 定位；ARC 用 `ordinal = 1`，并在处理函数里做类型守卫，将来序号漂移只会退回原版行为，而不会把别的列表当成舰队。仓库内的探针会解析发行版类文件，同时断言数量（四个）和位置（唯一被 `checkcast ConstructionStrategy` 跟随的是第 1 个）。

舰队设置与开局设置分开存放，但一起随战役固化：Acbric 的共享规则对每个 MOD ID 只允许注册一次，因此 ARC 把两者合并成同一份 JSON 载荷。规则版本保持 1，缺少 `fleets` 键的载荷按「全部允许」读取，此前保存的战役仍可加载。

## 已完成的验证

JDK 21 / Gradle 8.13 离线构建成功。dev.1/dev.2 历史证据由维护者私有保存，dev.3 原始基线为两版各 71 项。PR #1 合并后的连续 ID/描边回归在 API dev.25、两版游戏上各通过 407 项；用户确认缺色修复实机正常。复现入口、测试替身、合成归属网格及完整地形/道路/初始资产/GPU/联机覆盖边界见[测试手册](TESTING.zh-CN.md)，结果写入 `build/runtime-tests/<tag>/summary.json`。
