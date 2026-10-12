package com.cdiacal.tasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Optional emoji shown in the category icon; empty = use the name's initial. */
    val emoji: String = "",
)
