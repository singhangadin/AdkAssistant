package `in`.singhangad.adkassistant.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import `in`.singhangad.adkassistant.data.local.AdkDatabase
import `in`.singhangad.adkassistant.data.local.dao.ConversationDao
import `in`.singhangad.adkassistant.data.local.dao.MessageDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AdkDatabase =
        Room.databaseBuilder(context, AdkDatabase::class.java, "adk-assistant.db").build()

    @Provides
    fun provideConversationDao(db: AdkDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideMessageDao(db: AdkDatabase): MessageDao = db.messageDao()
}
