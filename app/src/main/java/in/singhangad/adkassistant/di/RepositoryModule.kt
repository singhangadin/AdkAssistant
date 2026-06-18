package `in`.singhangad.adkassistant.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import `in`.singhangad.adkassistant.data.repository.AssistantRepositoryImpl
import `in`.singhangad.adkassistant.data.repository.ConversationRepositoryImpl
import `in`.singhangad.adkassistant.data.repository.ModelRepositoryImpl
import `in`.singhangad.adkassistant.domain.repository.AssistantRepository
import `in`.singhangad.adkassistant.domain.repository.ConversationRepository
import `in`.singhangad.adkassistant.domain.repository.ModelRepository
import javax.inject.Singleton

/** Binds repository interfaces to their implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAssistantRepository(impl: AssistantRepositoryImpl): AssistantRepository

    @Binds
    @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindModelRepository(impl: ModelRepositoryImpl): ModelRepository
}
