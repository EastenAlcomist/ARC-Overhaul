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
