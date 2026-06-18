package `in`.singhangad.adkassistant.domain.model

/** Progress signal emitted while the agent is working on a turn. */
sealed interface AgentStatus {
    data object Thinking : AgentStatus
    data class Retrying(val attempt: Int, val maxAttempts: Int) : AgentStatus
}
