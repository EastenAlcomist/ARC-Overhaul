/* ArcRules.java — ARC 唯一的共享规则：开局设置与 AI 舰队规则合并成一份 JSON，只在新战役固化。 */
package net.poosh.arc.conquest;

import com.zarkonnen.airships.WorldMap;
import net.fabricacs.api.AcbricModContext;
import net.fabricacs.api.config.ModConfig;
import net.fabricacs.api.rules.SharedRules;
import org.json.JSONObject;
import java.util.Iterator;

/**
 * 框架的共享规则按 MOD ID 注册且只允许注册一次，因此 ARC 的开局设置和 AI 舰队规则必须共用同一份
 * JSON，而不是各注册一份。这份 JSON 在 {@code WorldGenScreen} 构造入口被固化进战役数据，早于
 * 生成阶段，所以生成期可以直接读到本局真正生效的值；联机由框架的大厅校验负责一致性，本类只发布
 * 候选值，不改动已存在的战役。
 */
public final class ArcRules {
    private static final int VERSION = 1;
    private static SharedRules rules;
    private static ModConfig startConfig;
    private static ModConfig fleetConfig;

    private ArcRules() { }

    public static void initialize(AcbricModContext context, ModConfig start, ModConfig fleets) {
        startConfig = start;
        fleetConfig = fleets;
        try {
            startConfig.load();
            fleetConfig.load();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Cannot load ARC settings / 无法读取 ARC 配置，未覆盖原文件", ex);
        }
        rules = context.sharedRules(VERSION, combine(), ArcRules::validate);
    }

    /** 任一设置保存后重新发布候选规则；只影响之后开始的新战役，不写配置也不改已存在的战役。 */
    static void publish() { rules.update(combine()); }

    static StartValues startValues(WorldMap map) { return StartValues.read(rules.forCampaign(map).values()); }

    static FleetPlan fleetPlan(WorldMap map) { return FleetPlan.read(rules.forCampaign(map).values()); }

    private static JSONObject combine() {
        JSONObject merged = new JSONObject();
        copy(startConfig.read().data(), merged);
        merged.put(FleetPlan.KEY, FleetPlan.read(fleetConfig.read().data()).json());
        return merged;
    }

    private static void copy(JSONObject from, JSONObject to) {
        for (Iterator<String> keys = from.keys(); keys.hasNext(); ) {
            String key = keys.next();
            to.put(key, from.get(key));
        }
    }

    /**
     * 兼容旧存档：这份 JSON 在加入舰队规则之前只有开局设置，缺少 {@code fleets} 键时按「全部允许」
     * 读取。规则版本因此保持 1，旧战役不会因为新增字段被判为版本不符。
     */
    private static void validate(JSONObject json) {
        StartValues.read(json);
        FleetPlan.read(json);
    }
}
