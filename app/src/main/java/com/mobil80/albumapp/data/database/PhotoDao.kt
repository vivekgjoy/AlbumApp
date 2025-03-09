package com.mobil80.albumapp.data.database

import androidx.room.*
import com.mobil80.albumapp.data.model.Photo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Dao
interface PhotoDao {

    @Transaction
    suspend fun insertOrUpdatePhotos(photos: List<Photo>) {
        val existingPhotos = getAllPhotos().first() // Get the current photos from DB
        val favoriteIds = existingPhotos.filter { it.isFavorite }.map { it.id }.toSet()

        val updatedPhotos = photos.map {
            if (favoriteIds.contains(it.id)) it.copy(isFavorite = true) // Keep it favorite
            else it
        }

        insertPhotos(updatedPhotos) // Now insert with updated favorite status
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPhotos(photos: List<Photo>)

    @Query("SELECT * FROM photos")
    fun getAllPhotos(): Flow<List<Photo>> // ✅ Must return Flow for Room + Flow support

    @Query("SELECT * FROM photos WHERE isFavorite = 1")
    fun getFavoritePhotos(): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE author LIKE '%' || :query || '%' OR id LIKE '%' || :query || '%'")
    fun searchPhotos(query: String): List<Photo>

    @Update
    fun updatePhoto(photo: Photo)
}
