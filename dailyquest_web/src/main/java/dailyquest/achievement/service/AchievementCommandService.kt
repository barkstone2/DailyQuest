package dailyquest.achievement.service

import dailyquest.achievement.dto.AchievementAchieveRequest
import dailyquest.achievement.entity.Achievement
import dailyquest.achievement.entity.AchievementType.*
import dailyquest.quest.service.QuestLogService
import dailyquest.user.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class AchievementCommandService @Autowired constructor(
    private val achievementQueryService: AchievementQueryService,
    private val achievementLogCommandService: AchievementLogCommandService,
    private val questLogService: QuestLogService,
    private val userService: UserService,
) {

    @Async
    fun checkAndAchieveAchievement(achieveRequest: AchievementAchieveRequest) {
        val targetAchievement = achievementQueryService.getNotAchievedAchievement(achieveRequest.type, achieveRequest.userId)
        val currentValue = this.resolveCurrentValue(achieveRequest, targetAchievement)
        if (targetAchievement.canAchieve(currentValue)) {
            achievementLogCommandService.achieve(targetAchievement, achieveRequest.userId)
        }
    }

    private fun resolveCurrentValue(
        achieveRequest: AchievementAchieveRequest,
        targetAchievement: Achievement
    ): Int {
        return when (achieveRequest.type) {
            QUEST_REGISTRATION -> questLogService.getTotalRegistrationCount(achieveRequest.userId)
            QUEST_COMPLETION -> questLogService.getTotalCompletionCount(achieveRequest.userId)
            QUEST_CONTINUOUS_REGISTRATION_DAYS -> questLogService.getRegDaysFrom(achieveRequest.userId, targetAchievement.targetValue)
            USER_LEVEL -> userService.getUserPrincipal(achieveRequest.userId).level
            EMPTY, USER_GOLD_EARN -> 0
        }
    }
}