package dailyquest.batch

import dailyquest.batch.job.DeadLineStepListener
import dailyquest.batch.job.ResetStepListener
import dailyquest.config.BatchConfig
import dailyquest.quest.entity.Quest
import dailyquest.quest.repository.QuestRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.batch.item.data.RepositoryItemWriter
import org.springframework.batch.item.function.FunctionItemProcessor
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.JobRepositoryTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@TestPropertySource(properties = ["spring.batch.job.enabled=false"])
@DisplayName("퀘스트 데드라인 스텝 단위 테스트")
@EnableAutoConfiguration
@SpringJUnitConfig(BatchConfig::class)
@SpringBatchTest
class DeadLineStepUnitTest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    private val jobRepositoryTestUtils: JobRepositoryTestUtils,
    private val questDeadLineBatchJob: Job,
) {

    @MockBean
    private lateinit var questRepository: QuestRepository

    @MockBean(name = "questDeadLineReader")
    private lateinit var questDeadLineReader: RepositoryItemReader<Quest>

    @MockBean(name = "questFailProcessor")
    private lateinit var questFailProcessor: FunctionItemProcessor<Quest, Quest>

    @MockBean(name = "questWriter")
    private lateinit var questWriter: RepositoryItemWriter<Quest>

    @MockBean
    private lateinit var resetStepListener: ResetStepListener

    @MockBean
    private lateinit var deadLineStepListener: DeadLineStepListener

    @BeforeEach
    fun clearMetadata() {
        jobRepositoryTestUtils.removeJobExecutions()
    }

    @DisplayName("읽기 과정에서 오류가 발생하면 에러 로그를 기록하고 스텝을 실패 처리한다")
    @Test
    fun `읽기 과정에서 오류가 발생하면 에러 로그를 기록하고 스텝을 실패 처리한다`() {
        //given
        jobLauncherTestUtils.job = questDeadLineBatchJob

        val targetDate = LocalDateTime.now().withSecond(0).withNano(0)
        val jobParameters = JobParametersBuilder()
            .addString("targetDate", targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .toJobParameters()

        doThrow(Exception("read error")).`when`(questDeadLineReader).read()
        doThrow(Exception("process error")).`when`(questFailProcessor).process(any())
        doThrow(Exception("write error")).`when`(questWriter).write(any())

        //when
        val jobExecution = jobLauncherTestUtils.launchStep("questDeadLineStep", jobParameters)

        //then
        assertThat(jobExecution.stepExecutions.first().exitStatus.exitCode).isEqualTo(ExitStatus.FAILED.exitCode)
        verify(deadLineStepListener, times(1)).onReadError(any())
        verify(deadLineStepListener, times(0)).onProcessError(any(), any())
        verify(deadLineStepListener, times(0)).onWriteError(any(), any())
    }

    @DisplayName("처리 과정이나 쓰기 과정에서 오류 발생 시 단계별로 3회까지 재시도 한다")
    @Test
    fun `처리 과정이나 쓰기 과정에서 오류 발생 시 단계별로 3회까지 재시도 한다`() {
        //given
        val mockQuest = mock<Quest>()
        jobLauncherTestUtils.job = questDeadLineBatchJob

        val targetDate = LocalDateTime.now().withSecond(0).withNano(0)
        val jobParameters = JobParametersBuilder()
            .addString("targetDate", targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .toJobParameters()

        doReturn(mockQuest, null).`when`(questDeadLineReader).read()
        doThrow(Exception("process error"), Exception("process error")).doReturn(mockQuest).`when`(questFailProcessor).process(any())
        doThrow(Exception("write error"), Exception("write error")).doNothing().`when`(questWriter).write(any())

        //when
        val jobExecution = jobLauncherTestUtils.launchStep("questDeadLineStep", jobParameters)

        //then
        assertThat(jobExecution.stepExecutions.first().exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)
        verify(deadLineStepListener, times(2)).onProcessError(any(), any())
        verify(deadLineStepListener, times(2)).onWriteError(any(), any())
    }

    @DisplayName("처리 과정이나 쓰기 과정에서 각 단계별로 3회를 초과한 오류 발생 시 스텝이 실패한다")
    @Test
    fun `처리 과정이나 쓰기 과정에서 각 단계별로 3회를 초과한 오류 발생 시 스텝이 실패한다`() {
        //given
        val mockQuest = mock<Quest>()
        jobLauncherTestUtils.job = questDeadLineBatchJob

        val targetDate = LocalDateTime.now().withSecond(0).withNano(0)
        val jobParameters = JobParametersBuilder()
            .addString("targetDate", targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .toJobParameters()

        doReturn(mockQuest, null).`when`(questDeadLineReader).read()
        val processError = Exception("process error")
        doThrow(processError, processError, processError).doReturn(mockQuest).`when`(questFailProcessor).process(any())
        doThrow(Exception("write error")).doNothing().`when`(questWriter).write(any())

        //when
        val jobExecution = jobLauncherTestUtils.launchStep("questDeadLineStep", jobParameters)

        //then
        assertThat(jobExecution.stepExecutions.first().exitStatus.exitCode).isEqualTo(ExitStatus.FAILED.exitCode)
        verify(deadLineStepListener, times(3)).onProcessError(any(), any())
        verify(deadLineStepListener, times(0)).onWriteError(any(), any())
    }

    @DisplayName("스텝 종료 후 listener를 통해 afterWrite 가 호출된다")
    @Test
    fun `스텝 종료 후 listener를 통해 afterWrite 가 호출된다`() {
        //given
        val mockQuest = mock<Quest>()
        jobLauncherTestUtils.job = questDeadLineBatchJob

        val targetDate = LocalDateTime.now().withSecond(0).withNano(0)
        val jobParameters = JobParametersBuilder()
            .addString("targetDate", targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .toJobParameters()

        doReturn(mockQuest, null).`when`(questDeadLineReader).read()
        doReturn(mockQuest).`when`(questFailProcessor).process(any())
        doNothing().`when`(questWriter).write(any())

        //when
        val jobExecution = jobLauncherTestUtils.launchStep("questDeadLineStep", jobParameters)

        //then
        assertThat(jobExecution.stepExecutions.first().exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)
        verify(deadLineStepListener).afterWrite(any())
    }

}