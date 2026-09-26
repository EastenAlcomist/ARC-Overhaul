/* TownPlacementMixin.java — 原生放置阶段按人类/AI 分配槽位；在土地和建筑生成前确定城市类型。 */
package net.poosh.arc.mixin;

import com.zarkonnen.airships.*;
import net.poosh.arc.conquest.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.zarkonnen.airships.WorldMap$3", remap = false)
public abstract class TownPlacementMixin {
    @Inject(method = "run(ILcom/zarkonnen/airships/WorldMap;)Z", at = @At("HEAD"), cancellable = true)
    private void arc$skipUnusedSlot(int index, WorldMap map, CallbackInfoReturnable<Boolean> cir) {
        GenerationAccess generation = (GenerationAccess) map;
        StartValues values = generation.arc$options();
        if (values == null || !values.changesLand()) return;
        Empire empire = map.empires.get(index % map.size.empires);
        int slots = empire.playerControlled ? values.extraSlots(generation.arc$originalTowns()) : generation.arc$originalTowns();
        if (index / map.size.empires >= slots) cir.setReturnValue(true);
    }
    @Redirect(method = "run(ILcom/zarkonnen/airships/WorldMap;)Z", at = @At(value = "INVOKE",
        target = "Lcom/zarkonnen/airships/WorldMap;access$700(Lcom/zarkonnen/airships/WorldMap;ZLcom/zarkonnen/airships/City;Lcom/zarkonnen/airships/GuardedRandom;)I"))
    private int arc$settlementIncome(WorldMap map, boolean human, City city, GuardedRandom random, int index, WorldMap ignored) {
        StartValues values = ((GenerationAccess) map).arc$options();
        if (values != null && values.changesLand() && human && index / map.size.empires < values.cityCount() - 1) {
            city.isTown = false;
            return ((WorldMapAccess) map).arc$cityIncome(true, city, random);
        }
        return ((WorldMapAccess) map).arc$townIncome(human, city, random);
    }
    /**
     * 把本槽位新建的定居点收紧为连续 ID。
     *
     * <p>跳过槽位会在原生 ID 序列（{@code empires.size() + index}）里留下空洞。而原生
     * {@code ShapeUtils.cityOwnershipAreas} 是从 ID 0 起顺序查找、一旦某个 ID 无对应格子就
     * 永久退出循环，因此空洞之上所有城市都不会生成领土多边形，战略地图上不会被上色。
     * 按创建顺序重新编号即可保持序列连续（无空洞时结果与原生完全相同）。</p>
     */
    @Inject(method = "run(ILcom/zarkonnen/airships/WorldMap;)Z", at = @At("RETURN"))
    private void arc$contiguousSettlementId(int index, WorldMap map, CallbackInfoReturnable<Boolean> cir) {
        GenerationAccess generation = (GenerationAccess) map;
        StartValues values = generation.arc$options();
        if (values == null || !values.changesLand()) return;
        Empire empire = map.empires.get(index % map.size.empires);
        if (empire.cities.isEmpty()) return;
        City settlement = empire.cities.get(empire.cities.size() - 1);
        // 仅当本槽位确实新建了定居点（其 ID 仍是本槽位的原生编号）时才重排。
        if (settlement.id != map.empires.size() + index) return;
        settlement.id = generation.arc$nextSettlementId();
    }
    @Inject(method = "run(ILcom/zarkonnen/airships/WorldMap;)Z", at = @At("RETURN"))
    private void arc$checkPlacement(int index, WorldMap map, CallbackInfoReturnable<Boolean> cir) {
        GenerationAccess generation = (GenerationAccess) map;
        StartValues values = generation.arc$options();
        if (values == null || !values.changesLand()) return;
        Empire empire = map.empires.get(index % map.size.empires);
        int slot = index / map.size.empires;
        if (empire.playerControlled && slot < values.extraSlots(generation.arc$originalTowns()) && empire.cities.size() != slot + 2)
            throw new IllegalStateException("ARC: Cannot place all player settlements. Reduce counts or enlarge the map. / ARC：玩家领地位置不足，请减少数量或增大地图。");
    }
}
