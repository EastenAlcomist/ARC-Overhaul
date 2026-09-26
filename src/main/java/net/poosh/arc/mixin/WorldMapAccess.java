/* WorldMapAccess.java — 精确调用原生收入函数，新增城市沿用城市收入而非任意翻倍。 */
package net.poosh.arc.mixin;
import com.zarkonnen.airships.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = WorldMap.class, remap = false)
public interface WorldMapAccess {
    @Invoker("income") int arc$cityIncome(boolean human, City city, GuardedRandom random);
    @Invoker("townIncome") int arc$townIncome(boolean human, City city, GuardedRandom random);
}
