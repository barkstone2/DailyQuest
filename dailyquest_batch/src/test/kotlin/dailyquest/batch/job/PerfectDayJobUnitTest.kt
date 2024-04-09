package dailyquest.batch.job

import com.ninjasquad.springmockk.MockkBean
import dailyquest.achievement.entity.Achievement
import dailyquest.achievement.entity.AchievementAchieveLog
import dailyquest.achievement.repository.AchievementAchieveLogRepository
import dailyquest.achievement.repository.AchievementRepository
import dailyquest.batch.listener.job.PerfectDayJobListener
import dailyquest.batch.listener.step.AchievementAchieveNotificationStepListener
import dailyquest.batch.listener.step.IncreasePerfectDayCountStepListener
import dailyquest.batch.listener.step.PerfectDayAchievementStepListener
import dailyquest.batch.listener.step.ReadPerfectDayUserIdStepListener
import dailyquest.batch.step.AchievementAchieveNotificationStepConfig
import dailyquest.batch.step.IncreasePerfectDayCountStepConfig
import dailyquest.batch.step.PerfectDayAchievementStepConfig
import dailyquest.batch.step.ReadPerfectDayUserIdStepConfig
import dailyquest.common.util.WebApiUtil
import dailyquest.notification.entity.Notification
import dailyquest.notification.repository.NotificationRepository
import dailyquest.quest.repository.QuestLogRepository
import dailyquest.user.dto.UserPerfectDayCount
import dailyquest.user.entity.User
import dailyquest.user.repository.UserRepository
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.JobRepositoryTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@Import(
    PerfectDayBatchConfig::class,
    ReadPerfectDayUserIdStepConfig::class,
    IncreasePerfectDayCountStepConfig::class,
    PerfectDayAchievementStepConfig::class,
    AchievementAchieveNotificationStepConfig::class,
    ReadPerfectDayUserIdStepListener::class,
    IncreasePerfectDayCountStepListener::class,
    PerfectDayAchievementStepListener::class,
    AchievementAchieveNotificationStepListener::class,
    PerfectDayJobListener::class
)
@EnableAutoConfiguration
@SpringBatchTest
@DisplayName("완벽한 하루 작업 유닛 테스트")
class PerfectDayJobUnitTest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    private val jobRepositoryTestUtils: JobRepositoryTestUtils,
    private val perfectDayBatchJob: Job
) {
    @MockkBean(relaxed = true)
    private lateinit var questLogRepository: QuestLogRepository
    @MockkBean(relaxed = true)
    private lateinit var userRepository: UserRepository
    @MockkBean(relaxed = true)
    private lateinit var achievementRepository: AchievementRepository
    @MockkBean(relaxed = true)
    private lateinit var achievementAchieveLogRepository: AchievementAchieveLogRepository
    @MockkBean(relaxed = true)
    private lateinit var notificationRepository: NotificationRepository
    @MockkBean(relaxed = true)
    private lateinit var webApiUtil: WebApiUtil

    private val jobParameters: JobParameters =
        JobParametersBuilder().addLocalDate("loggedDate", LocalDate.now()).toJobParameters()
    private val perfectDayUserIds = listOf(1L, 2L, 3L)
    private val achievedUserIds = listOf(1L, 2L)
    private val achievement: Achievement = mockk(relaxed = true)
    private val user: User = mockk(relaxed = true)
    private val userIdsKey = "perfectDayLogUserIds"
    private val userPerfectDayCountsKey = "userPerfectDayCounts"
    private val perfectDayAchievementsKey = "perfectDayAchievements"
    private val achievedLogsKey = "achievedLogs"
    private val notifiedUserIdsKey = "notifiedUserIds"

    @BeforeEach
    fun init() {
        jobRepositoryTestUtils.removeJobExecutions()
        jobLauncherTestUtils.job = perfectDayBatchJob
        every {
            questLogRepository.getAllUserIdsWhoAchievedPerfectDay(any(), any())
        } returns PageImpl(perfectDayUserIds) andThen Page.empty()
        every { achievementRepository.getAllActivatedOfType(any()) } returns listOf(achievement, achievement, achievement)
        every {
            userRepository.findAllByIdIn(any(), any())
        } returns PageImpl(perfectDayUserIds.map { user }) andThen Page.empty()
        every { user.id } returnsMany perfectDayUserIds
        every { achievement.canAchieve(any()) } returnsMany achievedUserIds.map { true } andThen false
    }

    @DisplayName("완벽한 하루를 달성한 유저 ID를 JobExecutionContext에 저장한다")
    @Test
    fun `완벽한 하루를 달성한 유저 ID를 JobExecutionContext에 저장한다`() {
        //given
        //when
        val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)

        //then
        val result = jobExecution.executionContext.get(userIdsKey)
        assertThat(result).isEqualTo(perfectDayUserIds)
    }

    @DisplayName("JobExecutionContext에 저장된 완벽한 하루 달성 유저ID로 유저 엔티티를 조회한다")
    @Test
    fun `JobExecutionContext에 저장된 완벽한 하루 달성 유저ID로 유저 엔티티를 조회한다`() {
        //given
        //when
        jobLauncherTestUtils.launchJob(jobParameters)

        //then
        verify { userRepository.findAllByIdIn(eq(perfectDayUserIds), any()) }
    }

    @DisplayName("완벽한 하루 달성 유저ID로 조회한 유저 엔티티의 완벽한 하루 횟수를 증가시킨다")
    @Test
    fun `완벽한 하루 달성 유저ID로 조회한 유저 엔티티의 완벽한 하루 횟수를 증가시킨다`() {
        //given
        //when
        jobLauncherTestUtils.launchJob(jobParameters)

        //then
        verify(exactly = perfectDayUserIds.size) { user.increasePerfectDayCount() }
    }

    @DisplayName("완벽한 하루 횟수를 증가시킨 유저 정보를 JobExecutionContext에 담는다")
    @Test
    fun `완벽한 하루 횟수를 증가시킨 유저 정보를 JobExecutionContext에 담는다`() {
        //given
        //when
        val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)

        //then
        val result = jobExecution.executionContext.get(userPerfectDayCountsKey) as List<UserPerfectDayCount>
        assertThat(result.size).isEqualTo(perfectDayUserIds.size)
    }

    @DisplayName("JobExecutionContext에 담긴 유저 ID가 없어도 Job이 성공한다")
    @Test
    fun `JobExecutionContext에 담긴 유저 ID가 없어도 Job이 성공한다`() {
        //given
        every {
            questLogRepository.getAllUserIdsWhoAchievedPerfectDay(any(), any())
        } returns Page.empty()

        //when
        val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)

        //then
        assertThat(jobExecution.status).isEqualTo(BatchStatus.COMPLETED)
    }

    @DisplayName("달성 가능한 업적이 있으면 업적 달성 로그를 등록한다")
    @Test
    fun `달성 가능한 업적이 있으면 업적 달성 로그를 등록한다`() {
        //given
        //when
        jobLauncherTestUtils.launchJob(jobParameters)

        //then
        verify { achievementAchieveLogRepository.saveAll<AchievementAchieveLog>(match { list -> list.all { achievedUserIds.contains(it.userId) } }) }
    }

    @DisplayName("업적 달성 로그가 저장된 유저에 대해 알림을 저장한다")
    @Test
    fun `업적 달성 로그가 저장된 유저에 대해 알림을 저장한다`() {
        //given
        //when
        jobLauncherTestUtils.launchJob(jobParameters)

        //then
        verify { notificationRepository.saveAll<Notification>(match { list -> list.all { achievedUserIds.contains(it.userId) } }) }
    }

    @DisplayName("알림이 저장된 유저에 대해 SSE 전송 요청을 보낸다")
    @Test
    fun `알림이 저장된 유저에 대해 SSE 전송 요청을 보낸다`() {
        //given
        //when
        jobLauncherTestUtils.launchJob(jobParameters)

        //then
        verify { webApiUtil.postSseNotify(match { achievedUserIds.containsAll(it) }) }
    }
}