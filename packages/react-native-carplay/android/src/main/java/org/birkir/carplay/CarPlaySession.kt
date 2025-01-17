package org.birkir.carplay

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.facebook.react.ReactInstanceManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.appregistry.AppRegistry
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import com.facebook.react.modules.core.TimingModule
import org.birkir.carplay.screens.CarScreen

class CarPlaySession(private val reactInstanceManager: ReactInstanceManager) :
        Session(), DefaultLifecycleObserver {
  private lateinit var screen: CarScreen

  @SuppressLint("UnspecifiedRegisterReceiverFlag")
  override fun onCreateScreen(intent: Intent): Screen {
    Log.d(TAG, "On create screen " + intent.action + " - " + intent.dataString)
    val lifecycle = lifecycle
    lifecycle.addObserver(this)
    screen = CarScreen(carContext)

    // Handle reload events
    carContext.registerReceiver(
            object : BroadcastReceiver() {
              override fun onReceive(context: Context, intent: Intent) {
                if ("org.birkir.carplay.APP_RELOAD" == intent.action) {
                  Log.d(TAG, "Reloading app")
                  invokeStartTask(reactInstanceManager.currentReactContext!!)
                }
              }
            },
            IntentFilter("org.birkir.carplay.APP_RELOAD")
    )

    // Run JS
    runJsApplication()
    return screen
  }

  private fun runJsApplication() {
    val reactContext = reactInstanceManager.currentReactContext
    if (reactContext == null) {
      reactInstanceManager.addReactInstanceEventListener(
              object : ReactInstanceManager.ReactInstanceEventListener {
                override fun onReactContextInitialized(reactContext: ReactContext) {
                  Log.d(TAG, "React context initialized")
                  invokeStartTask(reactContext)
                  reactInstanceManager.removeReactInstanceEventListener(this)
                }
              }
      )
      reactInstanceManager.createReactContextInBackground()
    } else {
      Log.d(TAG, "Using existing React context")
      invokeStartTask(reactContext)
    }
  }

  private fun invokeStartTask(reactContext: ReactContext) {
    try {
      val catalystInstance = reactContext.catalystInstance
      val jsAppModuleName = "AndroidAuto"
      val appParams = WritableNativeMap()
      appParams.putDouble("rootTag", 1.0)
      val appProperties = Bundle.EMPTY
      if (appProperties != null) {
        appParams.putMap("initialProps", Arguments.fromBundle(appProperties))
      }

      catalystInstance
              .getJSModule(AppRegistry::class.java)
              .runApplication(jsAppModuleName, appParams)

      val timingModule = reactContext.getNativeModule(TimingModule::class.java)
      val carModule =
              reactInstanceManager.currentReactContext?.getNativeModule(CarPlayModule::class.java)
      carModule!!.setCarContext(carContext, screen)
      timingModule!!.onHostResume()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onDestroy(owner: LifecycleOwner) {
    Log.i(TAG, "onDestroy")
    // Unregister any receivers or listeners
    emitDidDisconnectEvent()
  }

  override fun onNewIntent(intent: Intent) {
    // handle intents
    Log.d(TAG, "CarPlaySession.onNewIntent")
  }

  override fun onCarConfigurationChanged(configuration: Configuration) {
    // we should report this over the bridge
    Log.d(TAG, "CarPlaySession.onCarConfigurationChanged")
  }

  private fun emitDidDisconnectEvent() {
    val params = Arguments.createMap()
    val reactContext = reactInstanceManager.currentReactContext
    if (reactContext != null) {
      reactContext!!
              .getJSModule(RCTDeviceEventEmitter::class.java)
              .emit("didDisconnect", params)
    }
  }

  companion object {
    const val TAG = "CarPlaySession"
  }
}
