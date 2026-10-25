package org.openrun.app

import android.app.Application
import org.openrun.app.data.OpenRunDatabase
import org.openrun.app.data.RunRepository
import org.openrun.app.data.SettingsStore

/** Holds the few long-lived objects. No injection framework: there are only three of them. */
class OpenRunApp : Application() {
    val database by lazy { OpenRunDatabase.open(this) }
    val runs by lazy { RunRepository(database) }
    val settings by lazy { SettingsStore(this) }
}
