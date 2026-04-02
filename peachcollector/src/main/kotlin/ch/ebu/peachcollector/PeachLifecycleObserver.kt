package ch.ebu.peachcollector

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Observes app lifecycle via ProcessLifecycleOwner.
 * Checks session inactivity on resume and flushes events on pause/stop.
 */
internal class PeachLifecycleObserver : DefaultLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        PeachCollector.checkInactivity()
    }

    override fun onPause(owner: LifecycleOwner) {
        updateLastActiveTimestamp()
        PeachCollector.flush()
    }

    override fun onStop(owner: LifecycleOwner) {
        updateLastActiveTimestamp()
        PeachCollector.flush()
    }

    private fun updateLastActiveTimestamp() {
        val collector = PeachCollector.shared ?: return
        val prefs: SharedPreferences = collector.applicationContext.getSharedPreferences(
            "peach_collector_prefs", Context.MODE_PRIVATE
        )
        prefs.edit()
            .putLong(PeachConstants.SESSION_LAST_ACTIVE_TIMESTAMP_SPREF_KEY, System.currentTimeMillis())
            .apply()
    }
}
