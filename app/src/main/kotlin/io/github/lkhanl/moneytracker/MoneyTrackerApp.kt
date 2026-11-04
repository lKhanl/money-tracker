package io.github.lkhanl.moneytracker

import android.app.Application

class MoneyTrackerApp : Application() {
    // Örnek veri yalnızca -PdemoData=true ile derlenince eklenir.
    val graph: AppGraph by lazy { AppGraph(this, BuildConfig.VERSION_NAME, BuildConfig.DEMO_DATA) }

    override fun onCreate() {
        super.onCreate()
        graph
    }
}
