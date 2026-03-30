package com.example.nfctagemulator.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nfctagemulator.data.model.TagData
import com.example.nfctagemulator.data.repository.TagRepository
import com.example.nfctagemulator.nfc.emulator.TagEmulator
import com.example.nfctagemulator.nfc.reader.NfcReader
import com.example.nfctagemulator.ui.components.BottomNavigationBar
import com.example.nfctagemulator.ui.screen.CreateTagScreen
import com.example.nfctagemulator.ui.screen.SavedTagsScreen
import com.example.nfctagemulator.ui.screen.ScanScreen
import com.example.nfctagemulator.ui.theme.NfcTagEmulatorTheme

class MainActivity : ComponentActivity() {

    private lateinit var reader: NfcReader
    private lateinit var repository: TagRepository
    private lateinit var emulator: TagEmulator
    private var scannedTag = mutableStateOf<TagData?>(null)
    private var isEmulating = mutableStateOf(false)

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
        val navController = rememberNavController()

        var selectedTab by remember { mutableStateOf(0) }

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                BottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tabIndex ->
                        selectedTab = tabIndex
                        when (tabIndex) {
                            0 -> navController.navigate("saved") {
                                popUpTo("saved") { inclusive = true }
                            }
                            1 -> navController.navigate("scan") {
                                popUpTo("saved") { inclusive = false }
                            }
                            2 -> navController.navigate("create") {
                                popUpTo("saved") { inclusive = false }
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "saved",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                composable("saved") {
                    SavedTagsScreen(
                        repository = repository,
                        emulator = emulator,
                        isEmulating = isEmulating.value,
                        onEmulationStateChanged = { newState ->
                            isEmulating.value = newState
                            updateNfcState()
                        }
                    )
                }

                composable("scan") {
                    ScanScreen(
                        repository = repository,
                        scannedTag = scannedTag.value,
                        onNavigateToCreate = {
                            navController.navigate("create")
                        }
                    )
                }

                composable("create") {
                    CreateTagScreen(
                        repository = repository,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onTagCreated = {
                            navController.popBackStack()
                            Toast.makeText(context, "Tag created successfully", Toast.LENGTH_SHORT).show()
                        }
                    )
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