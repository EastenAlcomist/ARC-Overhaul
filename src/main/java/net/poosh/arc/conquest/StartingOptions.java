/* StartingOptions.java — 复用配置草稿和共享规则；完成生成时才设置玩家可用现金。 */
package net.poosh.arc.conquest;

import com.zarkonnen.airships.*;
import net.fabricacs.api.AcbricModContext;
import net.fabricacs.api.config.*;
import net.fabricacs.api.event.AirshipsCampaignEvents;
import net.fabricacs.api.rules.SharedRules;
import net.fabricacs.api.ui.*;
import net.fabricacs.api.util.AcbricLanguage;
import java.io.IOException;
import java.util.List;

public final class StartingOptions {
    private static ModConfig config;
    private static SharedRules rules;
    private static ModUi ui;
    private StartingOptions() { }
    public static String text(String en, String zh) { return AcbricLanguage.text(en, zh); }
    public static void initialize(AcbricModContext context) {
        try {
            config = context.config("conquest-start", 1, new StartValues(-1, -1, -1).json(), StartValues::read);
            rules = context.sharedRules(1, config.load().data(), StartValues::read);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load ARC settings / 无法读取 ARC 配置，未覆盖原文件", ex);
        }
        ui = context.ui();
        ui.register("conquest-start", StartingOptions::title, StartingOptions::window);
        AirshipsCampaignEvents.CREATED.register(world -> finish((CampaignWorld) world));
        context.logger().info("ARC starting options loaded / ARC 开局设置已加载");
    }
    public static String title() { return text("ARC Overhaul: Player starting options", "ARC 大修：玩家开局设置"); }
    public static StartValues forMap(WorldMap map) { return StartValues.read(rules.forCampaign(map).values()); }
    public static void open() { ui.open(window()); }
    public static UiWindow window() {
        var effect = ConfigField.Effect.NEW_CAMPAIGN;
        var fields = List.of(
            ConfigField.integer("cities", new ConfigField.Text("Cities per human empire", "每个人类势力的城市数"), -1, StartValues.MAX_CITIES, effect)
                .description(new ConfigField.Text("-1: vanilla (1). Otherwise 1–4; 0 is invalid.", "-1：原版（1 座）。自定义为 1–4，不能填 0。")),
            ConfigField.integer("towns", new ConfigField.Text("Towns per human empire", "每个人类势力的城镇数"), -1, StartValues.MAX_TOWNS, effect)
                .description(new ConfigField.Text("-1: map default. Otherwise 0–8. AI counts remain vanilla.", "-1：地图默认。自定义为 0–8。AI 数量保持原版。")),
            ConfigField.integer("cash", new ConfigField.Text("Player starting cash", "玩家开局现金"), -1, StartValues.MAX_CASH, effect)
                .description(new ConfigField.Text("-1: vanilla. Otherwise 0–1000000 AFTER starting assets. New campaigns only; all peers must use matching settings.", "-1：原版。自定义为 0–1000000，在初始资产配置后设置。仅新战役生效；联机各方需使用相同设置。")));
        try { return SettingsUi.window(title(), new ConfigEditor(config, fields), applied -> rules.update(applied.snapshot().data())); }
        catch (IOException ex) { throw new IllegalStateException("Cannot open ARC settings / 无法打开 ARC 设置", ex); }
    }
    /** CREATED 仅在新地图第一次 setupPlayer 后触发；不监听 LOADED/RESTORED。 */
    public static void finish(CampaignWorld world) {
        StartValues values = forMap(world.map);
        GenerationAccess generation = (GenerationAccess) world.map;
        if (values.changesLand()) {
            for (Empire empire : world.map.empires) {
                if (!empire.playerControlled) continue;
                long cities = empire.cities.stream().filter(city -> !city.isTown).count();
                long towns = empire.cities.size() - cities;
                if (cities != values.cityCount() || towns != values.townCount(generation.arc$originalTowns()))
                    throw new IllegalStateException("ARC: Not enough settlement space. Reduce counts or use a larger map. / ARC：可用领地位置不足，请减少数量或增大地图。");
            }
        }
        if (values.cash() >= 0)
            for (Empire empire : world.map.empires)
                if (empire.playerControlled) empire.setMoney(values.cash());
    }
}
