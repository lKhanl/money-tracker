package io.github.lkhanl.moneytracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.darwin.NSObject
import platform.posix.memcpy

@Composable
actual fun rememberPdfPicker(onPicked: (ByteArray) -> Unit): () -> Unit {
    val controller = LocalUIViewController.current
    val callback = rememberUpdatedState(onPicked)
    // Seçici açıkken temsilci bırakılmasın diye burada tutulur; UIKit ona zayıf referans verir.
    val delegate = remember { PdfPickerDelegate { callback.value(it) } }
    return {
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypePDF), asCopy = true)
        picker.delegate = delegate
        picker.allowsMultipleSelection = false
        controller.presentViewController(picker, animated = true, completion = null)
    }
}

private class PdfPickerDelegate(private val onPicked: (ByteArray) -> Unit) :
    NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
        val data = NSData.dataWithContentsOfURL(url) ?: return
        onPicked(data.toByteArray())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isNotEmpty()) result.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return result
}
