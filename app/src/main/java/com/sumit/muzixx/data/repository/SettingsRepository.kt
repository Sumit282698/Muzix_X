package com.sumit.muzixx.data.repository

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sumit.muzixx.data.manager.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsRepository(
    context: Context,
    private val externalScope: CoroutineScope
) {
    private val settingsManager = SettingsManager(context.applicationContext)

    //STATES
    var streamWifiOnly by mutableStateOf(false)
        private set
    var downloadWifiOnly by mutableStateOf(true)
        private set
    var showLyrics by mutableStateOf(true)
        private set
    var normalizeAudio by mutableStateOf(true)
        private set
    var skipSilence by mutableStateOf(false)
        private set
    var checkUpdatesOnStart by mutableStateOf(true)
        private set
    var audioQuality by mutableStateOf("320kbps")
        private set
    var userName by mutableStateOf("User")
        private set
    var appTheme by mutableStateOf("Neon Red")
        private set

    init {
        // Collect individual preferences asynchronously on initialization
        externalScope.launch(Dispatchers.IO) {
            launch {
                settingsManager.streamWifiOnlyFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { streamWifiOnly = value }
                }
            }
            launch {
                settingsManager.downloadWifiOnlyFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { downloadWifiOnly = value }
                }
            }
            launch {
                settingsManager.showLyricsFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { showLyrics = value }
                }
            }
            launch {
                settingsManager.normalizeAudioFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { normalizeAudio = value }
                }
            }
            launch {
                settingsManager.skipSilenceFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { skipSilence = value }
                }
            }
            launch {
                settingsManager.checkUpdatesOnStartFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { checkUpdatesOnStart = value }
                }
            }
            launch {
                settingsManager.audioQualityFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { audioQuality = value }
                }
            }
            launch {
                settingsManager.userNameFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { userName = value }
                }
            }
            launch {
                settingsManager.appThemeFlow.collectLatest { value ->
                    withContext(Dispatchers.Main) { appTheme = value }
                }
            }
        }
    }

    //THREAD-SAFE UPDATER
    fun updateStreamWifiOnly(value: Boolean) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveBooleanSetting(SettingsManager.STREAM_WIFI_ONLY, value) }
    }

    fun updateDownloadWifiOnly(value: Boolean) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveBooleanSetting(SettingsManager.DOWNLOAD_WIFI_ONLY, value) }
    }

    fun updateShowLyrics(value: Boolean) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveBooleanSetting(SettingsManager.SHOW_LYRICS, value) }
    }

    fun updateNormalizeAudio(value: Boolean) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveBooleanSetting(SettingsManager.NORMALIZE_AUDIO, value) }
    }

    fun updateSkipSilence(value: Boolean) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveBooleanSetting(SettingsManager.SKIP_SILENCE, value) }
    }

    fun updateCheckUpdatesOnStart(value: Boolean) {
        externalScope.launch(Dispatchers.IO) { settingsManager.saveBooleanSetting(SettingsManager.CHECK_UPDATES_ON_START, value) }
    }

    fun updateAudioQuality(value: String) {
        externalScope.launch(Dispatchers.IO) {
            settingsManager.saveStringSetting(SettingsManager.AUDIO_QUALITY, value)
        }
    }

    fun updateUserName(value: String) {
        externalScope.launch(Dispatchers.IO) {
            settingsManager.saveStringSetting(SettingsManager.USER_NAME, value)
        }
    }

    fun updateAppTheme(value: String) {
        externalScope.launch(Dispatchers.IO) {
            settingsManager.saveStringSetting(SettingsManager.APP_THEME, value)
        }
    }
}