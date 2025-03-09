package com.mobil80.albumapp.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.mobil80.albumapp.data.model.Photo
import com.mobil80.albumapp.data.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoViewModel(private val repository: PhotoRepository) : ViewModel() {

    private val _favorites = MutableStateFlow<List<Photo>>(emptyList())
    val favorites: StateFlow<List<Photo>> = _favorites

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Photo>>(emptyList())
    val searchResults: StateFlow<List<Photo>> = _searchResults.asStateFlow()

    val isLoading = MutableStateFlow(true)

    val isFavoritesLoading = MutableStateFlow(true) // Add this to ViewModel

    fun fetchPhotos(page: Int) {
        viewModelScope.launch {
            isLoading.value = true
            repository.getPhotos(page).collectLatest { photosList ->
                _photos.value = photosList // ✅ Update UI
                isLoading.value = false
            }
        }
    }

    fun toggleFavorite(photo: Photo) {
        viewModelScope.launch(Dispatchers.IO) {
            photo.isFavorite = !photo.isFavorite
            isFavoritesLoading.value = true
            repository.updateFavorite(photo)
            _favorites.value = repository.getFavorites()
            isFavoritesLoading.value = false
        }
    }

    fun searchPhotos(query: String) {
        viewModelScope.launch(Dispatchers.IO) { // ✅ Run in background thread
            val results = repository.searchPhotos(query)

            withContext(Dispatchers.Main) { // ✅ Update UI on main thread
                _searchResults.value = results
            }
        }
    }
}
