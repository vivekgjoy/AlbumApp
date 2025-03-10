package com.mobil80.albumapp.data.helper

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mobil80.albumapp.data.api.PhotoApi
import com.mobil80.albumapp.data.database.PhotoDatabase
import com.mobil80.albumapp.data.model.Photo
import kotlinx.coroutines.flow.firstOrNull

class PhotoPagingSource(
    private val api: PhotoApi,
    private val db: PhotoDatabase
) : PagingSource<Int, Photo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Photo> {
        val page = params.key ?: 1
        return try {
            val response = api.getPhotos(page, params.loadSize)
            db.photoDao().insertOrUpdatePhotos(response)

            LoadResult.Page(
                data = response,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            // If API fails, load from DB
            val cachedPhotos = db.photoDao().getAllPhotos().firstOrNull() ?: emptyList()
            LoadResult.Page(
                data = cachedPhotos,
                prevKey = null,
                nextKey = if (cachedPhotos.isEmpty()) null else page + 1
            )
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Photo>): Int? {
        return state.anchorPosition?.let { state.closestPageToPosition(it)?.prevKey }
    }
}


