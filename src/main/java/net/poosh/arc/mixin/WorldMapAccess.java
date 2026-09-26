/* WorldMapAccess.java — 精确调用原生收入函数与原生选址函数，保证新增城市沿用原版规则。 */
package net.poosh.arc.mixin;
import com.zarkonnen.airships.*;
import com.zarkonnen.catengine.util.Utils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = WorldMap.class, remap = false)
public interface WorldMapAccess {
    @Invoker("income") int arc$cityIncome(boolean human, City city, GuardedRandom random);
    @Invoker("townIncome") int arc$townIncome(boolean human, City city, GuardedRandom random);
    /**
     * 原生私有选址函数 {@code findMapLocationSpot(GuardedRandom, distance, Empire)}。
     * 只保证落点不是水，不保证连在一块像样的陆地上，因此 ARC 会在它的结果上再做一次检查。
     */
    @Invoker("findMapLocationSpot") Utils.Pair<Integer, Integer> arc$findMapLocationSpot(GuardedRandom random, int distance, Empire empire);
}
