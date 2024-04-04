package dailyquest.user.entity

import dailyquest.common.BaseTimeEntity
import dailyquest.user.dto.UserUpdateRequest
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.max

@Entity
@Table(name = "users", uniqueConstraints = [UniqueConstraint(name = "unique_nickname", columnNames = ["nickname"])])
class User(
    oauth2Id: String,
    nickname: String,
    providerType: ProviderType,
    coreTime: LocalTime = LocalTime.of(8, 0, 0),
    coreTimeLastModifiedDate: LocalDateTime? = null,
    questRegistrationCount: Int = 0,
    questCompletionCount: Int = 0,
    currentQuestContinuousRegistrationDays: Int = 0,
    currentQuestContinuousCompletionDays: Int = 0,
    maxQuestContinuousRegistrationDays: Int = 0,
    maxQuestContinuousCompletionDays: Int = 0,
    lastQuestRegistrationDate: LocalDate? = null,
    lastQuestCompletionDate: LocalDate? = null,
    perfectDayCount: Int = 0,
    goldEarnAmount: Int = 0,
    goldUseAmount: Int = 0,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val id: Long = 0

    @Column(nullable = false)
    val oauth2Id: String = oauth2Id

    @Column(nullable = false, length = 20)
    var nickname: String = nickname
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val providerType: ProviderType = providerType

    @Column(nullable = false)
    var coreTime: LocalTime = coreTime
        protected set

    @Column(name = "core_time_last_modified_date")
    var coreTimeLastModifiedDate: LocalDateTime? = coreTimeLastModifiedDate
        protected set

    @Column(nullable = false)
    var exp: Long = 0
        protected set

    @Column(nullable = false)
    var gold: Long = 0
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: RoleType = RoleType.USER

    @Column(name = "quest_registration_count", nullable = false)
    var questRegistrationCount: Int = questRegistrationCount
        protected set

    @Column(name = "quest_completion_count", nullable = false)
    var questCompletionCount: Int = questCompletionCount
        protected set

    @Column(name = "current_quest_continuous_registration_days", nullable = false)
    var currentQuestContinuousRegistrationDays: Int = currentQuestContinuousRegistrationDays
        protected set

    @Column(name = "current_quest_continuous_completion_days", nullable = false)
    var currentQuestContinuousCompletionDays: Int = currentQuestContinuousCompletionDays
        protected set

    @Column(name = "max_quest_continuous_registration_days", nullable = false)
    var maxQuestContinuousRegistrationDays: Int = maxQuestContinuousRegistrationDays
        protected set

    @Column(name = "max_quest_continuous_completion_days", nullable = false)
    var maxQuestContinuousCompletionDays: Int = maxQuestContinuousCompletionDays
        protected set

    @Column(name = "last_quest_registration_date")
    var lastQuestRegistrationDate: LocalDate? = lastQuestRegistrationDate
        protected set

    @Column(name = "last_quest_completion_date")
    var lastQuestCompletionDate: LocalDate? = lastQuestCompletionDate
        protected set

    @Column(name = "perfect_day_count", nullable = false)
    var perfectDayCount: Int = perfectDayCount
        protected set

    @Column(name = "gold_earn_amount", nullable = false)
    var goldEarnAmount: Int = goldEarnAmount
        protected set

    @Column(name = "gold_use_amount", nullable = false)
    var goldUseAmount: Int = goldUseAmount
        protected set

    fun updateUser(updateRequest: UserUpdateRequest): Boolean {
        if (this.updateCoreTime(updateRequest.coreTime)) {
            this.updateNickname(updateRequest.nickname)
            return true
        }
        return false
    }

    fun updateNickname(nickname: String?) {
        if (nickname != null) {
            this.nickname = nickname
        }
    }

    fun updateCoreTime(coreTime: Int?): Boolean {
        if (this.isSameOrNullCoreTime(coreTime)) return true
        if (this.canUpdateCoreTime()) {
            this.coreTime = LocalTime.of(coreTime!!, 0, 0)
            coreTimeLastModifiedDate = LocalDateTime.now()
            return true
        }
        return false
    }

    private fun isSameOrNullCoreTime(coreTime: Int?) = coreTime == null || coreTime == getCoreHour()

    private fun canUpdateCoreTime(): Boolean {
        if (this.coreTimeLastModifiedDate == null) return true
        val updateAvailableDateTime = this.getUpdateAvailableDateTimeOfCoreTime()
        val now = LocalDateTime.now()
        return now.isAfter(updateAvailableDateTime)
    }

    fun getUpdateAvailableDateTimeOfCoreTime(): LocalDateTime {
        return coreTimeLastModifiedDate?.plusDays(1L) ?: LocalDateTime.now()
    }

    fun addExpAndGold(earnedExp: Long, earnedGold: Long) {
        this.exp += earnedExp
        this.gold += earnedGold
    }

    fun isNowCoreTime() : Boolean {
        val now = LocalDateTime.now()
        val coreTimeOfToday = LocalDateTime.of(LocalDate.now(), coreTime)
        if (coreTimeOfToday.isAfter(now) || now.isAfter(coreTimeOfToday.plusHours(1))) return false;
        return true
    }

    fun calculateLevel(expTable: Map<Int, Long>): Triple<Int, Long, Long> {
        var level = 1
        var remainingExp = exp
        var requiredExp = 0L

        expTable.keys.sorted().forEach { key ->
            requiredExp = expTable[key] ?: return@forEach

            if (requiredExp == 0L) return@forEach

            if (remainingExp >= requiredExp) {
                remainingExp -= requiredExp
                level++
            } else {
                return Triple(level, remainingExp, requiredExp)
            }
        }

        return Triple(level, remainingExp, requiredExp)
    }

    fun getCoreHour(): Int {
        return coreTime.hour
    }

    fun increaseQuestRegistrationCount(registrationDate: LocalDate) {
        this.questRegistrationCount++
        if (this.isContinuousRegistration(registrationDate)) {
            this.increaseCurrentQuestContinuousRegistrationDays()
        }
    }

    private fun isContinuousRegistration(registrationDate: LocalDate) =
        lastQuestRegistrationDate == null || lastQuestRegistrationDate!!.isEqual(registrationDate.minusDays(1))

    fun increaseQuestCompletionCount(completionDate: LocalDate) {
        this.questCompletionCount++
        if (this.isContinuousCompletion(completionDate)) {
            this.increaseCurrentQuestContinuousCompletionDays()
        }
    }

    private fun isContinuousCompletion(completionDate: LocalDate) =
        lastQuestCompletionDate == null || lastQuestCompletionDate!!.isEqual(completionDate.minusDays(1))

    fun increaseCurrentQuestContinuousRegistrationDays() {
        this.currentQuestContinuousRegistrationDays++
        this.maxQuestContinuousRegistrationDays = max(maxQuestContinuousRegistrationDays, currentQuestContinuousRegistrationDays)
    }

    fun increaseCurrentQuestContinuousCompletionDays() {
        this.currentQuestContinuousCompletionDays++
        this.maxQuestContinuousCompletionDays = max(maxQuestContinuousCompletionDays, currentQuestContinuousCompletionDays)
    }

    fun increasePerfectDayCount() {
        this.perfectDayCount++
    }

    fun addGoldEarnAmount(goldEarnAmount: Int) {
        this.goldEarnAmount += goldEarnAmount
    }

    fun addGoldUseAmount(goldUseAmount: Int) {
        this.goldUseAmount += goldUseAmount
    }
}