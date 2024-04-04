package dailyquest.user.dto

import dailyquest.user.entity.ProviderType
import dailyquest.user.entity.RoleType
import dailyquest.user.entity.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class UserResponse(
    val id: Long,
    val nickname: String,
    val providerType: ProviderType = ProviderType.GOOGLE,
    val coreTime: LocalTime = LocalTime.of(8, 0),
    val coreTimeLastModifiedDate: LocalDateTime? = null,
    val exp: Long = 0,
    val gold: Long = 0,
    val role: RoleType = RoleType.USER,
    val questRegistrationCount: Int = 0,
    val questCompletionCount: Int = 0,
    val currentQuestContinuousRegistrationDays: Int = 0,
    val currentQuestContinuousCompletionDays: Int = 0,
    val maxQuestContinuousRegistrationDays: Int = 0,
    val maxQuestContinuousCompletionDays: Int = 0,
    val lastQuestRegistrationDate: LocalDate? = null,
    val lastQuestCompletionDate: LocalDate? = null,
    val perfectDayCount: Int = 0,
    val goldEarnAmount: Int = 0,
    val goldUseAmount: Int = 0,
) {
    companion object {
        @JvmStatic
        fun from(user: User): UserResponse {
            return UserResponse(
                 user.id,
                 user.nickname,
                 user.providerType,
                 user.coreTime,
                 user.coreTimeLastModifiedDate,
                 user.exp,
                 user.gold,
                 user.role,
                 user.questRegistrationCount,
                 user.questCompletionCount,
                 user.currentQuestContinuousRegistrationDays,
                 user.currentQuestContinuousCompletionDays,
                 user.maxQuestContinuousRegistrationDays,
                 user.maxQuestContinuousCompletionDays,
                 user.lastQuestRegistrationDate,
                 user.lastQuestCompletionDate,
                 user.perfectDayCount,
                 user.goldEarnAmount,
                 user.goldUseAmount,
            )
        }
    }
}