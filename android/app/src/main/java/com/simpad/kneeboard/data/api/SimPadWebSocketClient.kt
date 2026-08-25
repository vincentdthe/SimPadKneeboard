package com.simpad.kneeboard.data.api

import android.util.Log
import com.simpad.kneeboard.data.models.SimStateChangedEvent
import com.simpad.kneeboard.data.models.TelemetryData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

class SimPadWebSocketClient(
    private val scope: CoroutineScope
) {
    private val tag = "SimPadWebSocket"
    private var webSocket: WebSocket? = null
    private val jsonParser = Json { ignoreUnknownKeys = true }

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _telemetryFlow = MutableStateFlow(TelemetryData())
    val telemetryFlow: StateFlow<TelemetryData> = _telemetryFlow.asStateFlow()

    private val _simStateFlow = MutableSharedFlow<SimStateChangedEvent>(extraBufferCapacity = 10)
    val simStateFlow: SharedFlow<SimStateChangedEvent> = _simStateFlow.asSharedFlow()

    private val _tabsInvalidatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 5)
    val tabsInvalidatedFlow: SharedFlow<Unit> = _tabsInvalidatedFlow.asSharedFlow()

    private val _latencyMs = MutableStateFlow(0L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private var currentHost: String = "192.168.1.100"
    private var currentPort: Int = 8090
    private var isUserDisconnect: Boolean = false
    private var reconnectJob: Job? = null
    private var pingJob: Job? = null
    private var lastPingSendTime: Long = 0L

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    fun connect(host: String, port: Int = 8090) {
        currentHost = host
        currentPort = port
        isUserDisconnect = false
        reconnectJob?.cancel()

        _connectionStatus.value = ConnectionStatus.CONNECTING
        val wsUrl = "ws://$host:$port/ws/telemetry"
        Log.i(tag, "Connecting to WebSocket: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket?.cancel()
        webSocket = client.newWebSocket(request, createListener())
    }

    fun disconnect() {
        isUserDisconnect = true
        reconnectJob?.cancel()
        pingJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }

    private fun createListener() = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            Log.i(tag, "WebSocket Connected successfully")
            _connectionStatus.value = ConnectionStatus.CONNECTED
            startHeartbeat()
        }

        override fun onMessage(ws: WebSocket, text: String) {
            try {
                handleIncomingJson(text)
            } catch (e: Exception) {
                Log.w(tag, "Error parsing WebSocket frame: ${e.message}")
            }
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            Log.d(tag, "WebSocket Closing: $reason")
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            Log.i(tag, "WebSocket Closed: $reason")
            pingJob?.cancel()
            if (!isUserDisconnect) {
                scheduleReconnect()
            } else {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            Log.e(tag, "WebSocket Failure: ${t.message}")
            pingJob?.cancel()
            if (!isUserDisconnect) {
                scheduleReconnect()
            } else {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }
        }
    }

    private fun handleIncomingJson(text: String) {
        val root = jsonParser.parseToJsonElement(text).jsonObject
        val event = root["event"]?.jsonPrimitive?.content ?: return

        when (event) {
            "telemetry_frame" -> {
                val payload = root["payload"] ?: return
                val telemetry = jsonParser.decodeFromJsonElement<TelemetryData>(payload)
                _telemetryFlow.value = telemetry
            }
            "sim_state_changed" -> {
                val payload = root["payload"] ?: return
                val simState = jsonParser.decodeFromJsonElement<SimStateChangedEvent>(payload)
                scope.launch { _simStateFlow.emit(simState) }
            }
            "tabs_invalidated" -> {
                scope.launch { _tabsInvalidatedFlow.emit(Unit) }
            }
            "pong" -> {
                if (lastPingSendTime > 0) {
                    val rtt = System.currentTimeMillis() - lastPingSendTime
                    _latencyMs.value = rtt
                }
            }
        }
    }

    private fun startHeartbeat() {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive && _connectionStatus.value == ConnectionStatus.CONNECTED) {
                delay(3000)
                lastPingSendTime = System.currentTimeMillis()
                webSocket?.send("{\"event\":\"ping\",\"timestamp\":$lastPingSendTime}")
            }
        }
    }

    private fun scheduleReconnect() {
        if (isUserDisconnect) return
        _connectionStatus.value = ConnectionStatus.RECONNECTING
        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            var backoffMs = 2000L
            while (isActive && !isUserDisconnect && _connectionStatus.value != ConnectionStatus.CONNECTED) {
                delay(backoffMs)
                Log.i(tag, "Attempting reconnect to $currentHost:$currentPort...")
                connect(currentHost, currentPort)
                backoffMs = (backoffMs * 1.5).toLong().coerceAtMost(10000L)
            }
        }
    }
}
