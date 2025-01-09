package org.birkir.carplay

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.ScreenManager
import androidx.car.app.model.Alert
import androidx.car.app.model.AlertCallback
import androidx.car.app.model.Template
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.debug.DevSettingsModule
import java.util.WeakHashMap
import org.birkir.carplay.parser.Parser
import org.birkir.carplay.parser.TemplateParser
import org.birkir.carplay.screens.CarScreen
import org.birkir.carplay.screens.CarScreenContext
import org.birkir.carplay.utils.EventEmitter

@ReactModule(name = CarPlayModule.NAME)
class CarPlayModule internal constructor(private val reactContext: ReactApplicationContext) :
        ReactContextBaseJavaModule(reactContext) {

  private lateinit var carContext: CarContext
  private lateinit var parser: Parser

  private var currentCarScreen: CarScreen? = null
  private var screenManager: ScreenManager? = null
  private val carScreens: WeakHashMap<String, CarScreen> = WeakHashMap()
  private val carTemplates: WeakHashMap<String, ReadableMap> = WeakHashMap()
  private val carScreenContexts: WeakHashMap<CarScreen, CarScreenContext> = WeakHashMap()
  private val handler: Handler = Handler(Looper.getMainLooper())

  // Global event emitter (no templateId's)
  private var eventEmitter: EventEmitter? = null

  init {
    reactContext.addLifecycleEventListener(
            object : LifecycleEventListener {
              override fun onHostResume() {
                eventEmitter = EventEmitter(reactContext, "")
                reactContext
                        .getNativeModule(DevSettingsModule::class.java)
                        ?.addMenuItem("Reload Android Auto")
              }

              override fun onHostPause() {
                Log.d(TAG, "onHostPause")
              }
              override fun onHostDestroy() {
                Log.d(TAG, "onHostDestroy")
              }
            }
    )
  }

  override fun getName(): String {
    return NAME
  }

  fun setCarContext(carContext: CarContext, currentCarScreen: CarScreen) {
    parser = Parser(carContext, CarScreenContext("", eventEmitter!!, carScreens))
    this.carContext = carContext
    this.currentCarScreen = currentCarScreen
    screenManager = currentCarScreen.screenManager
    carScreens["wridzCarplayRoot"] = this.currentCarScreen
    carContext.onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
              override fun handleOnBackPressed() {
                eventEmitter?.backButtonPressed(screenManager?.top?.marker)
              }
            }
    )
    eventEmitter?.didConnect()
  }

  private fun parseTemplate(config: ReadableMap, carScreenContext: CarScreenContext): Template {
    val factory = TemplateParser(carContext, carScreenContext)
    return factory.parse(config)
  }

  @ReactMethod
  fun checkForConnection() {
    eventEmitter?.didConnect()
  }

  @ReactMethod
  fun createTemplate(templateId: String, config: ReadableMap, callback: Callback?) {
    handler.post {
      Log.d(TAG, "Creating template $templateId")

      // Store the template
      carTemplates[templateId] = config

      try {
        createScreen(templateId)
        callback?.invoke()
      } catch (err: IllegalArgumentException) {
        val args = Arguments.createMap()
        args.putString("error", "Failed to parse template '$templateId': ${err.message}")
        callback?.invoke(args)
      }
    }
  }

  @ReactMethod
  fun updateTemplate(templateId: String, config: ReadableMap) {
    handler.post {
      carTemplates[templateId] = config
      val screen = getScreen(templateId)
      if (screen != null) {
        val carScreenContext = carScreenContexts[screen]
        if (carScreenContext != null) {
          val template = parseTemplate(config, carScreenContext)
          screen.setTemplate(template, templateId, config)
          screen.invalidate()
        }
      }
    }
  }

  @ReactMethod
  fun setRootTemplate(templateId: String, animated: Boolean?) {
    Log.d(TAG, "set Root Template for $templateId")
    handler.post {
      val screen = getScreen(templateId)
      if (screen != null) {
        currentCarScreen = screen
        screenManager?.popToRoot()
        screenManager?.push(screen)
      }
    }
  }

  @ReactMethod
  fun popToRootTemplate(animated: Boolean?) {
    Log.d(TAG, "Pop to Root Template")
    handler.post {

      if (screenManager == null) {
        Log.e(TAG, "ScreenManager is null, cannot pop to wridzCarplayRoot")
        return@post
      }
    
      screenManager?.popToRoot()
      Log.d(TAG, "Popped to wridzCarplayRoot")

      // Check if currentCarScreen is not null before removing
      currentCarScreen?.let {
        Log.d(TAG, "Removing current screen: $it")
        removeScreen(it)
    } ?: Log.d(TAG, "No current screen to remove")

      currentCarScreen = screenManager?.top as? CarScreen
      currentCarScreen?.invalidate()
      Log.d(TAG, "Current screen after pop: $currentCarScreen")

      val screen = getScreen("wridzCarplayRoot")
      if (screen != null) {
        Log.d(TAG, "Pushing wridzCarplayRoot screen")
        screenManager?.push(screen)
      } else {
        Log.e(TAG, "wridzCarplayRoot screen not found")
      }
    }
  }

  @ReactMethod
  fun pushTemplate(templateId: String, animated: Boolean?) {
    handler.post {
      Log.d(TAG, "Attempting to push template: $templateId")
      val screen = getScreen(templateId)
      if (screen != null) {
        Log.d(TAG, "Pushing template: $templateId")
        currentCarScreen = screen
        screenManager?.push(screen)

        logCarScreens()
      } else {
        Log.e(TAG, "Template not found: $templateId")
        logCarScreens()
      }
    }
  }

  @ReactMethod
  fun getTemplate(templateId: String, promise: Promise) {
    promise.resolve(carTemplates[templateId])
  }

  @ReactMethod
  fun popToTemplate(templateId: String, animated: Boolean?) {
    handler.post { screenManager?.popTo(templateId) }
  }

  @ReactMethod
  fun popTemplate(animated: Boolean?) {
    handler.post {
      screenManager!!.pop()
      removeScreen(currentCarScreen)
      currentCarScreen = screenManager!!.top as CarScreen
      currentCarScreen?.invalidate()

      Log.d(TAG, "Current screen after pop: $currentCarScreen")
    }
  }

  @ReactMethod
  fun presentTemplate(templateId: String?, animated: Boolean?) {
    // void
  }

  @ReactMethod
  fun dismissTemplate(templateId: String?, animated: Boolean?) {
    // void
  }

  // pragma: Android Auto only stuff

  @ReactMethod
  fun toast(text: String, duration: Int) {
    if (!::carContext.isInitialized) {
      Log.e(TAG, "carContext is not initialized. Cannot show toast.")
      return
    }
    CarToast.makeText(carContext, text, duration).show()
  }

  @ReactMethod
  fun alert(props: ReadableMap) {
    handler.post {
      val id = props.getInt("id")
      val title = parser.parseCarText(props.getString("title")!!, props)
      val duration = props.getInt("duration").toLong()
      Log.d("alert Emitter ID", eventEmitter.toString())

      val alert =
              Alert.Builder(id, title, duration)
                      .apply {
                        setCallback(
                                object : AlertCallback {
                                  override fun onCancel(reason: Int) {
                                    val reasonString =
                                            when (reason) {
                                              AlertCallback.REASON_TIMEOUT -> "timeout"
                                              AlertCallback.REASON_USER_ACTION -> "userAction"
                                              AlertCallback.REASON_NOT_SUPPORTED -> "notSupported"
                                              else -> "unknown"
                                            }
                                    Log.d("onCancel Emitter ID", eventEmitter.toString())
                                    eventEmitter?.alertActionPressed("cancel", reasonString)
                                  }
                                  override fun onDismiss() {
                                    Log.d("onDismiss Emitter ID", eventEmitter.toString())
                                    eventEmitter?.alertActionPressed("dismiss")
                                  }
                                }
                        )
                        props.getString("subtitle")?.let {
                          setSubtitle(parser.parseCarText(it, props))
                        }
                        props.getMap("icon")?.let { setIcon(parser.parseCarIcon(it)) }
                        props.getArray("actions")?.let {
                          for (i in 0 until it.size()) {

                            addAction(parser.parseAction(it.getMap(i)))
                          }
                        }
                      }
                      .build()
      carContext.getCarService(AppManager::class.java).showAlert(alert)
    }
  }

  @ReactMethod
  fun dismissAlert(alertId: Int) {
    carContext.getCarService(AppManager::class.java).dismissAlert(alertId)
  }

  @ReactMethod
  fun updateMapTemplateMapButtons(templateId: String, config: ReadableMap) {
    handler.post {
      carTemplates[templateId] = config
      val screen = getScreen(templateId)
      if (screen != null) {
        val carScreenContext = carScreenContexts[screen]
        if (carScreenContext != null) {
          val template = parseTemplate(config, carScreenContext)
          screen.setTemplate(template, templateId, config)
          screen.invalidate()
        }
      }
    }
  }

  @ReactMethod
  fun showPanningInterface(templateId: String, animated: Boolean) {
    Log.d(TAG, "showPanningInterface")
    //carContext.getCarService(AppManager::class.java).showPanningInterface(animated)
  }

  @ReactMethod
  fun dismissPanningInterface(templateId: String, animated: Boolean) {
    Log.d(TAG, "dismissPanningInterface")
    //carContext.getCarService(AppManager::class.java).dismissPanningInterface(animated)
  }

  @ReactMethod
  fun invalidate(templateId: String) {
    handler.post {
      val screen = getScreen(templateId)
      if (screen === screenManager!!.top) {
        Log.d(TAG, "Invalidated screen $templateId")
        screen.invalidate()
      }
    }
  }

  @ReactMethod
  fun reload() {
    val intent = Intent("org.birkir.carplay.APP_RELOAD")
    reactContext.sendBroadcast(intent)
  }

  @ReactMethod
  fun getHostInfo(promise: Promise) {
    return promise.resolve(
            Arguments.createMap().apply {
              carContext.hostInfo?.packageName?.let { putString("packageName", it) }
              carContext.hostInfo?.uid?.let { putInt("uid", it) }
            }
    )
  }

  // Others

  @ReactMethod
  fun addListener(eventName: String) {
    Log.d(TAG, "listener added $eventName")
  }

  @ReactMethod
  fun removeListeners(count: Int) {
    Log.d(TAG, "remove listeners $count")
  }

  @ReactMethod
  fun getScreenDimensions(promise: Promise) {
    handler.post {
      if (!::carContext.isInitialized) {
        promise.reject("CarContextNotInitialized", "CarContext is not initialized.")
      }

      val displayMetrics = carContext.resources.displayMetrics
      val width = displayMetrics.widthPixels
      val height = displayMetrics.heightPixels

      val dimensions =
              Arguments.createMap().apply {
                putInt("width", width)
                putInt("height", height)
              }

      promise.resolve(dimensions)
    }
  }

  private fun createCarScreenContext(screen: CarScreen): CarScreenContext {
    val templateId = screen.marker!!
    return CarScreenContext(templateId, EventEmitter(reactContext, templateId), carScreens)
  }

  private fun createScreen(templateId: String): CarScreen? {
    if (!::carContext.isInitialized) {
      Log.e(TAG, "carContext not initialized")
      return null
    }
    val config = carTemplates[templateId]
    if (config != null) {
      val screen = CarScreen(carContext)
      screen.marker = templateId

      // context
      carScreenContexts.remove(screen)
      val carScreenContext = createCarScreenContext(screen)
      carScreenContexts[screen] = carScreenContext

      val template = parseTemplate(config, carScreenContext)
      screen.setTemplate(template, templateId, config)
      carScreens[templateId] = screen

      return screen
    }
    return null
  }

  private fun getScreen(name: String): CarScreen? {
    return carScreens[name] ?: createScreen(name)
  }

  private fun logCarScreens() {
    if (carScreens.isEmpty()) {
        Log.d(TAG, "carScreens is empty")
    } else {
        Log.d(TAG, "Logging carScreens contents:")
        for ((key, screen) in carScreens) {
            Log.d(TAG, "Screen ID: $key, Screen: $screen")
        }
    }
  }

  private fun removeScreen(screen: CarScreen?) {
    logCarScreens()

    if (screen == null) {
      Log.d(TAG, "Attempted to remove a null screen")
      return
    }
    Log.d(TAG, "Removing screen: ${screen.marker}")
    val params = WritableNativeMap()
    params.putString("screen", screen!!.marker)
    carScreens.values.remove(screen)

    logCarScreens()
  }

  companion object {
    const val NAME = "RNCarPlay"
    const val TAG = "CarPlay"
  }
}
