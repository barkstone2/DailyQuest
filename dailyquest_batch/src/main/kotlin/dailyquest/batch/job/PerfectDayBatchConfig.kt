package dailyquest.batch.job

import dailyquest.batch.listener.job.PerfectDayJobListener
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PerfectDayBatchConfig {
    @Bean
    fun perfectDayBatchJob(
        jobRepository: JobRepository,
        perfectDayLogStep: Step,
        perfectDayAchievementStep: Step,
        achievementAchieveNotificationStep: Step,
        perfectDayJobListener: PerfectDayJobListener
    ): Job {
        return JobBuilder("perfectDayJob", jobRepository)
            .start(perfectDayLogStep)
            .next(perfectDayAchievementStep)
            .next(achievementAchieveNotificationStep)
            .listener(perfectDayJobListener)
            .build()
    }
}