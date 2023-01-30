package todayquest.quest.entity

import jakarta.persistence.*
import org.springframework.security.access.AccessDeniedException
import todayquest.common.BaseTimeEntity
import todayquest.common.MessageUtil
import todayquest.quest.dto.DetailQuestRequestDto
import todayquest.quest.dto.QuestRequestDto
import todayquest.reward.entity.Reward
import todayquest.user.entity.UserInfo

@Entity
class Quest(
    title: String,
    description: String?,
    user: UserInfo,
    seq: Long,
    state: QuestState = QuestState.PROCEED,
    type: QuestType,
) : BaseTimeEntity() {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quest_id")
    val id: Long? = null

    @Column(length = 50, nullable = false)
    var title: String = title
        protected set

    @Column(length = 300)
    var description: String? = description
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: UserInfo = user

    @Column(name = "user_quest_seq", nullable = false)
    val seq: Long = seq

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: QuestType = type

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var state: QuestState = state
        protected set

    // 퀘스트에서 제거되면 매핑 엔티티 삭제
    @OneToMany(mappedBy = "quest", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _rewards: MutableList<QuestReward> = mutableListOf()
    val rewards : List<QuestReward>
        get() = _rewards.toList()

    @OneToMany(mappedBy = "quest", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _detailQuests: MutableList<DetailQuest> = mutableListOf()
    val detailQuests : List<DetailQuest>
        get() = _detailQuests.toList()

    fun updateQuestEntity(dto: QuestRequestDto, updateRewards: List<Reward>): MutableList<QuestReward> {
        title = dto.title ?: throw IllegalArgumentException("퀘스트 이름은 비어있을 수 없습니다.")
        description = dto.description
        return updateRewardList(updateRewards)
    }

    private fun updateRewardList(updateRewards: List<Reward>): MutableList<QuestReward> {
        val newRewards: MutableList<QuestReward> = mutableListOf()
        val updateCount = updateRewards.size

        for (i in 0 until updateCount) {
            val newReward = QuestReward(reward = updateRewards[i], quest = this)
            try {
                _rewards[i].updateReward(updateRewards[i])
            } catch (e: IndexOutOfBoundsException) {
                newRewards.add(newReward)
            }
        }

        val overCount: Int = _rewards.size - updateCount
        if (overCount > 0) {
            for (i in updateCount until updateCount + overCount) {
                // 새로 변경된 rewards의 길이 index에서 요소를 계속 삭제
                _rewards.removeAt(updateCount)
            }
        }
        return newRewards
    }

    fun updateDetailQuests(detailQuestRequestDtos: List<DetailQuestRequestDto>): List<DetailQuest> {
        val newDetailQuests: MutableList<DetailQuestRequestDto> = mutableListOf()

        val updateCount = detailQuestRequestDtos.size
        for (i in 0 until updateCount) {
            val newDetailQuest = detailQuestRequestDtos[i]
            try {
                _detailQuests[i].updateDetailQuest(newDetailQuest)
            } catch (e: IndexOutOfBoundsException) {
                newDetailQuests.add(newDetailQuest)
            }
        }

        val overCount: Int = _detailQuests.size - updateCount
        if (overCount > 0) {
            for (i in updateCount until updateCount + overCount) {
                _detailQuests.removeAt(updateCount)
            }
        }

        return newDetailQuests.map { it.mapToEntity(this) }
            .toCollection(mutableListOf())
    }

    fun completeQuest() {
        require(state != QuestState.DELETE) { MessageUtil.getMessage("quest.error.deleted") }
        require(state == QuestState.PROCEED) { MessageUtil.getMessage("quest.error.not-proceed") }
        require(
            detailQuests.stream()
                .allMatch(DetailQuest::isCompletedDetailQuest)
        ) { MessageUtil.getMessage("quest.error.complete.detail") }

        state = QuestState.COMPLETE
    }

    fun deleteQuest() {
        state = QuestState.DELETE
    }

    fun discardQuest() {
        require(state != QuestState.DELETE) { MessageUtil.getMessage("quest.error.deleted") }
        state = QuestState.DISCARD
    }

    fun failQuest() {
        state = QuestState.FAIL
    }

    fun checkIsProceedingQuest() {
        require(state == QuestState.PROCEED) { MessageUtil.getMessage("quest.error.update.invalid.state") }
    }

    fun checkIsQuestOfValidUser(userId: Long) {
        if (user.id != userId) throw AccessDeniedException(
            MessageUtil.getMessage(
                "exception.access.denied",
                MessageUtil.getMessage("quest")
            )
        )

    }

}
