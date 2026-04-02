package ch.ebu.peachdemo.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import ch.ebu.peachcollector.PeachConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the demo app. Collects PeachCollector log broadcasts
 * and exposes them as StateFlow for the UI.
 */
class DemoViewModel(application: Application) : AndroidViewModel(application) {

    private val _logMessages = MutableStateFlow<List<String>>(emptyList())
    val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

    private val _lastPayload = MutableStateFlow("")
    val lastPayload: StateFlow<String> = _lastPayload.asStateFlow()

    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra(PeachConstants.PEACH_LOG_NOTIFICATION_MESSAGE) ?: return
            _logMessages.value = _logMessages.value + message

            val payload = intent.getStringExtra(PeachConstants.PEACH_LOG_NOTIFICATION_PAYLOAD)
            if (!payload.isNullOrEmpty()) {
                _lastPayload.value = payload
            }
        }
    }

    init {
        val filter = IntentFilter(PeachConstants.PEACH_LOG_NOTIFICATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(logReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            application.registerReceiver(logReceiver, filter)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(logReceiver)
        } catch (_: Exception) {
            // Already unregistered
        }
    }

    fun clearLogs() {
        _logMessages.value = emptyList()
        _lastPayload.value = ""
    }
}
