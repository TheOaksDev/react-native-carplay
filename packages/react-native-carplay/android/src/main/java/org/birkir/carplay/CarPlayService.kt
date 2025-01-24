package org.birkir.carplay

import android.content.Intent
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.validation.HostValidator
import com.facebook.react.ReactApplication
import com.facebook.react.ReactInstanceManager

class CarPlayService : CarAppService() {
    private lateinit var reactInstanceManager: ReactInstanceManager

    override fun onCreate() {
        super.onCreate()
        reactInstanceManager =
            (application as ReactApplication).reactNativeHost.reactInstanceManager
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CarPlayService destroyed")
        // Perform cleanup related to CarPlay disconnection
    }

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(sessionInfo: SessionInfo): Session {
        Log.d(TAG, "onCreateSession: sessionId = ${sessionInfo.sessionId}, display = ${sessionInfo.displayType}")
        //startCarPlayForegroundService()
        return CarPlaySession(reactInstanceManager)
    }

    private fun startCarPlayForegroundService() {
        Log.d(TAG, "startCarPlayForegroundService")
        val serviceIntent = Intent(this, CarPlayForegroundService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Log.d(TAG, "startCarPlayForegroundService: startForegroundService")
            startForegroundService(serviceIntent)
        } else {
            Log.d(TAG, "startCarPlayForegroundService: startService")
            startService(serviceIntent)
        }
    }

    companion object {
        var TAG = "CarPlayService"
    }
}