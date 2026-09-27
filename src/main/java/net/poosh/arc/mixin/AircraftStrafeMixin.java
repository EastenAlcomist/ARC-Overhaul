/* AircraftStrafeMixin.java — 飞机攻击航点跟随目标：目标移动多少，航点就平移多少，不再因为目标离开原点位而作废重选。 */
package net.poosh.arc.mixin;

import com.zarkonnen.airships.*;
import com.zarkonnen.catengine.util.Pt;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让飞机的攻击航线（strafe）航点相对目标保持固定偏移。
 *
 * <p>原生 {@code Crewman.outsideFlyingTick} 每次只锁定一个<b>世界坐标</b>航点
 * {@code strafeTo}：若飞机在目标中心左侧，航点取「目标右边缘 + strafeOvershoot」，否则取
 * 「目标左边缘 − strafeOvershoot」；随后四个条件之一成立就把航点整块丢弃
 * （原生第 2562 行：已到位 &lt;48px、目标朝航点方向离开 ≥ w+600、目标背离航点方向离开 ≥
 * 2×strafeOvershoot、目标上移甩开航点）。丢弃后<b>完全按「此刻在目标中心哪一侧」重选</b>，
 * 不看飞机正在往哪飞。于是目标只要沿远离航点的方向移动 200px（{@code 2×strafeOvershoot}），
 * 这一趟攻击就被丢弃；若此时飞机已被目标运动甩到中心另一侧，新航点就落在机身背后 ——
 * 飞机掉头。轰炸机 {@code airXAcceleration} 只有 0.00008，掉头一次要 6.25–15 秒，
 * 而它又必须平飞穿过目标宽度中间 60% 才能投弹（第 2296 行），所以中途掉头等于整趟白跑。</p>
 *
 * <p>本注入把航点从「世界坐标」改成「目标相对坐标」：目标这一帧移动 Δx，已存航点也平移 Δx。
 * 这样第 2562 行的两个位移条件不再触发，方向只会在真正飞到航点（&lt;48px，即一趟完整飞完）
 * 之后才翻转。目标切换（母舰 fireAt 改写、原目标阵亡后重选最近邻）与读档进来时不平移，
 * 交还原版判定 —— 换目标本来就该重新决策。</p>
 *
 * <p>只影响「有目标」的那条航线分支：航点为 null 或目标为 null 时直接返回，因此
 * {@code attackTarget} 为空的原版巡逻分支（第 2592 行）、地面单位与非飞行单位完全不受影响。
 * 两个判空条件恰好等价于「正在执行第 2561 行那个分支」：{@code attackTarget} 只在
 * {@code shootsShips} 时被赋值（第 2200-2215 行），所以不必再读 {@code type}。</p>
 *
 * <p>锚点用<b>引用相等</b>判断原版是否重选过航点：原生只有两处把 {@code strafeTo} 置 null、
 * 两处赋新实例（字节码偏移 1117/1612 与 1497/1949），{@code Pt} 又是不可变类（x/y 均 final），
 * 因此对象身份变化一定意味着原版换了一趟。平移只能新建 {@code Pt}：CatEngine 里
 * {@code Pt.shifted()} 等变换方法是没有实现的桩，调用即抛异常。{@code popOut} 会在起飞时把 {@code strafeTo} 与
 * {@code attackTarget} 一起清空（第 1589、1641 行），新航点必然是新实例，锚点不会串档。</p>
 */
@Mixin(value = Crewman.class, remap = false)
public abstract class AircraftStrafeMixin {
    /** 公开字段，原生在两处置 null、两处赋新实例；只读身份用于判断"原版是否重选过这一趟"。 */
    @Shadow public Pt strafeTo;
    /** 公开字段；只在 {@code shootsShips} 类型上被赋值，非 null 即表示正在执行有目标的航线分支。 */
    @Shadow public Airship attackTarget;

    /** 上一次锚定过的航点实例；与原版当前 {@code strafeTo} 不同即表示原版换了一趟。 */
    @Unique private Pt arc$anchorPoint;
    /** 锚定时的目标；目标换了就不做平移，交还原版重新决策。 */
    @Unique private Airship arc$anchorTarget;
    /** 锚定（或上次平移）时目标的 x。 */
    @Unique private double arc$anchorTargetX;

    @Inject(method = "outsideFlyingTick(ILcom/zarkonnen/airships/Combat;Lcom/zarkonnen/airships/Combat$Side;Z)Z",
            at = @At("HEAD"))
    private void arc$followTarget(int ms, Combat combat, Combat.Side side, boolean onViewingSide,
                                  CallbackInfoReturnable<Boolean> cir) {
        Pt point = this.strafeTo;
        Airship target = this.attackTarget;
        // 没有航点或没有目标：原版巡逻分支与一切非攻击状态，完全不碰。
        if (point == null || target == null) return;
        double x = target.getX();
        if (point != this.arc$anchorPoint || target != this.arc$anchorTarget) {
            // 原版刚刚重选过航点（或换了目标）：本帧不平移，只把锚点对齐到新状态。
            this.arc$anchorPoint = point;
            this.arc$anchorTarget = target;
            this.arc$anchorTargetX = x;
            return;
        }
        double moved = x - this.arc$anchorTargetX;
        this.arc$anchorTargetX = x;
        if (moved != 0.0) {
            // Pt 是不可变类（x/y 均为 final），只能换一个新实例。注意：CatEngine 里 Pt 的
            // shifted()/towards()/between()/scaled() 全都是抛 UnsupportedOperationException 的
            // 未实现桩，编译能过、一跑就炸，只能用公开构造器。
            this.strafeTo = new Pt(point.x + moved, point.y);
            this.arc$anchorPoint = this.strafeTo;
        }
    }
}
