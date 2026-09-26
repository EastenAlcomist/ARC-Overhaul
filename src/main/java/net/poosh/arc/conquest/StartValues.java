/* StartValues.java — 不可变的新战役设置；-1 保留原版，独立字段不会互相隐式启用。 */
package net.poosh.arc.conquest;

import org.json.JSONObject;

public record StartValues(int cities, int towns, int cash, int research) {
    public static final int MAX_CITIES = 4, MAX_TOWNS = 8, MAX_CASH = 1_000_000;
    /**
     * 研发点上限必须和游戏自身的技术成本同量级：{@code Tech.baseCost} 为
     * {@code BASE_RESEARCH_COST × RESEARCH_COST_MULTIPLIER × 5600 × 1.4^tier}，
     * 前几级科技就需要 1,680,000–2,352,000 点（引擎内实测）。上限过小会让进度条上
     * 出现一条和 0 无法区分的细线，看起来就像没有发放。
     */
    public static final int MAX_RESEARCH = 10_000_000;
    public StartValues {
        if (cities != -1 && (cities < 1 || cities > MAX_CITIES)
                || towns < -1 || towns > MAX_TOWNS || cash < -1 || cash > MAX_CASH
                || research < 0 || research > MAX_RESEARCH)
            throw new IllegalArgumentException("Invalid ARC starting values / ARC 开局数值超出范围");
    }
    /** 忽略研发点的简写；{@code 0} 就是不发放，等同原版。 */
    public StartValues(int cities, int towns, int cash) { this(cities, towns, cash, 0); }
    /** 研发点默认 0：0 表示不发放，行为等同原版；旧配置没有该键时按 0 读取，不强制迁移。 */
    public static StartValues read(JSONObject json) {
        for (String key : new String[]{"cities", "towns", "cash"})
            if (!(json.get(key) instanceof Integer))
                throw new IllegalArgumentException("Expected integer / 必须为整数: " + key);
        Object points = json.has("research") ? json.get("research") : Integer.valueOf(0);
        if (!(points instanceof Integer))
            throw new IllegalArgumentException("Expected integer / 必须为整数: research");
        return new StartValues(json.getInt("cities"), json.getInt("towns"), json.getInt("cash"), (Integer) points);
    }
    public JSONObject json() {
        return new JSONObject().put("cities", cities).put("towns", towns).put("cash", cash).put("research", research);
    }
    public boolean changesLand() { return cities != -1 || towns != -1; }
    /** 只有正数才发放：0 是默认值，必须保持原版的开局研发行为。 */
    public boolean grantsResearch() { return research > 0; }
    public int cityCount() { return cities == -1 ? 1 : cities; }
    public int townCount(int original) { return towns == -1 ? original : towns; }
    public int extraSlots(int original) { return cityCount() - 1 + townCount(original); }
}
