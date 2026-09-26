/* AssetFixtures.java — 测试专用资源替身，不进入发行 JAR；保留原生放置、ID 和收入路径。 */
package regression.fixtures;

import com.zarkonnen.airships.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

public final class AssetFixtures {
    @Mixin(value = CoatOfArms.class, remap = false)
    public static abstract class Arms {
        @Inject(method = "getRandom(Lcom/zarkonnen/airships/GuardedRandom;Lcom/zarkonnen/airships/HeraldicStyle;)Lcom/zarkonnen/airships/CoatOfArms;", at = @At("HEAD"), cancellable = true)
        private static void fixture(GuardedRandom random, HeraldicStyle style, CallbackInfoReturnable<CoatOfArms> cir) { cir.setReturnValue(null); }
    }
    @Mixin(value = WorldMap.class, remap = false)
    public static abstract class Background {
        @Inject(method = "getBackground(II)Lcom/zarkonnen/airships/CombatBackgroundFlavor;", at = @At("HEAD"), cancellable = true)
        private void fixture(int x, int y, CallbackInfoReturnable<CombatBackgroundFlavor> cir) { cir.setReturnValue(null); }
    }
    @Mixin(value = MapLocation.class, remap = false)
    public static abstract class Land {
        @Inject(method = "generateLand(Lcom/zarkonnen/airships/GuardedRandom;Lcom/zarkonnen/airships/CombatBackgroundFlavor;)V", at = @At("HEAD"), cancellable = true)
        private void fixture(GuardedRandom random, CombatBackgroundFlavor background, CallbackInfo ci) {
            regression.ArcRuntimeProbe.generatedLand++;
            if (!((City)(Object)this).isTown) regression.ArcRuntimeProbe.generatedCityLand++;
            ci.cancel();
        }
    }
}
