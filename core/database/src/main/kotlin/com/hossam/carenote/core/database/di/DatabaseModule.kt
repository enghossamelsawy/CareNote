package com.hossam.carenote.core.database.di

import android.content.Context
import androidx.room.Room
import com.hossam.carenote.core.database.CareNoteDatabase
import com.hossam.carenote.core.database.Migrations
import com.hossam.carenote.core.database.dao.CareNoteDao
import com.hossam.carenote.core.database.dao.OutboxDao
import com.hossam.carenote.core.database.dao.ResidentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CareNoteDatabase = Room
        .databaseBuilder(context, CareNoteDatabase::class.java, "care_note.db")
        .addMigrations(*Migrations.all)
        .build()

    @Provides
    fun provideResidentDao(database: CareNoteDatabase): ResidentDao = database.residentDao()

    @Provides
    fun provideCareNoteDao(database: CareNoteDatabase): CareNoteDao = database.careNoteDao()

    @Provides
    fun provideOutboxDao(database: CareNoteDatabase): OutboxDao = database.outboxDao()
}
