package com.simpad.kneeboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simpad.kneeboard.ui.components.ConnectionDialog
import com.simpad.kneeboard.ui.components.InkingToolbar
import com.simpad.kneeboard.ui.components.TabNavigationBar
import com.simpad.kneeboard.ui.components.TopBar
import com.simpad.kneeboard.ui.inking.InkingCanvas
import com.simpad.kneeboard.ui.pages.DocumentPageViewer
import com.simpad.kneeboard.ui.pages.QuickNotesPage
import com.simpad.kneeboard.ui.pages.TelemetryHudPage
import com.simpad.kneeboard.ui.pages.WebViewPage
import com.simpad.kneeboard.ui.theme.LocalSimPadColors
import com.simpad.kneeboard.ui.theme.SimPadTheme
import com.simpad.kneeboard.viewmodel.InkingViewModel
import com.simpad.kneeboard.viewmodel.KneeboardViewModel

@Composable
fun MainKneeboardScreen(
    kneeboardViewModel: KneeboardViewModel = viewModel(),
    inkingViewModel: InkingViewModel = viewModel()
) {
    val uiState by kneeboardViewModel.uiState.collectAsState()
    val inkingState by inkingViewModel.uiState.collectAsState()

    // Determine current page ID for vector note persistence
    val currentTab = uiState.selectedTab
    val currentPage = currentTab?.pages?.getOrNull(uiState.currentPageIndex)
    val pageId = when {
        currentTab == null -> "empty_page"
        currentTab.isDynamic && currentTab.dynamicType == "Radio" -> "dynamic_telemetry"
        currentTab.isDynamic && currentTab.dynamicType == "QuickNotes" -> "quick_notes_${uiState.currentPageIndex}"
        currentTab.category == 5 || currentTab.dynamicType == "WebView" -> "webview_${currentTab.id}"
        currentPage != null -> "doc_${currentPage.id}_p${currentPage.pdfPageNumber}"
        else -> "tab_${currentTab.id}_p${uiState.currentPageIndex}"
    }

    // Load strokes when page changes
    LaunchedEffect(pageId) {
        inkingViewModel.loadPageStrokes(pageId)
    }

    SimPadTheme(mode = uiState.lightingMode) {
        val colors = LocalSimPadColors.current

        Scaffold(
            topBar = {
                Column {
                    TopBar(
                        activeSimulator = uiState.telemetry.simulator,
                        activeAircraft = uiState.telemetry.aircraft,
                        connectionStatus = uiState.connectionStatus,
                        latencyMs = uiState.latencyMs,
                        currentLightingMode = uiState.lightingMode,
                        onLightingModeChanged = { kneeboardViewModel.setLightingMode(it) },
                        onOpenConnectionSettings = { kneeboardViewModel.openConnectionDialog(true) }
                    )

                    if (uiState.tabs.isNotEmpty()) {
                        TabNavigationBar(
                            tabs = uiState.tabs,
                            selectedTabId = uiState.selectedTab?.id,
                            onTabSelected = { kneeboardViewModel.selectTab(it) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(colors.background)
            ) {
                // 1. BASE DOCUMENT / DYNAMIC HUD LAYER
                if (currentTab != null) {
                    when {
                        currentTab.isDynamic && currentTab.dynamicType == "Radio" -> {
                            TelemetryHudPage(telemetry = uiState.telemetry)
                        }
                        currentTab.isDynamic && currentTab.dynamicType == "QuickNotes" -> {
                            QuickNotesPage(pageTitle = currentPage?.title ?: "Scratchpad")
                        }
                        (currentTab.category == 5 || currentTab.dynamicType == "WebView") && currentTab.webViewUrl != null -> {
                            WebViewPage(url = currentTab.webViewUrl)
                        }
                        currentPage != null -> {
                            val imageUrl = kneeboardViewModel.getPageImageUrl(currentPage)
                            DocumentPageViewer(
                                page = currentPage,
                                imageUrl = imageUrl,
                                totalPagesInTab = currentTab.pages.size,
                                currentPageIndex = uiState.currentPageIndex,
                                onPreviousPage = { kneeboardViewModel.previousPage() },
                                onNextPage = { kneeboardViewModel.nextPage() }
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No pages available in this kneeboard tab.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Connect to SimPad Server to load kneeboard tabs.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textMuted
                        )
                    }
                }

                // 2. TRANSPARENT ACTIVE INKING LAYER (WITH PALM REJECTION)
                InkingCanvas(
                    modifier = Modifier.fillMaxSize(),
                    strokes = inkingState.strokes,
                    activeTool = inkingState.activeTool,
                    activeColorArgb = inkingState.activeColorArgb,
                    activeStrokeWidth = inkingState.activeStrokeWidth,
                    onStrokeAdded = { stroke -> inkingViewModel.addStroke(stroke) },
                    onStrokesChanged = { updated -> inkingViewModel.updateStrokes(updated) },
                    onPageSwipe = { isNext ->
                        if (isNext) kneeboardViewModel.nextPage() else kneeboardViewModel.previousPage()
                    }
                )

                // 3. FLOATING INKING TOOLBAR
                InkingToolbar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp),
                    activeTool = inkingState.activeTool,
                    activeColorArgb = inkingState.activeColorArgb,
                    activeStrokeWidth = inkingState.activeStrokeWidth,
                    canUndo = inkingState.canUndo,
                    canRedo = inkingState.canRedo,
                    onToolSelected = { inkingViewModel.setTool(it) },
                    onColorSelected = { inkingViewModel.setColor(it) },
                    onStrokeWidthSelected = { inkingViewModel.setStrokeWidth(it) },
                    onUndoClicked = { inkingViewModel.undo() },
                    onRedoClicked = { inkingViewModel.redo() },
                    onClearClicked = { kneeboardViewModel.openClearNotesDialog(true) }
                )
            }

            // Connection Dialog
            if (uiState.isConnectionDialogOpen) {
                ConnectionDialog(
                    initialHost = uiState.serverHost,
                    initialPort = uiState.serverPort,
                    connectionStatus = uiState.connectionStatus,
                    onDismiss = { kneeboardViewModel.openConnectionDialog(false) },
                    onConnect = { host, port -> kneeboardViewModel.connectServer(host, port) },
                    onDisconnect = { kneeboardViewModel.disconnectServer() }
                )
            }

            // Clear Notes Confirmation Dialog
            if (uiState.isClearNotesDialogOpen) {
                AlertDialog(
                    onDismissRequest = { kneeboardViewModel.openClearNotesDialog(false) },
                    title = {
                        Text(
                            text = "Clear Handwritten Notes",
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.textPrimary
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to clear all handwritten notes on this page?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                inkingViewModel.clearCurrentPage()
                                kneeboardViewModel.openClearNotesDialog(false)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                        ) {
                            Text("Clear Notes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { kneeboardViewModel.openClearNotesDialog(false) }) {
                            Text("Cancel", color = colors.textSecondary)
                        }
                    },
                    containerColor = colors.surfaceElevated
                )
            }
        }
    }
}
