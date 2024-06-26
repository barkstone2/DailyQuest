package dailyquest.achievement.dto

import dailyquest.achievement.entity.Achievement
import dailyquest.achievement.entity.AchievementType

interface AchievementSaveRequest {
    val title: String
    val description: String
    val type: AchievementType
    val targetValue: Long
    fun mapToEntity(): Achievement {
        return Achievement(title, description, type, targetValue)
    }
}