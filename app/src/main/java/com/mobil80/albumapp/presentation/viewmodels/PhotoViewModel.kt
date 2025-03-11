package com.mobil80.albumapp.presentation.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.mobil80.albumapp.core.NetworkObserver
import com.mobil80.albumapp.data.model.Photo
import com.mobil80.albumapp.data.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoViewModel(
    private val repository: PhotoRepository,
    private val networkObserver: NetworkObserver
) : ViewModel() {

    private val _favorites = MutableStateFlow<List<Photo>>(emptyList())
    val favorites: StateFlow<List<Photo>> = _favorites

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Photo>>(emptyList())
    val searchResults: StateFlow<List<Photo>> = _searchResults.asStateFlow()

    val isLoading = MutableStateFlow(true)
    val isFavoritesLoading = MutableStateFlow(true)

    // Track favorite states for paged photos
    private val _favoriteStates = mutableStateMapOf<String, Boolean>() // Photo ID to isFavorite

    init {
        fetchFavorites()
    }

    val isInternetAvailable: StateFlow<Boolean> = networkObserver.isInternetAvailable

    // Other ViewModel code...

    override fun onCleared() {
        super.onCleared()
        networkObserver.unregister()
    }

    val pagedPhotos = repository.getPagedPhotos().cachedIn(viewModelScope)

    fun fetchPhotos(page: Int) {
        viewModelScope.launch {
            isLoading.value = true
            repository.getPhotos(page).collectLatest { photosList ->
                val favoriteIds = _favorites.value.map { it.id }.toSet()
                _photos.value = photosList.map {
                    it.copy(isFavorite = favoriteIds.contains(it.id))
                }
                isLoading.value = false
            }
        }
    }

    fun fetchFavorites() {
        viewModelScope.launch {
            isFavoritesLoading.value = true
            repository.getFavorites().collectLatest { favList ->
                _favorites.value = favList
                _photos.value = _photos.value.map {
                    it.copy(isFavorite = favList.any { fav -> fav.id == it.id })
                }
                isFavoritesLoading.value = false
            }
        }
    }

    fun toggleFavorite(photo: Photo) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedPhoto = photo.copy(isFavorite = !photo.isFavorite)
            repository.updateFavorite(updatedPhoto)

            // Update favorites list
            val updatedFavorites = repository.getFavorites().first()
            _favorites.value = updatedFavorites

            // Update favorite state in the map
            _favoriteStates[photo.id] = updatedPhoto.isFavorite

            // Update photos list to reflect favorite changes
            _photos.value = _photos.value.map {
                if (it.id == photo.id) it.copy(isFavorite = updatedPhoto.isFavorite) else it
            }

            // Update search results if the photo is in the search results
            _searchResults.value = _searchResults.value.map {
                if (it.id == photo.id) it.copy(isFavorite = updatedPhoto.isFavorite) else it
            }
        }
    }

    fun searchPhotos(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = if (query.isEmpty()) {
                emptyList() // Clear search results if query is empty
            } else {
                repository.searchPhotos(query) // Fetch search results
            }
            _searchResults.value = results
        }
    }

    // Get the favorite state for a photo
    fun isFavorite(photoId: String): Boolean {
        return _favoriteStates[photoId] ?: false
    }
}