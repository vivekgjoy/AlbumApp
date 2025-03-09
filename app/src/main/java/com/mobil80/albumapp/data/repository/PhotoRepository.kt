package com.mobil80.albumapp.data.repository

import android.util.Log
import com.mobil80.albumapp.data.api.PhotoApi
import com.mobil80.albumapp.data.database.PhotoDao
import com.mobil80.albumapp.data.database.PhotoDatabase
import com.mobil80.albumapp.data.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class PhotoRepository(private val api: PhotoApi, private val db: PhotoDatabase) {

    private val photoDao = db.photoDao()

    fun getPhotos(page: Int): Flow<List<Photo>> = flow {
        try {
            val response = api.getPhotos(page)
            Log.d("viv", "API Success: ${response.size} photos")
            if (response.isNotEmpty()) {
                photoDao.insertPhotos(response)
                emit(response)
                return@flow
            }
        } catch (e: Exception) {
            Log.e("PhotoRepository", "API failed, trying DB", e)
        }

        val dbPhotos = photoDao.getAllPhotos().first()
        Log.d("viv", "DB Loaded: ${dbPhotos.size} photos")
        emit(dbPhotos)
    }.flowOn(Dispatchers.IO)

    suspend fun updateFavorite(photo: Photo) {
        photoDao.updatePhoto(photo)
    }

    fun getFavorites(): Flow<List<Photo>> = photoDao.getFavoritePhotos()

    fun searchPhotos(query: String): List<Photo> = photoDao.searchPhotos(query)
}

