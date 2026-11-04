package io.github.lkhanl.moneytracker

import android.content.Context
import androidx.core.content.edit
import androidx.room.Room
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import io.github.lkhanl.moneytracker.data.AppDatabase
import io.github.lkhanl.moneytracker.data.PdfTextExtractor
import io.github.lkhanl.moneytracker.settings.KeyValueStore

fun AppGraph(context: Context, versionName: String, demoData: Boolean): AppGraph {
    val app = context.applicationContext
    val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
    return AppGraph(
        databaseBuilder = Room.databaseBuilder<AppDatabase>(app, app.getDatabasePath(AppDatabase.FILE_NAME).absolutePath),
        settingsStore = object : KeyValueStore {
            override fun getString(key: String): String? = prefs.getString(key, null)
            override fun putString(key: String, value: String) = prefs.edit { putString(key, value) }
        },
        pdfText = PdfTextExtractor { pdf ->
            PDFBoxResourceLoader.init(app)
            PDDocument.load(pdf).use { PDFTextStripper().getText(it) }
        },
        versionName = versionName,
        demoData = demoData,
    )
}
