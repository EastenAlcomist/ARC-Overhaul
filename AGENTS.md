# ARC Overhaul 项目指引

独立的 Airships 大修 MOD，正式名称 **ARC Overhaul**，中文 **ARC 大修**，不要自行解释或扩展 ARC 缩写。MOD ID `arc_overhaul`，基于 Acbric dev.21，JDK 21。自有源码与文档采用用户选定的 MIT；第三方 wrapper 保留原许可证。本项目是独立 Git 仓库，协作基线为 `main`。禁止把功能 MOD 加入 Acbric 框架默认包。

2026-09-26 dev.3 整理命名与协作工程，保留 dev.2 的征服开局城市/城镇/现金行为。仅人类势力，AI 保持原版规则。三个字段 -1 保留原版，自定义 1–4 城市、0–8 城镇、0–1000000 现金。旧参考为 acbric-starting-cities.jar 0.3.1，已研究并声明冲突，不能与本 MOD 同时启用。另一份 City Upgrade 是建筑升级链项目。研究文件不在 Git 中。

文档入口 README / CONTRIBUTING / RESEARCH / TESTING / CHANGELOG，均有英文与 zh-CN 版本。原生设置列表底部及 MOD 详情入口打开框架设置页。dev.3 两版各 71 项（142 项）通过，证据为本地 `build/runtime-tests/repo-init/summary.json`（不提交运行夹具），摘要见 CHANGELOG。真实原生放置/保存/状态恢复已测，但纹章/背景/土地资源有测试替身；完整世界、道路、初始资产、GPU 和联机仍待实机，不扩大验收范围。

数量在首次 doSetup 用已冻结共享规则固定，独立 MapSize 容量副本覆盖全原生数组/阶段/巢穴 ID 逻辑；不能修改全局 MapSize。WorldMap$3 放置前后控制槽位与城市类型，祭坛不足保护在 $4。现金使用 CREATED，不能挂 StrategicScreen 构造器。原生恢复早期需要 arcStartingLayout 技术字段；不要移到构造 RETURN 后才修容量。只支持新战役，旧档 RULE_MISSING 不自动转换。联机没有房主参数广播，各端提前手动设置相同值，端到端未验证。

后续代码须有中文文件头，用户界面、文档同时支持中英文（跟随游戏语言）。优先公共 API；确需 Mixin 时 remap=false 并精确核对两版描述符。开局值只影响新地图，不能在加载/恢复时重复发钱或改领地。共享值不能直接读取各客户端本地配置；不得绕过框架大厅规则一致性检查。读写存档不代表自动同步。

构建入口 `gradlew.bat build`，可用 `-PacbricDir` 和 `-PgameLibDir` 指定仓库外依赖。框架基线提交 `1be89b2`，不要默认公开分支已包含。集成检查使用 `tests/run_runtime.py --tag NAME --game LABEL=ROOT`，路径及隔离要求见 TESTING。默认 Gradle test 无源码，不算测试通过。

不改写反编译资料，不覆盖用户运行包/存档，不提交游戏文件、反编译源码、私人日志、凭据或本地依赖。`build/` 和运行夹具排除在 Git 外；wrapper JAR 是唯一预期跟踪的二进制构建工具。当前 build 不自动安装到游戏；改名仍保留 `arc_overhaul`、包名及所有存档契约。提交与推送按用户当前授权处理，不自动创建远端或发布。
