package `in`.singhangad.adkassistant.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.singhangad.adkassistant.BuildConfig
import `in`.singhangad.adkassistant.domain.repository.ModelRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : ModelRepository {

    private val prefs = context.getSharedPreferences("model_prefs", Context.MODE_PRIVATE)
    private val selected = MutableStateFlow(prefs.getString(KEY_SELECTED, DEFAULT_MODEL) ?: DEFAULT_MODEL)

    override fun observeSelectedModel(): Flow<String> = selected.asStateFlow()

    override fun selectedModel(): String = selected.value

    override suspend fun setSelectedModel(model: String) {
        selected.value = model
        prefs.edit { putString(KEY_SELECTED, model) }
    }

    override suspend fun availableModels(): List<String> = withContext(ioDispatcher) {
        val fetched = runCatching { fetchFromApi() }.getOrNull().orEmpty()
        val models = (if (fetched.isNotEmpty()) fetched else FALLBACK).toMutableList()
        // Always keep the current selection selectable, even if filtered out.
        if (selected.value !in models) models.add(0, selected.value)
        models
    }

    /** Lists models that support generateContent, filtered to chat-capable Geminis. */
    private fun fetchFromApi(): List<String> {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank()) return emptyList()

        val connection = (URL("$BASE_URL?pageSize=200").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("x-goog-api-key", key)
            connectTimeout = 8_000
            readTimeout = 8_000
        }
        return connection.inputStream.bufferedReader().use { reader ->
            val models = JSONObject(reader.readText()).optJSONArray("models") ?: return emptyList()
            buildList {
                for (i in 0 until models.length()) {
                    val model = models.getJSONObject(i)
                    val name = model.optString("name").removePrefix("models/")
                    val methods = model.optJSONArray("supportedGenerationMethods")
                    val supportsChat = methods != null &&
                        (0 until methods.length()).any { methods.getString(it) == "generateContent" }
                    if (supportsChat && name.startsWith("gemini") && EXCLUDE.none { it in name }) {
                        add(name)
                    }
                }
            }.distinct().sorted()
        }
    }

    companion object {
        const val DEFAULT_MODEL = "gemini-flash-latest"
        private const val KEY_SELECTED = "selected_model"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private val FALLBACK = listOf(
            "gemini-flash-latest",
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-2.5-pro",
        )
        // Keep the dropdown to text chat models — drop embeddings, TTS, image, etc.
        private val EXCLUDE = listOf("embedding", "aqa", "tts", "image", "imagen", "vision", "learnlm")
    }
}
