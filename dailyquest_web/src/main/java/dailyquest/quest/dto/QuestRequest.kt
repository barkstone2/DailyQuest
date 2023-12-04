package dailyquest.quest.dto

import dailyquest.common.MessageUtil
import dailyquest.quest.entity.Quest
import dailyquest.quest.entity.QuestState
import dailyquest.quest.entity.QuestType
import dailyquest.user.entity.UserInfo
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

class QuestRequest(
    title: String,
    description: String,
    details: MutableList<DetailRequest>? = null,
    deadLine: LocalDateTime? = null,
) {

    @NotBlank(message = "{NotBlank.quest.title}")
    @Size(max = 50, message = "{size.quest.title}")
    val title = title

    @Size(max = 300, message = "{Size.quest.description}")
    val description = description

    val deadLine: LocalDateTime? = deadLine

    @Valid
    @Size(max = 5, message = "{Size.quest.details}")
    val details = details ?: mutableListOf()

    private var type: QuestType = QuestType.SUB

    fun toMainQuest() {
        this.type = QuestType.MAIN
    }

    fun mapToEntity(nextSeq: Long, userInfo: UserInfo): Quest {
        return Quest(
            title = title,
            description = description,
            user = userInfo,
            seq = nextSeq,
            state = QuestState.PROCEED,
            type = type,
            deadline = deadLine
        )
    }

    override fun toString(): String {
        return "QuestRequest(title='$title', description='$description', details=$details, type=$type)"
    }

    fun checkRangeOfDeadLine() {
        if (deadLine != null) {
            val now = LocalDateTime.now().withSecond(0).withNano(0)
            var nextReset = now.withHour(6).withMinute(0)
            if(now.isEqual(nextReset) || now.isAfter(nextReset)) nextReset = nextReset.plusDays(1L)

            require(deadLine.isAfter(now.plusMinutes(5)) && deadLine.isBefore(nextReset.minusMinutes(5))) { MessageUtil.getMessage("Range.quest.deadLine") }
        }
    }

}
