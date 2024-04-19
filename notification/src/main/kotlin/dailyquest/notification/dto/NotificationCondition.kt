package dailyquest.notification.dto

import dailyquest.notification.entity.NotificationType

data class NotificationCondition(
    val page: Int = 0,
    val type: NotificationType? = null,
)