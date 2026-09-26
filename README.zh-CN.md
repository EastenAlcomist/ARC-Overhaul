# ARC Overhaul（ARC 大修）

[English](README.md)

独立于 Acbric 框架的功能 MOD，按功能逐步扩展。**0.1.0-dev.3** 实现征服开局城市数、城镇数和现金，**仅影响人类势力**；AI 保留原版初始数量与现金规则。地图布局及随机过程改变后，AI 的具体位置、资产组合不保证与原版同种子完全相同。

## 使用

需要 Acbric API **0.3.3-dev.21 或更新的兼容版本**、Java 21。退出游戏，把 `build/libs/ARC-Overhaul-0.1.0-dev.3.jar` 放进运行副本的 `game/mods/`。先停用旧 `acbric-starting-cities.jar` 并重启；两者声明互斥。不要安装 dev.1 骨架或测试夹具 JAR。

入口：**单人征服开局设置 → 将设置列表滚到最底部 → ARC 大修：玩家开局设置**。另可从 **MOD 列表 → ARC Overhaul → 详情** 打开。输入后点击“应用”，关闭窗口再开始新局。中英文跟随游戏语言。

| 字段 | 自定义范围 | `-1` 含义 |
|---|---|---|
| 城市数 | 1–4 | 原版 1 座 |
| 城镇数 | 0–8 | 当前地图默认城镇数 |
| 现金 | 0–1,000,000 | 原版难度及初始资产结算后的余额 |

三个字段默认均为 `-1`，可独立设置。城市数 `0` 无效，应用时拒绝；现金 `0` 表示零现金开局。现金在初始建筑/舰船配置后设置，不提高生成资产的预算。数量影响原生资产分配和玩法平衡。

数量上限暂时保守。位置不足时明确报错，不静默少生成；新增距离数组超过 128 MiB 也会拒绝，此限制不代表整个游戏的内存预算。

## 存档与联机边界

- 配置位于 `game/config/arc_overhaul/conquest-start.json`，经 Acbric 共享规则在新战役开始时冻结，后续改配置不影响已有战役。
- 本版面向新战役。缺少 `arc_overhaul` 规则的旧档会被框架以 `RULE_MISSING` 阻止，不自动转换。玩旧档可停用 ARC 后重启；暂无 ARC 专属迁移工具。
- ARC 新档继续运行须保留 ARC。加载/恢复不重复分地或发钱；扩大 ID 容量的存档带有 `arcStartingLayout` 技术字段。
- 联机参与现有规则检查，但不自动广播房主配置。各方须在入厅前从 MOD 详情设置相同值。没有新增大厅编辑面板；完整联机生成/续局未验收，先测试单人。
- 不保证与其他世界生成 MOD 共存；框架源码、用户运行副本均未被本次构建覆盖。

## 构建

使用 JDK 21 和匹配的 Acbric API dev.21 构建，依赖与路径详见[协作开发指南](CONTRIBUTING.zh-CN.md)。框架基线提交为 `1be89b2`，不能假设上游默认分支已经包含。

```powershell
.\gradlew.bat build
# 覆盖默认的相邻 Acbric 目录及其中的游戏库路径：
.\gradlew.bat build -PacbricDir="D:/Development/Acbric" -PgameLibDir="D:/Games/Airships/libs"
```

输出：`build/libs/ARC-Overhaul-0.1.0-dev.3.jar`。安装时替换旧 ARC JAR，不要并存。产物不含游戏、框架或测试类，不自动安装。`build` 检查编译/打包，默认 `test` 无源码；[集成检查](TESTING.zh-CN.md#运行隔离检查windows)使用自有游戏输入单独运行。

两套游戏 1.2.15.2 / 1.2.14 各通过 71 项定向检查，合计 142：真实 Fabric 注入、原生放置/ID、现金一次性、配置和原生磁盘/内存保存恢复。纹章、背景与土地资源生成使用测试替身；完整地图/道路/初始资产生成、GPU 与联机仍待验收。见 [测试手册](TESTING.zh-CN.md)。

## 目录

- `src/main/java/net/poosh/arc/`：ARC 自有源码，后续按功能分包。
- `src/main/resources/fabric.mod.json`：MOD 身份与入口。
- `build/`：可重建的构建输出。
- [源码定位和设计边界](RESEARCH.zh-CN.md)：首个功能的实现依据与旧 MOD 对照。

玩法放在 `conquest/`，原生适配放在 `mixin/`。后续按功能独立扩展，不将玩法混入框架。

## 协作与许可证

[协作开发指南](CONTRIBUTING.zh-CN.md) · [改动记录](CHANGELOG.zh-CN.md) · [MIT 许可证](LICENSE) · [第三方声明](THIRD_PARTY_NOTICES.md)

产品名称为 **ARC Overhaul**，中文名 **ARC 大修**，建议远端仓库名 `ARC-Overhaul`；稳定 MOD ID 仍是 `arc_overhaul`。这是独立的玩家 MOD，并非游戏本体或官方扩展。Git 包含自有源码、文档、测试源码及构建工具，排除游戏文件、本地依赖、运行与构建产物。本次本地初始化尚未配置远端。
