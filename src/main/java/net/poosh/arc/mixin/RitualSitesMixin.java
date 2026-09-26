/* RitualSitesMixin.java — 允许零城镇时祭坛数量不能超过剩余城镇；城市不被选成城镇祭坛。 */
package net.poosh.arc.mixin;
import com.zarkonnen.airships.*;
import net.poosh.arc.conquest.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets = "com.zarkonnen.airships.WorldMap$4", remap = false)
public abstract class RitualSitesMixin {
    @Redirect(method = "run(ILcom/zarkonnen/airships/WorldMap;)Z", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"))
    private int arc$boundedRituals(int a, int b, int index, WorldMap map) {
        int count = Math.max(a, b);
        StartValues values = ((GenerationAccess) map).arc$options();
        if (values == null || !values.changesLand()) return count;
        int towns = 0;
        for (Empire empire : map.empires) for (City city : empire.cities) if (city.isTown) towns++;
        return Math.min(count, towns);
    }
}
