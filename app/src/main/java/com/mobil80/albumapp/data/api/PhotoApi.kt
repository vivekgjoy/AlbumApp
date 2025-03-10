package com.mobil80.albumapp.data.api

import com.mobil80.albumapp.data.model.Photo
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface PhotoApi {
    @GET("v2/list")
    suspend fun getPhotos(
        @Query("page") page: Int,
        @Query("limit") limit: Int = 100
    ): List<Photo>

    companion object {
        fun create(): PhotoApi {
            return Retrofit.Builder()
                .baseUrl("https://picsum.photos/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PhotoApi::class.java)
        }
    }
}
