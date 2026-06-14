package com.example.nfctagemulator.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.example.nfctagemulator.nfc.emulator.TagHostApduService
import com.example.nfctagemulator.nfc.reader.NfcReader
import com.example.nfctagemulator.ui.components.BottomNavigationBar
import com.example.nfctagemulator.ui.onboarding.OnboardingScreen
import com.example.nfctagemulator.ui.onboarding.OnboardingViewModel
import com.example.nfctagemulator.ui.screen.CreateTagScreen
import com.example.nfctagemulator.ui.screen.SavedTagsScreen
import com.example.nfctagemulator.ui.screen.ScanScreen
import com.example.nfctagemulator.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var reader: NfcReader
    private lateinit var repository: TagRepository
    private lateinit var emulator: TagEmulator
    private var scannedTag = mutableStateOf<TagData?>(null)
    private var isEmulating = mutableStateOf(false)
    private var isNfcEnabled = mutableStateOf(false)
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

        updateNfcState()
        isEmulating.value = emulator.isEmulating()

        setContent {
            NfcTagEmulatorTheme {
                AppNavigation()
            }
        }
    }

    private fun updateNfcState() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        isNfcEnabled.value = nfcAdapter?.isEnabled == true

        // If NFC is disabled and emulation is active, stop emulation
        if (!isNfcEnabled.value && isEmulating.value) {
            emulator.setEmulatingTag(null)
            isEmulating.value = false
            Toast.makeText(this, "NFC is disabled. Emulation stopped.", Toast.LENGTH_SHORT).show()
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
                    updateNfcState()
                    checkAndFixNfcSettings()
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

    private fun checkAndFixNfcSettings() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter != null && nfcAdapter.isEnabled) {
            val cardEmulation = CardEmulation.getInstance(nfcAdapter)
            val component = ComponentName(this, TagHostApduService::class.java)

            if (cardEmulation?.isDefaultServiceForCategory(component, CardEmulation.CATEGORY_OTHER) != true) {
                showDisableBuiltInTagDialog()
            }
        }
    }

    private fun showDisableBuiltInTagDialog() {
        runOnUiThread {
            Toast.makeText(
                this,
                "Open NFC settings → Tap & pay → Select this app as default",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openNfcSettings() {
        try {
            val intent = Intent(Settings.ACTION_NFC_PAYMENT_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                this,
                "Select this app as default payment app",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            try {
                val intent = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        Intent(Settings.Panel.ACTION_NFC)
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                        Intent(Settings.ACTION_NFC_SETTINGS)
                    }
                    else -> {
                        Intent(Settings.ACTION_WIRELESS_SETTINGS)
                    }
                }
                startActivity(intent)
                Toast.makeText(
                    this,
                    "Go to: Tap & pay → Select this app",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e2: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
                Toast.makeText(
                    this,
                    "Go to: Connected devices → Connection preferences → Tap & pay → Select this app",
                    Toast.LENGTH_LONG
                ).show()
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
        val isNfcEnabledLocal = nfcAdapter?.isEnabled == true

        val isDefaultApp = remember {
            if (nfcAdapter != null) {
                val cardEmulation = CardEmulation.getInstance(nfcAdapter)
                val component = ComponentName(context, TagHostApduService::class.java)
                cardEmulation?.isDefaultServiceForCategory(component, CardEmulation.CATEGORY_OTHER) == true
            } else false
        }

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
                    Text(text = "⚠️", fontSize = 48.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Tap & Pay Conflict",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isNfcEnabledLocal) NeonGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                    ) {
                        Text(
                            text = if (isNfcEnabledLocal) "✓ NFC is ENABLED" else "✗ NFC is DISABLED",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = if (isNfcEnabledLocal) NeonGreen else Color.White.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDefaultApp) NeonGreen.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isDefaultApp) "✓ This app is DEFAULT" else "⚠ This app is NOT default",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = if (isDefaultApp) NeonGreen else NeonCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "💡 Tip: After setting as default, restart NFC if conflict persists",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )

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

        // Update NFC state periodically
        LaunchedEffect(Unit) {
            while (true) {
                updateNfcState()
                delay(1000)
            }
        }

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
                            isNfcEnabled = isNfcEnabled.value,
                            onEmulationStateChanged = { newState ->
                                if (newState && !isNfcEnabled.value) {
                                    Toast.makeText(context, "Cannot start emulation: NFC is disabled", Toast.LENGTH_SHORT).show()
                                } else {
                                    isEmulating.value = newState
                                    updateNfcState()
                                }
                            },
                            refreshTrigger = refreshSavedTags.value
                        )
                    }
                    1 -> {
                        ScanScreen(
                            repository = repository,
                            scannedTag = scannedTag.value,
                            isNfcEnabled = isNfcEnabled.value,
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
        updateNfcState()
        if (!isEmulating.value && isNfcEnabled.value) {
            reader.enable(this)
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isEmulating.value) {
            reader.disable(this)
        }
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

        if (!isNfcEnabled.value) {
            Toast.makeText(
                this,
                "NFC is disabled. Please enable NFC first.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

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
}
