package com.example.nfctagemulator.ui.onboarding

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(context: Context) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val onboardingKey = "has_seen_onboarding"
    private val nfcSetupShownKey = "nfc_setup_shown"

    private val _isFirstLaunch = MutableStateFlow(false)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch

    private val _shouldShowNfcSetup = MutableStateFlow(false)
    val shouldShowNfcSetup: StateFlow<Boolean> = _shouldShowNfcSetup

    init {
        checkFirstLaunch()
    }

    private fun checkFirstLaunch() {
        val hasSeen = prefs.getBoolean(onboardingKey, false)
        _isFirstLaunch.value = !hasSeen

        // Check if we already showed NFC setup dialog
        val nfcSetupShown = prefs.getBoolean(nfcSetupShownKey, false)
        _shouldShowNfcSetup.value = !hasSeen && !nfcSetupShown
    }

    fun markOnboardingCompleted() {
        viewModelScope.launch {
            prefs.edit().putBoolean(onboardingKey, true).apply()
            _isFirstLaunch.value = false
        }
    }

    fun markNfcSetupShown() {
        viewModelScope.launch {
            prefs.edit().putBoolean(nfcSetupShownKey, true).apply()
            _shouldShowNfcSetup.value = false
        }
    }
}