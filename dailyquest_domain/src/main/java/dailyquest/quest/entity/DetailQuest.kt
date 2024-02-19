package dailyquest.quest.entity

import jakarta.persistence.*
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.GenerationType.IDENTITY

@Entity
@Table(name = "detail_quest")
class DetailQuest(
    title: String,
    targetCount: Int,
    type: DetailQuestType,
    state: DetailQuestState,
    quest: Quest,
) {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "detail_quest_id")
    val id: Long = 0

    @Column(nullable = false, length = 100)
    var title: String = title
        protected set

    @Column(nullable = false)
    var targetCount: Int = targetCount
        protected set

    @Column(nullable = false)
    var count: Int = 0
        protected set

    @Enumerated(STRING)
    @Column(nullable = false)
    var type: DetailQuestType = type
        protected set

    @Enumerated(STRING)
    @Column(nullable = false)
    var state: DetailQuestState = state
        protected set

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "quest_id")
    val quest: Quest = quest

    fun resetCount() {
        count = 0
        state = DetailQuestState.PROCEED
    }

    fun addCount() {
        count++
        if (count == targetCount) state = DetailQuestState.COMPLETE
    }

    fun changeCount(count: Int) {
        this.count = if(count > targetCount) targetCount else count

        state = when {
            count < targetCount -> DetailQuestState.PROCEED
            else -> DetailQuestState.COMPLETE
        }
    }

    fun isCompletedDetailQuest() : Boolean {
        return state == DetailQuestState.COMPLETE
    }

    fun interact(count: Int?) {
        if(count != null) {
            this.changeCount(count)
        } else if(this.isCompletedDetailQuest()) {
            this.resetCount()
        } else {
            this.addCount()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DetailQuest

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}