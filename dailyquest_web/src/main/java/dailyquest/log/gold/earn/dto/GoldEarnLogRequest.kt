package dailyquest.log.gold.earn.dto

import dailyquest.log.gold.earn.entity.GoldEarnSource

class GoldEarnLogRequest(
    val amount: Long,
    val source: GoldEarnSource
) {
}