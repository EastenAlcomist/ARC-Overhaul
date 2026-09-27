/* FleetPlan.java — AI 舰队出场规则：允许/强制启用/强制禁用三态与出场国家数；不可变，缺失键按原版。 */
package net.poosh.arc.conquest;

import org.json.JSONObject;
import java.util.*;

/**
 * 玩家可配置的「AI 舰队」规则。
 *
 * <p>游戏里的一支 AI 舰队就是一条 {@code ConstructionStrategy}：它决定某个势力能造哪些船、
 * 陆行舰、建筑与科技。原生在生成世界时为每个势力随机挑一条（见 {@code WorldMap$2.run}），
 * 候选集来自 {@code ConstructionStrategy.forCharge(charge, difficulty)}，只按「军徽图案 + 难度」
 * 过滤，玩家无法干预。本类保存玩家想让哪些舰队出现、哪些不出现、哪些必须出现几次。</p>
 *
 * <p>三态语义：{@link Mode#ALLOW} 允许被随机抽中（默认，等同原版）；{@link Mode#FORCE}
 * 从随机池里拿掉，改为按 {@code count} 强制分配给恰好这么多非玩家势力；{@link Mode#BAN}
 * 永远不出现。只有非默认项才写进配置，因此空配置与「全部允许」完全等价。</p>
 */
public final class FleetPlan {
    /** 共享规则 JSON 里存放舰队规则的键；同一份 JSON 还载着开局设置。 */
    public static final String KEY = "fleets";
    /** 单支舰队的出场国家数上限；大地图势力数远低于此值。 */
    public static final int MAX_COUNTRIES = 32;
    private static final int MAX_NAME = 128;

    public enum Mode {
        ALLOW("allow"), FORCE("force"), BAN("ban");
        private final String wire;
        Mode(String wire) { this.wire = wire; }
        public String wire() { return wire; }
        public static Mode of(String wire) {
            for (Mode mode : values()) if (mode.wire.equals(wire)) return mode;
            throw new IllegalArgumentException("Unknown AI fleet mode / 未知的 AI 舰队模式: " + wire);
        }
    }

    /** 一条舰队的设置。{@code FORCE} 必须给出 1..{@link #MAX_COUNTRIES} 的出场国家数。 */
    public record Entry(Mode mode, int count) {
        public static final Entry ALLOW = new Entry(Mode.ALLOW, 0);
        public Entry {
            Objects.requireNonNull(mode, "mode");
            if (count < 0 || count > MAX_COUNTRIES)
                throw new IllegalArgumentException("AI fleet country count out of range / AI 舰队出场国家数超出范围: " + count);
            if (mode == Mode.FORCE && count < 1)
                throw new IllegalArgumentException("A force-enabled AI fleet needs 1.." + MAX_COUNTRIES + " countries / 强制启用的 AI 舰队需要 1–" + MAX_COUNTRIES + " 个国家");
        }
        public JSONObject json() { return new JSONObject().put("mode", mode.wire()).put("count", count); }
    }

    private static final FleetPlan EMPTY = new FleetPlan(Map.of());

    private final Map<String, Entry> entries;

    private FleetPlan(Map<String, Entry> entries) {
        Map<String, Entry> copy = new TreeMap<>();
        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            if (entry.getValue() == null || entry.getValue().equals(Entry.ALLOW)) continue;
            String name = requireName(entry.getKey());
            copy.put(name, entry.getValue());
        }
        this.entries = Collections.unmodifiableMap(copy);
    }

    /** 只保留非默认项的构造入口；{@code ALLOW} 项会被丢弃。 */
    public static FleetPlan of(Map<String, Entry> entries) { return entries.isEmpty() ? EMPTY : new FleetPlan(entries); }

    /** 没有任何强制或禁用项时的空规则；此时生成过程与写入随机数的方式和原版逐字节一致。 */
    public static FleetPlan empty() { return EMPTY; }

    public static FleetPlan read(JSONObject owner) {
        Objects.requireNonNull(owner, "owner");
        if (!owner.has(KEY)) return EMPTY;
        Object raw = owner.get(KEY);
        if (raw instanceof JSONObject) return parse((JSONObject) raw);
        if (raw == JSONObject.NULL) return EMPTY;
        throw new IllegalArgumentException("Expected an object for / " + KEY + " 必须是对象");
    }

    private static FleetPlan parse(JSONObject raw) {
        Map<String, Entry> parsed = new LinkedHashMap<>();
        Iterator<String> keys = raw.keys();
        while (keys.hasNext()) {
            String name = keys.next();
            Object value = raw.get(name);
            if (!(value instanceof JSONObject))
                throw new IllegalArgumentException("Expected an object for AI fleet / AI 舰队的值必须是对象: " + name);
            JSONObject entry = (JSONObject) value;
            Object mode = entry.has("mode") ? entry.get("mode") : null;
            if (!(mode instanceof String))
                throw new IllegalArgumentException("Expected a mode for AI fleet / AI 舰队缺少 mode: " + name);
            Object count = entry.has("count") ? entry.get("count") : Integer.valueOf(0);
            if (!(count instanceof Integer))
                throw new IllegalArgumentException("Expected an integer country count for AI fleet / AI 舰队的出场国家数必须是整数: " + name);
            parsed.put(name, new Entry(Mode.of((String) mode), (Integer) count));
        }
        return of(parsed);
    }

    /** 独立配置文件的顶层校验器；只允许 {@code fleets} 一个键，避免误把别的设置写进这个文件。 */
    public static void validate(JSONObject data) {
        for (Iterator<String> keys = data.keys(); keys.hasNext(); ) {
            String key = keys.next();
            if (!KEY.equals(key))
                throw new IllegalArgumentException("Unknown key in ARC fleet settings / ARC 舰队配置出现未知键: " + key);
        }
        read(data);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME || name.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Invalid AI fleet name / 无效的 AI 舰队名称");
        return name;
    }

    public Map<String, Entry> entries() { return entries; }
    public Entry entry(String name) { Entry entry = entries.get(name); return entry == null ? Entry.ALLOW : entry; }
    public Mode mode(String name) { return entry(name).mode(); }
    public boolean isVanilla() { return entries.isEmpty(); }
    /** 强制启用的舰队及剩余名额，按名字排序；生成期从这里取号。 */
    public Map<String, Integer> forced() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Entry> entry : entries.entrySet())
            if (entry.getValue().mode() == Mode.FORCE) result.put(entry.getKey(), entry.getValue().count());
        return result;
    }
    public JSONObject json() {
        JSONObject raw = new JSONObject();
        for (Map.Entry<String, Entry> entry : entries.entrySet()) raw.put(entry.getKey(), entry.getValue().json());
        return raw;
    }
    @Override public boolean equals(Object other) { return other instanceof FleetPlan plan && entries.equals(plan.entries); }
    @Override public int hashCode() { return entries.hashCode(); }
    @Override public String toString() { return entries.toString(); }
}
