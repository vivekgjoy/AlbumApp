package com.mobil80.albumapp.presentation.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.mobil80.albumapp.R
import com.mobil80.albumapp.data.api.PhotoApi
import com.mobil80.albumapp.data.database.PhotoDatabase
import com.mobil80.albumapp.data.model.Photo
import com.mobil80.albumapp.data.repository.PhotoRepository
import com.mobil80.albumapp.presentation.ui.theme.AlbumAppTheme
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
            AlbumAppTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: PhotoViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "photos",
            modifier = Modifier.padding(paddingValues) // Ensure proper padding for content
        ) {
            composable("photos") { PhotoScreen(viewModel) }
            composable("favorites") { FavoritesScreen(viewModel) }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.height(56.dp),
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        BottomNavigationItem(
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Photos",
                    tint = if (currentRoute == "photos") colorResource(id = R.color.system_blue) else Color.Gray
                )
            },
            label = { Text("All Photos") },
            selected = currentRoute == "photos",
            onClick = { navController.navigate("photos") }
        )

        BottomNavigationItem(
            icon = {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Favorites",
                    tint = if (currentRoute == "favorites") colorResource(id = R.color.system_blue) else Color.Gray
                )
            },
            label = { Text("Favorites") },
            selected = currentRoute == "favorites",
            onClick = { navController.navigate("favorites") }
        )
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

        TextField(
            value = searchQuery.value,
            onValueChange = {
                searchQuery.value = it
                viewModel.searchPhotos(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
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
        shape = RoundedCornerShape(12.dp), // ✅ Apply corner radius to the Card itself
        elevation = CardDefaults.elevatedCardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)) // ✅ Ensures the image has rounded corners
            ) {
                AsyncImage(
                    model = photo.download_url,
                    contentDescription = "Photo by ${photo.author}",
                    contentScale = ContentScale.Crop, // ✅ Ensures the image fills the space properly
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically // ✅ Aligns text & button vertically
            ) {
                Text(
                    text = "Author: ${photo.author}",
                    maxLines = 2, // ✅ Ensures max two lines
                    overflow = TextOverflow.Ellipsis, // ✅ Truncates text if too long
                    modifier = Modifier.weight(1f) // ✅ Pushes the button to the end
                )
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
    val isLoading by viewModel.isFavoritesLoading.collectAsState()

    Column(modifier = Modifier.padding(8.dp)) {
        when {
            isLoading -> {
                LoadingIndicator()
            }

            favorites.isEmpty() -> {
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



