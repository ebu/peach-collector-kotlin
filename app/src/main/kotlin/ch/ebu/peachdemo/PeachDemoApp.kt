package ch.ebu.peachdemo

import android.app.Application
import ch.ebu.peachcollector.PeachCollector

/**
 * Application class that initializes PeachCollector on app start.
 */
class PeachDemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PeachCollector.isUnitTesting = true
        PeachCollector.shouldCollectAnonymousEvents = true
        PeachCollector.init(this)
    }
}
