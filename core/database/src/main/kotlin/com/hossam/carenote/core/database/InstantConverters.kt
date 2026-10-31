package com.hossam.carenote.core.database

import androidx.room.TypeConverter
import java.time.Instant

/** Stores an [Instant] as epoch milliseconds. */
internal class InstantConverters {
    @TypeConverter
    fun fromEpochMillis(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun toEpochMillis(instant: Instant?): Long? = instant?.toEpochMilli()
}
