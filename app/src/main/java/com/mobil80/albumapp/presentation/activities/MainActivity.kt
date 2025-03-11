package com.mobil80.albumapp.presentation.activities

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.mobil80.albumapp.R
import com.mobil80.albumapp.core.NetworkObserver
import com.mobil80.albumapp.data.api.PhotoApi
import com.mobil80.albumapp.data.database.PhotoDatabase
import com.mobil80.albumapp.data.model.Photo
import com.mobil80.albumapp.data.repository.PhotoRepository
import com.mobil80.albumapp.presentation.helper.Functions.showToast
import com.mobil80.albumapp.presentation.ui.theme.AlbumAppTheme
import com.mobil80.albumapp.presentation.viewmodels.PhotoViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize dependencies
        val api = PhotoApi.create()
        val db = PhotoDatabase.getDatabase(this)
        val repository = PhotoRepository(api, db)

        // Initialize NetworkObserver
        val networkObserver = NetworkObserver(this)

        // Initialize ViewModel
        val viewModel = PhotoViewModel(repository, networkObserver)

        // Fetch initial photos
        viewModel.fetchPhotos(1)

        setContent {
            AlbumAppTheme {
                MainScreen(viewModel, LocalContext.current)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: PhotoViewModel, context: Context) {
    val navController = rememberNavController()

    // State to control the visibility of the exit dialog
    var showExitDialog by remember { mutableStateOf(false) }

    // Handle back press
    BackHandler(enabled = true) {
        showExitDialog = true // Show the exit dialog when back is pressed
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController) // Use the custom BottomNavigationBar
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "photos", // Set the default route to "photos"
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("photos") {
                PhotoScreen(viewModel, LocalContext.current)
            }
            composable("favorites") {
                FavoritesScreen(viewModel)
            }
        }
    }

    // Exit Confirmation Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                showExitDialog = false // Hide the dialog when dismissed
            },
            title = {
                Text(text = "Exit App", style = MaterialTheme.typography.headlineSmall)
            },
            text = {
                Text(text = "Are you sure you want to exit the app?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Exit the app
                        (context as Activity).finish()
                    }
                ) {
                    Text("Yes", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false // Hide the dialog
                    }
                ) {
                    Text("No", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
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
            onClick = {
                navController.navigate("photos") {
                    popUpTo(navController.graph.startDestinationId) // Pop backstack to avoid multiple instances
                    launchSingleTop = true
                }
            }
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
            onClick = {
                navController.navigate("favorites") {
                    popUpTo(navController.graph.startDestinationId) // Pop backstack to avoid multiple instances
                    launchSingleTop = true
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoScreen(viewModel: PhotoViewModel, context: Context) {
    val lazyPhotos = viewModel.pagedPhotos.collectAsLazyPagingItems()
    val searchQuery = remember { mutableStateOf("") }
    val isRefreshing by remember { derivedStateOf { lazyPhotos.loadState.refresh is LoadState.Loading } }
    val searchResults by viewModel.searchResults.collectAsState()

    // Observe internet connection state dynamically
    val isInternetAvailable by viewModel.isInternetAvailable.collectAsState()

    // Show toast if no internet connection
    LaunchedEffect(isInternetAvailable) {
        if (!isInternetAvailable) {
            showToast(context)
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {

        // Search Bar
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
                if (searchQuery.value.isNotEmpty()) {
                    IconButton(onClick = { searchQuery.value = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Search")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing),
            onRefresh = {
                if (isInternetAvailable) {
                    lazyPhotos.refresh()
                    searchQuery.value = ""
                } else {
                    showToast(context)
                }
            }
        ) {
            // Show "No internet" message if there's no internet
            if (!isInternetAvailable) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Please check your internet connection",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp
                    )
                }
            } else {
                // Show loading indicator during initial load
                if (lazyPhotos.loadState.refresh is LoadState.Loading && lazyPhotos.itemCount == 0) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                } else {
                    LazyColumn {
                        // Show search results if search query is not empty
                        if (searchQuery.value.isNotEmpty()) {
                            if (searchResults.isEmpty()) {
                                // Show "No items found" message if search results are empty
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No items found",
                                            color = Color.Gray,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            } else {
                                items(searchResults) { photo ->
                                    photo?.let {
                                        PhotoItem(
                                            photo = it.copy(isFavorite = viewModel.isFavorite(it.id)),
                                            onFavoriteClick = { viewModel.toggleFavorite(it) }
                                        )
                                    }
                                }
                            }
                        } else {
                            // Show paged photos if no search query
                            items(lazyPhotos.itemCount) { index ->
                                val photo = lazyPhotos[index]
                                photo?.let {
                                    PhotoItem(
                                        photo = it.copy(isFavorite = viewModel.isFavorite(it.id)),
                                        onFavoriteClick = { viewModel.toggleFavorite(it) }
                                    )
                                }
                            }

                            // Handle loading and error states
                            lazyPhotos.apply {
                                when {
                                    loadState.append is LoadState.Loading -> {
                                        item { CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally)) }
                                    }
                                    loadState.append is LoadState.Error -> {
                                        item { Text("Failed to load more items", Modifier.align(Alignment.CenterHorizontally)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoItem(photo: Photo, onFavoriteClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
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
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = photo.download_url,
                    contentDescription = "Photo by ${photo.author}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Author: ${photo.author}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No favorites found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
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

@Composable
fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}



