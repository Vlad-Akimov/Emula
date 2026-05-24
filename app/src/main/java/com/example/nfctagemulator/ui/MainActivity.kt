package com.example.nfctagemulator.ui

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.nfctagemulator.ui.theme.*
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

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.apply {
                statusBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setDecorFitsSystemWindows(false)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

        val onboardingViewModel: OnboardingViewModel = viewModel(
            factory = viewModelFactory {
                initializer {
                    OnboardingViewModel(context)
                }
            }
        )

        val isFirstLaunch by onboardingViewModel.isFirstLaunch.collectAsState()
        val shouldShowNfcSetup by onboardingViewModel.shouldShowNfcSetup.collectAsState()

        if (isFirstLaunch) {
            OnboardingScreen(
                onComplete = {
                    onboardingViewModel.markOnboardingCompleted()
                }
            )
        } else {
            MainAppContent()

            if (shouldShowNfcSetup) {
                NfcSetupDialog(
                    onDismiss = {
                        onboardingViewModel.markNfcSetupShown()
                    },
                    onSetupComplete = {
                        onboardingViewModel.markNfcSetupShown()
                        openNfcSettings()
                    }
                )
            }
        }
    }

    private fun openNfcSettings() {
        try {
            // Пробуем открыть напрямую настройки NFC
            val intent = when {
                // Android 10+
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    Intent(Settings.Panel.ACTION_NFC)
                }
                // Android 6.0 - 9.0
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    Intent(Settings.ACTION_NFC_SETTINGS)
                }
                // Android 4.4 - 5.1
                else -> {
                    Intent(Settings.ACTION_WIRELESS_SETTINGS)
                }
            }

            startActivity(intent)

            // Если успешно открылось, показываем короткий Toast
            Toast.makeText(this, "Enable NFC and set as default app", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // Если не получилось открыть NFC напрямую, пробуем альтернативные варианты
            try {
                // Пробуем другой вариант
                startActivity(Intent("android.settings.NFC_SETTINGS"))
            } catch (e2: Exception) {
                try {
                    // Пробуем беспроводные настройки
                    startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                    Toast.makeText(this, "Look for NFC in wireless settings", Toast.LENGTH_LONG).show()
                } catch (e3: Exception) {
                    // Самый последний вариант - главные настройки
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                    Toast.makeText(this, "Go to: Connected devices → Connection preferences → NFC", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Composable
    fun NfcSetupDialog(
        onDismiss: () -> Unit,
        onSetupComplete: () -> Unit
    ) {
        val context = LocalContext.current
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        val isNfcEnabled = nfcAdapter?.isEnabled == true

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "📡", fontSize = 40.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "NFC Setup Required",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Instruction text
                    Text(
                        text = "To use this app, you need to:\n\n" +
                                "1️⃣ Enable NFC on your device\n" +
                                "2️⃣ Set this app as default for Tap & pay\n\n" +
                                "👇 Tap SET UP to open Settings",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // NFC status
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isNfcEnabled) NeonGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                    ) {
                        Text(
                            text = if (isNfcEnabled) "✓ NFC is ENABLED" else "✗ NFC is DISABLED",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = if (isNfcEnabled) NeonGreen else Color.White.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("LATER", fontSize = 13.sp)
                        }

                        Button(
                            onClick = onSetupComplete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("SET UP", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
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