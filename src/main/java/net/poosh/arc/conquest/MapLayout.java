/* MapLayout.java — 每张地图独立的 ID 容量，保留全局 MapSize；原生反序列化早期恢复布局。 */
package net.poosh.arc.conquest;

import com.zarkonnen.airships.MapSize;
import org.json.JSONObject;

public final class MapLayout {
    public static final String KEY = "arcStartingLayout";
    private MapLayout() { }
    public static MapSize copy(MapSize base, int slots) {
        if (slots == base.townsPerEmpire) return base;
        if (base.gridSize % 32 != 0 || slots < 0 || slots > Math.max(base.townsPerEmpire, 11))
            throw new IllegalArgumentException("Unsupported ARC map layout / ARC 不支持此地图布局");
        MapSize result = new MapSize(new JSONObject().put("name", base.name).put("gridSize", base.gridSize / 32)
            .put("empires", base.empires).put("nests", base.nests).put("townsPerEmpire", slots));
        result.sourceExpansion = base.sourceExpansion;
        result.sourceMod = base.sourceMod;
        return result;
    }
    /** 此字段只保存原生布局容量；玩家设置由 Acbric 共享规则保存，不从本机配置恢复。 */
    public static MapSize read(MapSize base, JSONObject data) {
        if (!data.has(KEY)) return base;
        JSONObject layout = data.getJSONObject(KEY);
        if (!(layout.get("version") instanceof Integer) || layout.getInt("version") != 1
                || !(layout.get("slots") instanceof Integer) || layout.getInt("slots") < base.townsPerEmpire)
            throw new IllegalArgumentException("Invalid ARC map layout / ARC 地图布局数据无效");
        return copy(base, layout.getInt("slots"));
    }
    public static void write(JSONObject data, MapSize size, boolean modified) {
        if (modified) data.put(KEY, new JSONObject().put("version", 1).put("slots", size.townsPerEmpire));
    }
}
