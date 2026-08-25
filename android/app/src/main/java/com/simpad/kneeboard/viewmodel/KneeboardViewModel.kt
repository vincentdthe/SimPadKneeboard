package com.simpad.kneeboard.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simpad.kneeboard.data.api.ConnectionStatus
import com.simpad.kneeboard.data.api.SimPadWebSocketClient
import com.simpad.kneeboard.data.models.KneeboardPage
import com.simpad.kneeboard.data.models.KneeboardTab
import com.simpad.kneeboard.data.models.Profile
import com.simpad.kneeboard.data.models.ServerStatus
import com.simpad.kneeboard.data.models.TelemetryData
import com.simpad.kneeboard.data.repository.KneeboardRepository
import com.simpad.kneeboard.ui.theme.CockpitLightingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class KneeboardUiState(
    val serverHost: String = "192.168.1.100",
    val serverPort: Int = 8090,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val latencyMs: Long = 0L,
    val serverStatus: ServerStatus? = null,
    val telemetry: TelemetryData = TelemetryData(),
    val profiles: List<Profile> = emptyList(),
    val activeProfile: Profile? = null,
    val tabs: List<KneeboardTab> = emptyList(),
    val selectedTab: KneeboardTab? = null,
    val currentPageIndex: Int = 0,
    val lightingMode: CockpitLightingMode = CockpitLightingMode.TACTICAL_DARK,
    val isConnectionDialogOpen: Boolean = false,
    val isClearNotesDialogOpen: Boolean = false,
    val errorMessage: String? = null
)

class KneeboardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("simpad_prefs", Context.MODE_PRIVATE)
    private val repository = KneeboardRepository(application)
    private val wsClient = SimPadWebSocketClient(viewModelScope)

    private val _uiState = MutableStateFlow(
        KneeboardUiState(
            serverHost = prefs.getString("server_host", "192.168.1.100") ?: "192.168.1.100",
            serverPort = prefs.getInt("server_port", 8090)
        )
    )
    val uiState: StateFlow<KneeboardUiState> = _uiState.asStateFlow()

    init {
        // Collect WebSocket telemetry and connection state
        viewModelScope.launch {
            wsClient.telemetryFlow.collect { telemetry ->
                _uiState.value = _uiState.value.copy(telemetry = telemetry)
            }
        }
        viewModelScope.launch {
            wsClient.connectionStatus.collect { status ->
                _uiState.value = _uiState.value.copy(connectionStatus = status)
                if (status == ConnectionStatus.CONNECTED) {
                    refreshTabsAndStatus()
                }
            }
        }
        viewModelScope.launch {
            wsClient.latencyMs.collect { latency ->
                _uiState.value = _uiState.value.copy(latencyMs = latency)
            }
        }
        viewModelScope.launch {
            wsClient.simStateFlow.collect { simEvent ->
                // Automatically refresh kneeboard tabs when simulator starts/switches aircraft
                refreshTabsAndStatus()
            }
        }
        viewModelScope.launch {
            wsClient.tabsInvalidatedFlow.collect {
                refreshTabsAndStatus()
            }
        }

        // Auto-connect on startup if configured
        val autoConnect = prefs.getBoolean("auto_connect", true)
        if (autoConnect) {
            connectServer(_uiState.value.serverHost, _uiState.value.serverPort)
        }
    }

    fun connectServer(host: String, port: Int = 8090) {
        prefs.edit()
            .putString("server_host", host)
            .putInt("server_port", port)
            .apply()

        _uiState.value = _uiState.value.copy(
            serverHost = host,
            serverPort = port
        )

        repository.configureServer(host, port)
        wsClient.connect(host, port)
    }

    fun disconnectServer() {
        wsClient.disconnect()
    }

    fun refreshTabsAndStatus() {
        viewModelScope.launch {
            val statusRes = repository.getServerStatus()
            if (statusRes.isSuccess) {
                _uiState.value = _uiState.value.copy(serverStatus = statusRes.getOrNull())
            }

            val tabsRes = repository.getTabs()
            if (tabsRes.isSuccess) {
                val tabs = tabsRes.getOrDefault(emptyList())
                val currentTab = _uiState.value.selectedTab
                val matchedTab = tabs.find { it.id == currentTab?.id } ?: tabs.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    tabs = tabs,
                    selectedTab = matchedTab,
                    currentPageIndex = 0
                )
            }
        }
    }

    fun selectTab(tab: KneeboardTab) {
        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            currentPageIndex = 0
        )
    }

    fun nextPage() {
        val tab = _uiState.value.selectedTab ?: return
        val current = _uiState.value.currentPageIndex
        if (current < tab.pages.size - 1) {
            _uiState.value = _uiState.value.copy(currentPageIndex = current + 1)
        }
    }

    fun previousPage() {
        val current = _uiState.value.currentPageIndex
        if (current > 0) {
            _uiState.value = _uiState.value.copy(currentPageIndex = current - 1)
        }
    }

    fun setLightingMode(mode: CockpitLightingMode) {
        _uiState.value = _uiState.value.copy(lightingMode = mode)
    }

    fun openConnectionDialog(open: Boolean) {
        _uiState.value = _uiState.value.copy(isConnectionDialogOpen = open)
    }

    fun openClearNotesDialog(open: Boolean) {
        _uiState.value = _uiState.value.copy(isClearNotesDialogOpen = open)
    }

    fun getPageImageUrl(page: KneeboardPage): String {
        return repository.getPageContentUrl(page)
    }

    override fun onCleared() {
        super.onCleared()
        wsClient.disconnect()
    }
}
