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
import kotlinx.coroutines.flow.first
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

    init {
        fetchFavorites() // ✅ Load favorites initially
    }

//    val pagedPhotos = repository.getPagedPhotos().cachedIn(viewModelScope)

    fun fetchPhotos(page: Int) {
        viewModelScope.launch {
            isLoading.value = true
            repository.getPhotos(page).collectLatest { photosList ->
                val favoriteIds = _favorites.value.map { it.id }.toSet() // ✅ Always check latest favorites

                _photos.value = photosList.map {
                    it.copy(isFavorite = favoriteIds.contains(it.id)) // ✅ Ensure favorite status persists
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

                // ✅ Also update _photos list to reflect new favorites
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

            // ✅ Update repository (Database or API)
            repository.updateFavorite(updatedPhoto)

            // ✅ Fetch the latest favorites from repository to ensure sync
            val updatedFavorites = repository.getFavorites().first()

            withContext(Dispatchers.Main) {
                _favorites.value = updatedFavorites

                // ✅ Update _photos list to reflect favorite changes
                _photos.value = _photos.value.map {
                    it.copy(isFavorite = updatedFavorites.any { fav -> fav.id == it.id })
                }
            }
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
