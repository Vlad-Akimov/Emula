package com.example.nfctagemulator.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.repository.TagRepository
import com.example.nfctagemulator.nfc.emulator.TagEmulator
import com.example.nfctagemulator.nfc.reader.NfcReader
import com.example.nfctagemulator.ui.components.BottomNavigationBar
import com.example.nfctagemulator.ui.screen.CreateTagScreen
import com.example.nfctagemulator.ui.screen.SavedTagsScreen
import com.example.nfctagemulator.ui.screen.ScanScreen
import com.example.nfctagemulator.ui.theme.NfcTagEmulatorTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var reader: NfcReader
    private lateinit var repository: TagRepository
    private lateinit var emulator: TagEmulator
    private var scannedTag = mutableStateOf<TagData?>(null)
    private var isEmulating = mutableStateOf(false)
    // Add a refresh trigger for SavedTagsScreen
    private var refreshSavedTags = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reader = NfcReader(this)
        repository = TagRepository(this)
        emulator = TagEmulator(this)
        isEmulating.value = emulator.isEmulating()

        setContent {
            NfcTagEmulatorTheme {
                AppNavigation()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppNavigation() {
        val context = LocalContext.current
        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { 3 }
        )
        val coroutineScope = rememberCoroutineScope()

        var selectedTab by remember { mutableStateOf(0) }

        // Sync tab with pager
        LaunchedEffect(pagerState.currentPage) {
            selectedTab = pagerState.currentPage
            // When returning to SavedTagsScreen (tab 0), trigger refresh
            if (pagerState.currentPage == 0) {
                refreshSavedTags.value++
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                BottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tabIndex ->
                        selectedTab = tabIndex
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(tabIndex)
                        }
                    }
                )
            }
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                pageSpacing = 0.dp,
                userScrollEnabled = true,
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> {
                        SavedTagsScreen(
                            repository = repository,
                            emulator = emulator,
                            isEmulating = isEmulating.value,
                            onEmulationStateChanged = { newState ->
                                isEmulating.value = newState
                                updateNfcState()
                            },
                            refreshTrigger = refreshSavedTags.value
                        )
                    }
                    1 -> {
                        ScanScreen(
                            repository = repository,
                            scannedTag = scannedTag.value,
                            onNavigateToCreate = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(2)
                                }
                            }
                        )
                    }
                    2 -> {
                        CreateTagScreen(
                            repository = repository,
                            onBackClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            },
                            onTagCreated = {
                                Toast.makeText(context, "Tag created successfully", Toast.LENGTH_SHORT).show()
                                // Trigger refresh when returning to saved tags
                                refreshSavedTags.value++
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isEmulating.value) {
            reader.enable(this)
        }
    }

    override fun onPause() {
        super.onPause()
        reader.disable(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (isEmulating.value) {
            Toast.makeText(
                this,
                "Emulation mode is active. Stop emulation to scan.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val tagData = reader.readTag(intent)
        tagData?.let {
            val existingTag = repository.getTagByUid(it.uid)

            if (existingTag != null) {
                Toast.makeText(
                    this,
                    "⚠️ Tag already saved as \"${existingTag.name}\"",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                repository.saveTag(it)
                scannedTag.value = it
                // Trigger refresh for saved tags screen
                refreshSavedTags.value++
                Toast.makeText(this, "✅ ${it.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateNfcState() {
        if (isEmulating.value) {
            reader.disable(this)
        } else {
            reader.enable(this)
        }
    }
}