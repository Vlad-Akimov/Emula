package com.example.nfctagemulator.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.repository.TagRepository
import com.example.nfctagemulator.nfc.emulator.TagEmulator
import com.example.nfctagemulator.nfc.reader.NfcReader
import com.example.nfctagemulator.ui.components.BottomNavigationBar
import com.example.nfctagemulator.ui.onboarding.OnboardingScreen
import com.example.nfctagemulator.ui.onboarding.OnboardingViewModel
import com.example.nfctagemulator.ui.screen.CreateTagScreen
import com.example.nfctagemulator.ui.screen.SavedTagsScreen
import com.example.nfctagemulator.ui.screen.ScanScreen
import com.example.nfctagemulator.ui.theme.NfcTagEmulatorTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var reader: NfcReader
    private lateinit var repository: TagRepository
    private lateinit var emulator: TagEmulator
    private var scannedTag = mutableStateOf<TagData?>(null)
    private var isEmulating = mutableStateOf(false)
    private var refreshSavedTags = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Make status bar transparent with proper insets handling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.apply {
                statusBarColor = android.graphics.Color.TRANSPARENT
                // For Android 10 and above, we can make status bar icons light
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setDecorFitsSystemWindows(false)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Use light status bar icons (white) because background is dark
                    decorView.systemUiVisibility = decorView.systemUiVisibility or
                            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
        }

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

        // Onboarding ViewModel
        val onboardingViewModel: OnboardingViewModel = viewModel(
            factory = viewModelFactory {
                initializer {
                    OnboardingViewModel(context)
                }
            }
        )

        val isFirstLaunch by onboardingViewModel.isFirstLaunch.collectAsState()

        if (isFirstLaunch) {
            // Show onboarding
            OnboardingScreen(
                onComplete = {
                    onboardingViewModel.markOnboardingCompleted()
                }
            )
        } else {
            // Main app
            MainAppContent()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainAppContent() {
        val context = LocalContext.current
        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { 3 }
        )
        val coroutineScope = rememberCoroutineScope()

        var selectedTab by remember { mutableStateOf(0) }

        LaunchedEffect(pagerState.currentPage) {
            selectedTab = pagerState.currentPage
            if (pagerState.currentPage == 0) {
                refreshSavedTags.value++
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(
                left = 0,
                top = 0,
                right = 0,
                bottom = 0
            ),
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

    override fun onDestroy() {
        super.onDestroy()
        // Stop emulation when app is closed
        if (emulator.isEmulating()) {
            emulator.setEmulatingTag(null)
            isEmulating.value = false
        }
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