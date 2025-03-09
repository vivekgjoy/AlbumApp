package com.mobil80.albumapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class Photo(
    @PrimaryKey val id: String,
    val author: String,
    val download_url: String,
    var isFavorite: Boolean = false
)


