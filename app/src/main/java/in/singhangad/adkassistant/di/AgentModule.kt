package `in`.singhangad.adkassistant.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * The agent runner is built dynamically per selected model in
 * AssistantRepositoryImpl (so the model dropdown can switch it at runtime), so
 * this module only provides the IO dispatcher.
 */
@Module
@InstallIn(SingletonComponent::class)
object AgentModule {

    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
