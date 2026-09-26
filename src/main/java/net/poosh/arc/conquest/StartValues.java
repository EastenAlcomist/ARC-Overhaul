/* StartValues.java — 不可变的新战役设置；-1 保留原版，独立字段不会互相隐式启用。 */
package net.poosh.arc.conquest;

import org.json.JSONObject;

public record StartValues(int cities, int towns, int cash) {
    public static final int MAX_CITIES = 4, MAX_TOWNS = 8, MAX_CASH = 1_000_000;
    public StartValues {
        if (cities != -1 && (cities < 1 || cities > MAX_CITIES)
                || towns < -1 || towns > MAX_TOWNS || cash < -1 || cash > MAX_CASH)
            throw new IllegalArgumentException("Invalid ARC starting values / ARC 开局数值超出范围");
    }
    public static StartValues read(JSONObject json) {
        for (String key : new String[]{"cities", "towns", "cash"})
            if (!(json.get(key) instanceof Integer))
                throw new IllegalArgumentException("Expected integer / 必须为整数: " + key);
        return new StartValues(json.getInt("cities"), json.getInt("towns"), json.getInt("cash"));
    }
    public JSONObject json() { return new JSONObject().put("cities", cities).put("towns", towns).put("cash", cash); }
    public boolean changesLand() { return cities != -1 || towns != -1; }
    public int cityCount() { return cities == -1 ? 1 : cities; }
    public int townCount(int original) { return towns == -1 ? original : towns; }
    public int extraSlots(int original) { return cityCount() - 1 + townCount(original); }
}
