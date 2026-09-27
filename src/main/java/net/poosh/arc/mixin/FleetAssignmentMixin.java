/* FleetAssignmentMixin.java — 在原生把 AI 舰队交给势力之前改写选择；默认设置下不消耗随机数。 */
package net.poosh.arc.mixin;

import com.zarkonnen.airships.*;
import net.poosh.arc.conquest.FleetOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 原生 {@code WorldMap$2.run(index, map)} 在创建第 {@code index} 个势力时这样挑舰队：
 * {@code strategies.get(map.r.nextInt(strategies.size()))}，其中 {@code strategies} 来自
 * {@code ConstructionStrategy.forCharge(charge, difficulty)}。整个方法里 {@code ArrayList.get}
 * 有四个调用点（城市名、舰队、科技选项、英雄），舰队那一个是第 2 个，因此按字节码序号 ordinal = 1 定位；
 * 处理函数再核对取到的对象类型，序号一旦因游戏升级漂移就自动退回原版行为，而不是把别的列表当成舰队。
 *
 * <p>必须在这里改写：{@code Empire.constructionStrategy} 是 final 字段，势力一旦创建就无法更换舰队。</p>
 */
@Mixin(targets = "com.zarkonnen.airships.WorldMap$2", remap = false)
public abstract class FleetAssignmentMixin {
    @Redirect(method = "run(ILcom/zarkonnen/airships/WorldMap;)Z", at = @At(value = "INVOKE",
        target = "Ljava/util/ArrayList;get(I)Ljava/lang/Object;", ordinal = 1))
    private Object arc$fleetChoice(ArrayList pool, int index, int empireIndex, WorldMap map) {
        Object vanilla = pool.get(index);
        if (!(vanilla instanceof ConstructionStrategy)) return vanilla;
        @SuppressWarnings("unchecked") List<ConstructionStrategy> candidates = (List<ConstructionStrategy>) pool;
        return FleetOptions.choose(candidates, (ConstructionStrategy) vanilla, empireIndex, map);
    }
}
