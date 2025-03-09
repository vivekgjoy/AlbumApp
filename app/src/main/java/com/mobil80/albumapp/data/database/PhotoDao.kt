package com.mobil80.albumapp.data.database

import androidx.room.*
import com.mobil80.albumapp.data.model.Photo
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPhotos(photos: List<Photo>)

    @Query("SELECT * FROM photos")
    fun getAllPhotos(): Flow<List<Photo>> // ✅ Must return Flow for Room + Flow support

    @Query("SELECT * FROM photos WHERE isFavorite = 1")
    fun getFavoritePhotos(): List<Photo>

    @Query("SELECT * FROM photos WHERE author LIKE '%' || :query || '%' OR id LIKE '%' || :query || '%'")
    fun searchPhotos(query: String): List<Photo>

    @Update
    fun updatePhoto(photo: Photo)
}
