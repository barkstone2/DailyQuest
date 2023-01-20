package todayquest.quest.dto

import org.springframework.format.annotation.DateTimeFormat
import todayquest.quest.entity.Quest
import todayquest.quest.entity.QuestDifficulty
import todayquest.quest.entity.QuestState
import todayquest.quest.entity.QuestType
import todayquest.reward.dto.RewardResponseDto
import todayquest.user.entity.UserInfo
import java.time.LocalDate
import java.time.LocalTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

data class QuestRequestDto(
    @NotBlank @Size(max = 50)
    var title: String? = null,

    @NotBlank @Size(max = 300)
    var description: String? = null,

    @NotNull
    var isRepeat: Boolean = false,

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    var deadLineDate: LocalDate? = null,

    @DateTimeFormat(pattern = "HH:mm")
    var deadLineTime: LocalTime? = null,

    @NotNull
    var difficulty: QuestDifficulty = QuestDifficulty.EASY,
    var state: QuestState? = null,

    @Size(max = 3)
    var rewards: MutableList<Long> = mutableListOf(),

    @Size(max = 5)
    var detailQuests: MutableList<DetailQuestRequestDto> = mutableListOf()
) {

    fun mapToEntity(nextSeq: Long, userInfo: UserInfo): Quest {
        return Quest(
            title = title ?: throw IllegalArgumentException("퀘스트 이름은 비어있을 수 없습니다."),
            description = description,
            user = userInfo,
            seq = nextSeq,
            isRepeat = isRepeat,
            deadLineDate = deadLineDate,
            deadLineTime = deadLineTime,
            state = QuestState.PROCEED,
            type = QuestType.DAILY,
            difficulty = difficulty
            )
    }

    fun mapToResponseDto(rewards: List<RewardResponseDto>): QuestResponseDto {
        return QuestResponseDto(
            title = title,
            description = description,
            difficulty = difficulty,
            deadLineTime = deadLineTime,
            deadLineDate = deadLineDate,
            isRepeat = isRepeat,
            rewards = rewards,
        )
    }
}