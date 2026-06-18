package `in`.singhangad.adkassistant.domain.repository

import kotlinx.coroutines.flow.Flow

/** Holds the user's model choice and the list of models they can pick from. */
interface ModelRepository {

    /** The currently selected Gemini model id (e.g. "gemini-flash-latest"). */
    fun observeSelectedModel(): Flow<String>

    /** The current selection, read once (used when starting a turn). */
    fun selectedModel(): String

    suspend fun setSelectedModel(model: String)

    /**
     * Models that actually support `generateContent` for this key. Fetched from
     * the API, with a curated fallback when the network/list is unavailable.
     */
    suspend fun availableModels(): List<String>
}
