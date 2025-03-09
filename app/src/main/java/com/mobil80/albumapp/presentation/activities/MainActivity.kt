package com.mobil80.albumapp.presentation.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mobil80.albumapp.data.api.PhotoApi
import com.mobil80.albumapp.data.database.PhotoDatabase
import com.mobil80.albumapp.data.model.Photo
import com.mobil80.albumapp.data.repository.PhotoRepository
import com.mobil80.albumapp.presentation.viewmodels.PhotoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val api = PhotoApi.create()
        val db = PhotoDatabase.getDatabase(this)
        val repository = PhotoRepository(api, db)
        val viewModel = PhotoViewModel(repository)

        viewModel.fetchPhotos(1)

        setContent {
            var showFavorites by remember { mutableStateOf(false) }

            Column {
                Row {
                    Button(onClick = { showFavorites = false }) { Text("All Photos") }
                    Button(onClick = { showFavorites = true }) { Text("Favorites") }
                }

                if (showFavorites) {
                    FavoritesScreen(viewModel)
                } else {
                    PhotoScreen(viewModel)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoScreen(viewModel: PhotoViewModel) {
    val photos by viewModel.photos.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery = remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState() // Observe loading state

    LaunchedEffect(Unit) {
        viewModel.fetchPhotos(1)
    }

    Column(modifier = Modifier.padding(8.dp)) {
        // Search Bar with Search & Clear Icons + Rounded Corners
        TextField(
            value = searchQuery.value,
            onValueChange = {
                searchQuery.value = it
                viewModel.searchPhotos(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray.copy(alpha = 0.3f)),
            placeholder = { Text("Search by ID or Author") },
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            trailingIcon = {
                Row {
                    if (searchQuery.value.isNotEmpty()) {
                        IconButton(onClick = { searchQuery.value = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        val displayedPhotos = if (searchQuery.value.isEmpty()) photos else searchResults
        when {
            isLoading -> {
                // Show Loading Indicator (3 Dots Animation)
                LoadingIndicator()
            }

            displayedPhotos.isEmpty() -> {
                // Show No Items Found Text
                Text(
                    text = "No items found",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            else -> {
                LazyColumn {
                    items(displayedPhotos) { photo ->
                        PhotoItem(photo, onFavoriteClick = { viewModel.toggleFavorite(photo) })
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoItem(photo: Photo, onFavoriteClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column {
            AsyncImage(
                model = photo.download_url,
                contentDescription = "Photo by ${photo.author}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Row(modifier = Modifier.padding(8.dp)) {
                Text(text = "Author: ${photo.author}")
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (photo.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (photo.isFavorite) Color.Red else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(viewModel: PhotoViewModel) {
    val favorites by viewModel.favorites.collectAsState()
    val isLoading by viewModel.isFavoritesLoading.collectAsState() // Observe loading state

    Column(modifier = Modifier.padding(8.dp)) {
        when {
            isLoading -> {
                // Show Loading Indicator (3 Dots Animation)
                LoadingIndicator()
            }

            favorites.isEmpty() -> {
                // Show No Favorites Found Text
                Text(
                    text = "No favorites found",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            else -> {
                LazyColumn {
                    items(favorites) { photo ->
                        PhotoItem(photo, onFavoriteClick = { viewModel.toggleFavorite(photo) })
                    }
                }
            }
        }
    }
}

// Loading Indicator (3 Dots Animation)
@Composable
fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}



