package edu.singaporetech.inf2007quiz01.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import edu.singaporetech.inf2007quiz01.data.AppDatabase
import edu.singaporetech.inf2007quiz01.data.ExpressionHistoryDao
import edu.singaporetech.inf2007quiz01.data.MathJsApi
import edu.singaporetech.inf2007quiz01.data.PreferencesManager
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "calbot_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideDao(database: AppDatabase): ExpressionHistoryDao {
        return database.expressionHistoryDao()
    }

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.mathjs.org/v4/")
            .build()
    }

    @Provides
    @Singleton
    fun provideMathJsApi(retrofit: Retrofit): MathJsApi {
        return retrofit.create(MathJsApi::class.java)
    }

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }
}
