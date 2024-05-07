package dailyquest.common.util

import org.springframework.batch.core.StepExecution

@Suppress("UNCHECKED_CAST")
class ExecutionContextUtil private constructor(
    private val stepExecution: StepExecution,
) {
    private lateinit var jobExecutionContextUtil: JobExecutionContextUtil

    @Throws(ClassCastException::class)
    fun <T> getFromStepContext(key: String): T? {
        return stepExecution.executionContext.get(key) as? T
    }

    fun putToStepContext(key: String, value: Any) {
        stepExecution.executionContext.put(key, value)
    }

    @Throws(ClassCastException::class)
    fun <T> getFromJobContext(key: String): T? {
        return jobExecutionContextUtil.getFromJobContext(key)
    }

    fun putToJobContext(key: String, value: Any) {
        jobExecutionContextUtil.putToJobContext(key, value)
    }

    fun <E> mergeListFromStepContextToJobContext(key: String) {
        val fromJobContext = this.getFromJobContext<List<E>?>(key)?.toMutableList() ?: mutableListOf()
        val fromStepContext = this.getFromStepContext<List<E>>(key)
        fromStepContext?.let { fromJobContext.addAll(it) }
        this.putToJobContext(key, fromJobContext)
    }

    fun removeFromStepContext(key: String) {
        stepExecution.executionContext.remove(key)
    }

    fun containsKeyOnStepContext(key: String): Boolean {
        return stepExecution.executionContext.containsKey(key)
    }

    companion object {
        @JvmStatic
        fun from(stepExecution: StepExecution): ExecutionContextUtil {
            val executionContextUtil = ExecutionContextUtil(stepExecution)
            executionContextUtil.jobExecutionContextUtil = JobExecutionContextUtil.from(stepExecution.jobExecution)
            return executionContextUtil
        }
    }
}