package org.openrun.app

import android.app.Application
import org.openrun.app.data.OpenRunDatabase
import org.openrun.app.data.RunRepository
import org.openrun.app.data.SettingsStore

/** Holds the few long-lived objects. No injection framework: there are only three of them. */
class OpenRunApp : Application() {
    /** For work that must outlive a screen or a service (writing to Health Connect after a session ends). */
    val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    val database by lazy { OpenRunDatabase.open(this) }
    val runs by lazy { RunRepository(database) }
    val settings by lazy { SettingsStore(this) }
    val health by lazy { org.openrun.app.health.HealthConnectSync(this, runs) }
}
