package com.hossam.carenote.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "residents")
data class ResidentEntity(@PrimaryKey val id: String, val firstName: String, val lastName: String, val room: String)
