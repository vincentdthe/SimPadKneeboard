package com.simpad.kneeboard.data.models

import kotlinx.serialization.Serializable

@Serializable
enum class TabCategory {
    GLOBAL,
    SIMULATOR,
    MODULE,
    CUSTOM,
    DYNAMIC,
    WEBVIEW
}

@Serializable
enum class PageFileType {
    PDF,
    IMAGE_PNG,
    IMAGE_JPG,
    DYNAMIC_VIEW,
    WEBVIEW
}

@Serializable
data class KneeboardTab(
    val id: String,
    val name: String,
    val category: Int = 0,
    val sourcePath: String? = null,
    val webViewUrl: String? = null,
    val order: Int = 0,
    val pageCount: Int = 0,
    val pages: List<KneeboardPage> = emptyList(),
    val isDynamic: Boolean = false,
    val dynamicType: String? = null
)

@Serializable
data class KneeboardPage(
    val id: String,
    val tabId: String,
    val pageIndex: Int,
    val title: String,
    val fileType: Int,
    val filePath: String,
    val pdfPageNumber: Int = 1,
    val totalPdfPages: Int = 1,
    val fileSizeBytes: Long = 0L,
    val lastModifiedUtc: String = "",
    val contentUrl: String
)

@Serializable
data class Profile(
    val id: String,
    val name: String,
    val simulator: String = "All",
    val aircraftModule: String? = null,
    val isActive: Boolean = false,
    val folderSources: List<ProfileFolderSource> = emptyList(),
    val webViewTabs: List<ProfileWebViewTab> = emptyList()
)

@Serializable
data class ProfileFolderSource(
    val id: String,
    val tabName: String,
    val path: String,
    val recursive: Boolean = false,
    val priority: Int = 10,
    val enabled: Boolean = true
)

@Serializable
data class ProfileWebViewTab(
    val id: String,
    val tabName: String,
    val url: String,
    val priority: Int = 50,
    val enabled: Boolean = true
)
