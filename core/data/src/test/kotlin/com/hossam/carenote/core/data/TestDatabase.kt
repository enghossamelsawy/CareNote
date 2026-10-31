package com.hossam.carenote.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hossam.carenote.core.database.CareNoteDatabase

/** A fresh in-memory database; main-thread queries are allowed in tests only. */
fun inMemoryDatabase(): CareNoteDatabase = Room
    .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), CareNoteDatabase::class.java)
    .allowMainThreadQueries()
    .build()
