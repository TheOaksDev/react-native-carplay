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
    if (carContext == null) {
      Log.e("CarPlayModule", "carContext is null")
    }
    if (eventEmitter == null) {
      Log.e("CarPlayModule", "eventEmitter is null")
    }
    if (carScreens == null) {
      Log.e("CarPlayModule", "carScreens is null")
    }

    // Ensure all necessary components are initialized
    if (carContext != null && eventEmitter != null && carScreens != null) {
      parser = Parser(carContext, CarScreenContext("", eventEmitter!!, carScreens))
      this.carContext = carContext
      this.currentCarScreen = currentCarScreen
      screenManager = currentCarScreen.screenManager

      // TemplateID must be hardset to default root component
      // this method is only called once when the car context is initialized
      carScreens["wridzCarplayRoot"] = this.currentCarScreen
      carContext.onBackPressedDispatcher.addCallback(
              object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                  eventEmitter?.backButtonPressed(screenManager?.top?.marker)
                }
              }
      )
      eventEmitter?.didConnect()
    } else {
      Log.e("CarPlayModule", "Failed to initialize Parser due to null dependencies")
    }
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
      Log.d(TAG, "Updating template $templateId")
      carTemplates[templateId] = config
      val screen = getScreen(templateId)
      if (screen != null) {
        Log.d(TAG, "Screen found for templateId $templateId")
        val carScreenContext = carScreenContexts[screen]
        if (carScreenContext != null) {
          Log.d(TAG, "CarScreenContext found for templateId $templateId")
          val template = parseTemplate(config, carScreenContext)
          screen.invalidate()
          screen.setTemplate(template, templateId, config)

          val newContext = createCarScreenContext(screen)
          carScreenContexts[screen] = newContext
          carScreens[templateId] = screen
        }
      } else {
        Log.d(TAG, "Screen with templateId $templateId is null")
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
      } else {
        Log.d(TAG, "Screen with templateId $templateId is null")
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

      currentCarScreen = screenManager?.top as? CarScreen

      if (currentCarScreen == null) {
        Log.d(TAG, "Current screen is null")
        return@post
      }

      if (currentCarScreen!!.marker == "wridzCarplayRoot") {
        Log.d(TAG, "Current screen is already at the root")
        return@post
      }

      // Check if currentCarScreen is not null before removing
      val screensToPop = mutableListOf<CarScreen>()
      while (currentCarScreen != null && currentCarScreen!!.marker != "wridzCarplayRoot") {
        screenManager!!.top?.let { screensToPop.add(it as CarScreen) }
        screenManager!!.pop()
        currentCarScreen = screenManager?.top as? CarScreen
      }

      screenManager?.popTo("wridzCarplayRoot")

      // Remove and invalidate the screens
      screensToPop.forEach { screen ->
        removeScreen(screen)
        screen.invalidate()

        // Remove from hashmaps
        carScreens.remove(screen.marker)
        carTemplates.remove(screen.marker)
        carScreenContexts.remove(screen)
        Log.d(TAG, "Removed screen: ${screen.marker}")
      }

      currentCarScreen = screenManager?.top as? CarScreen
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
    handler.post { 
      if (screenManager == null) {
        Log.e(TAG, "ScreenManager is null, cannot pop to wridzCarplayRoot")
        return@post
      }

      currentCarScreen = screenManager?.top as? CarScreen

      if (currentCarScreen == null) {
        Log.d(TAG, "Current screen is null")
        return@post
      }

      if (currentCarScreen!!.marker == templateId) {
        Log.d(TAG, "Current screen is already at the root")
        return@post
      }
      
      val screensToPop = mutableListOf<CarScreen>()
      while (currentCarScreen != null && currentCarScreen!!.marker != templateId) {
        screenManager!!.top?.let { screensToPop.add(it as CarScreen) }
        screenManager!!.pop()
        currentCarScreen = screenManager?.top as? CarScreen
      }

      screenManager?.popTo(templateId)

      // Remove and invalidate the screens
      screensToPop.forEach { screen ->
        removeScreen(screen)
        screen.invalidate()

        // Remove from hashmaps
        carScreens.remove(screen.marker)
        carTemplates.remove(screen.marker)
        carScreenContexts.remove(screen)
        Log.d(TAG, "Removed screen: ${screen.marker}")
      }

      currentCarScreen = screenManager?.top as? CarScreen
    }
  }

  @ReactMethod
  fun popTemplate(animated: Boolean?) {
    handler.post {
      if (screenManager == null) {
        Log.e(TAG, "ScreenManager is null, cannot pop to wridzCarplayRoot")
        return@post
      }

      currentCarScreen = screenManager?.top as? CarScreen

      if (currentCarScreen == null) {
        Log.d(TAG, "Current screen is null")
        return@post
      }
      
      screenManager!!.pop()
      removeScreen(currentCarScreen)
      currentCarScreen?.invalidate()

      // Remove from hashmaps
      carScreens.remove(currentCarScreen?.marker)
      carTemplates.remove(currentCarScreen?.marker)
      carScreenContexts.remove(currentCarScreen)

      currentCarScreen = screenManager?.top as? CarScreen
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
      val templateId = props.getString("templateId")
      val title = parser.parseCarText(props.getString("title")!!, props)
      val duration = props.getInt("duration").toLong()
      Log.d("alert Emitter ID", eventEmitter.toString())

      var internalEventEmitter = eventEmitter
      var internalParser = parser
      val screen = getScreen(templateId!!)

      if (screen != null) {
        val carScreenContext = carScreenContexts[screen]
        if (carScreenContext != null) {
          internalEventEmitter = carScreenContext.eventEmitter
          internalParser = Parser(carContext, carScreenContext)
        }
      }

      Log.d("alert Emitter ID", internalEventEmitter.toString())
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
                                    internalEventEmitter?.alertActionPressed("cancel", templateId, reasonString)
                                  }
                                  override fun onDismiss() {
                                    Log.d("onDismiss Emitter ID", eventEmitter.toString())
                                    internalEventEmitter?.alertActionPressed("dismiss", templateId)
                                  }
                                }
                        )
                        props.getString("subtitle")?.let {
                          setSubtitle(internalParser.parseCarText(it, props))
                        }
                        props.getMap("icon")?.let { setIcon(internalParser.parseCarIcon(it)) }
                        props.getArray("actions")?.let {
                          for (i in 0 until it.size()) {
                            addAction(internalParser.parseAction(it.getMap(i)))
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
          val newContext = createCarScreenContext(screen)
          carScreenContexts[screen] = newContext
          carScreens[templateId] = screen
          // val template = parseTemplate(config, carScreenContext)
          // screen.invalidate()
          // screen.setTemplate(template, templateId, config)
          // carScreens[templateId] = screen
        }
      } else {
        Log.d(TAG, "Screen with templateId $templateId is null")
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
      } else {
        Log.d(TAG, "Screen with templateId $templateId is null")
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

  @ReactMethod
  fun getCurrentTemplateId(promise: Promise) {
    handler.post {
      if (screenManager == null) {
        Log.d(TAG, "ScreenManager is null")
        promise.resolve(Arguments.createMap().apply {
          putString("templateId", "unknown")
        })
        return@post
      }

      currentCarScreen = screenManager?.top as? CarScreen

      if (currentCarScreen == null) {
        Log.d(TAG, "Current screen is null")
        promise.resolve(Arguments.createMap().apply {
          putString("templateId", "unknown")
        })
        return@post
      }

      promise.resolve(Arguments.createMap().apply {
        putString("templateId", currentCarScreen?.marker ?: "unknown")
      })
    }
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
        return@post
      }

      try {
        val displayMetrics = carContext.resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val dimensions = Arguments.createMap().apply {
            putInt("width", width)
            putInt("height", height)
        }

        promise.resolve(dimensions)
      } catch (e: Exception) {
          promise.reject("Error", "Failed to get screen dimensions: ${e.message}")
      }
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
    return carScreens[name]
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
