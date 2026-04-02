package ch.ebu.peachcollector

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Retrieves Google Play Services advertising ID via IPC.
 * Translated from Java, preserving the same IPC protocol.
 */
object AdvertisingIdClient {

    data class AdInfo(
        val id: String?,
        val isLimitAdTrackingEnabled: Boolean
    )

    /**
     * Fetches the advertising ID info from Google Play Services.
     * Must be called from a coroutine (runs on IO dispatcher).
     */
    suspend fun getAdvertisingIdInfo(context: Context): AdInfo = withContext(Dispatchers.IO) {
        val connection = AdvertisingConnection()
        val intent = Intent("com.google.android.gms.ads.identifier.service.START").apply {
            setPackage("com.google.android.gms")
        }

        if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            throw Exception("Cannot bind to Google Play Services advertising ID service")
        }

        try {
            val adInterface = AdvertisingInterface(connection.binder)
            AdInfo(
                id = adInterface.id,
                isLimitAdTrackingEnabled = adInterface.isLimitAdTrackingEnabled(true)
            )
        } finally {
            context.unbindService(connection)
        }
    }

    private class AdvertisingConnection : ServiceConnection {
        private val queue = LinkedBlockingQueue<IBinder>(1)

        val binder: IBinder
            get() = queue.poll(10, TimeUnit.SECONDS)
                ?: throw Exception("Timed out waiting for Google Play Services advertising ID service")

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            service?.let { queue.put(it) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    private class AdvertisingInterface(private val binder: IBinder) : IInterface {

        override fun asBinder(): IBinder = binder

        val id: String?
            get() {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService")
                    binder.transact(1, data, reply, 0)
                    reply.readException()
                    reply.readString()
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }

        fun isLimitAdTrackingEnabled(includeUnderAgeOfConsent: Boolean): Boolean {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            return try {
                data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService")
                data.writeInt(if (includeUnderAgeOfConsent) 1 else 0)
                binder.transact(2, data, reply, 0)
                reply.readException()
                reply.readInt() != 0
            } finally {
                reply.recycle()
                data.recycle()
            }
        }
    }
}
