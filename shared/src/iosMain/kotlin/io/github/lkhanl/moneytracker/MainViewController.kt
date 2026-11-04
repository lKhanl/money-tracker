package io.github.lkhanl.moneytracker

import androidx.compose.ui.window.ComposeUIViewController
import androidx.room.Room
import io.github.lkhanl.moneytracker.data.AppDatabase
import io.github.lkhanl.moneytracker.data.PdfTextExtractor
import io.github.lkhanl.moneytracker.settings.KeyValueStore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes
import platform.PDFKit.PDFDocument
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

private val graph: AppGraph by lazy {
    val defaults = NSUserDefaults.standardUserDefaults
    AppGraph(
        databaseBuilder = Room.databaseBuilder<AppDatabase>(name = documentsPath() + "/" + AppDatabase.FILE_NAME),
        settingsStore = object : KeyValueStore {
            override fun getString(key: String): String? = defaults.stringForKey(key)
            override fun putString(key: String, value: String) = defaults.setObject(value, key)
        },
        pdfText = PdfTextExtractor { pdf ->
            val document = PDFDocument(pdf.toNSData()) ?: error("PDF açılamadı")
            document.string ?: ""
        },
        versionName = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "",
        // Örnek veri yalnızca istenirse: xcrun simctl launch <cihaz> io.github.lkhanl.moneytracker -demoData
        // (ya da Xcode şemasında "Arguments Passed On Launch"). Gerçek telefonda sahte işlemler karışmasın.
        demoData = "-demoData" in NSProcessInfo.processInfo.arguments,
    )
}

/** Swift tarafının gösterdiği kök ekran. */
@Suppress("unused", "FunctionName")
fun MainViewController(): UIViewController = ComposeUIViewController {
    App(graph, onDarkThemeChanged = ::applyInterfaceStyle)
}

/** Tema sistemden bağımsız seçildiği için durum çubuğu yazılarının rengi pencerenin stiliyle eşlenir. */
private fun applyInterfaceStyle(dark: Boolean) {
    val style = if (dark) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
    UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .flatMap { it.windows.filterIsInstance<UIWindow>() }
        .forEach { it.overrideUserInterfaceStyle = style }
}

@OptIn(ExperimentalForeignApi::class)
private fun documentsPath(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(url?.path) { "Belgeler klasörü bulunamadı" }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = usePinned {
    NSData.dataWithBytes(it.addressOf(0), size.toULong())
}
