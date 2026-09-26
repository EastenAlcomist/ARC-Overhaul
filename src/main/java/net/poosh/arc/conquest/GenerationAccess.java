/* GenerationAccess.java — ARC 自己的地图适配契约；不作为 Acbric 公共 API 发布。 */
package net.poosh.arc.conquest;

public interface GenerationAccess {
    StartValues arc$options();
    int arc$originalTowns();
    /** 取下一个连续的定居点 ID；原生领土描边要求 ID 序列无空洞，见 TownPlacementMixin。 */
    int arc$nextSettlementId();
}
