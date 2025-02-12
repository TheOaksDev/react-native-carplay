package org.birkir.carplay.utils

import android.app.Presentation
import android.content.Context
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.ViewGroup
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.facebook.react.ReactApplication
import com.facebook.react.ReactRootView
import com.facebook.react.bridge.Arguments
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter

/**
 * Renders the view tree into a surface using VirtualDisplay. It runs the ReactNative component registered
 */
class VirtualRenderer(private val context: CarContext, private val moduleName: String) {

  private var rootView: ReactRootView? = null
  private var surfaceContainer: SurfaceContainer? = null

  init {
    context.getCarService(AppManager::class.java).setSurfaceCallback(object : SurfaceCallback {
      override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        this@VirtualRenderer.surfaceContainer = surfaceContainer
        val surface = surfaceContainer.surface
        if (surface == null) {
          Log.w(TAG, "surface is null")
        } else {
          renderPresentation(surfaceContainer)
        }
      }

      override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        Log.d(TAG, "onSurfaceDestroyed")
        this@VirtualRenderer.surfaceContainer = null
      }

      override fun onScroll(distanceX: Float, distanceY: Float) {
        Log.d(TAG, "onScroll: distanceX: $distanceX, distanceY: $distanceY")
        val instanceManager =
                (context.applicationContext as ReactApplication).reactNativeHost.reactInstanceManager
        val params = Arguments.createMap()
        params.putString("distanceX", distanceX.toString())
        params.putString("distanceY", distanceY.toString())
        Log.d(TAG, "moduleName: $moduleName")
        params.putString("templateId", moduleName)
        val reactContext = instanceManager.currentReactContext
        if (reactContext != null) {
          Log.d(TAG, "reactContext is not null")
          reactContext!!
                  .getJSModule(RCTDeviceEventEmitter::class.java)
                  .emit("scroll", params)
        } else {
          Log.w(TAG, "reactContext is null")
        }
      }

      override fun onFling(velocityX: Float, velocityY: Float) {
        Log.d(TAG, "onFling: velocityX: $velocityX, velocityY: $velocityY")
        val instanceManager =
                (context.applicationContext as ReactApplication).reactNativeHost.reactInstanceManager
        val params = Arguments.createMap()
        params.putString("velocityX", velocityX.toString())
        params.putString("velocityY", velocityY.toString())
        Log.d(TAG, "moduleName: $moduleName")
        params.putString("templateId", moduleName)
        val reactContext = instanceManager.currentReactContext
        if (reactContext != null) {
          Log.d(TAG, "reactContext is not null")
          reactContext!!
                  .getJSModule(RCTDeviceEventEmitter::class.java)
                  .emit("fling", params)
        } else {
          Log.w(TAG, "reactContext is null")
        }
      }

      override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        Log.d(TAG, "onScale: scaleFactor: $scaleFactor, focusX: $focusX, focusY: $focusY")
        val instanceManager =
                (context.applicationContext as ReactApplication).reactNativeHost.reactInstanceManager
        val params = Arguments.createMap()
        params.putString("focusX", focusX.toString())
        params.putString("focusY", focusY.toString())
        params.putString("scaleFactor", scaleFactor.toString())
        Log.d(TAG, "moduleName: $moduleName")
        params.putString("templateId", moduleName)
        val reactContext = instanceManager.currentReactContext
        if (reactContext != null) {
          Log.d(TAG, "reactContext is not null")
          reactContext!!
                  .getJSModule(RCTDeviceEventEmitter::class.java)
                  .emit("scale", params)
        } else {
          Log.w(TAG, "reactContext is null")
        }
      }

      override fun onStableAreaChanged(stableArea: Rect) {
        Log.d(TAG, "onStableAreaChanged: stableArea: $stableArea")
        val instanceManager =
                (context.applicationContext as ReactApplication).reactNativeHost.reactInstanceManager
        val params = Arguments.createMap()
        val fullWidth = this@VirtualRenderer.surfaceContainer?.width ?: 0
        val fullHeight = this@VirtualRenderer.surfaceContainer?.height ?: 0
        params.putString("top", stableArea.top.toString())
        params.putString("left", stableArea.left.toString())
        params.putString("right", stableArea.right.toString())
        params.putString("bottom", stableArea.bottom.toString())
        params.putString("width", fullWidth.toString())
        params.putString("height", fullHeight.toString())
        Log.d(TAG, "moduleName: $moduleName")
        params.putString("templateId", moduleName)
        val reactContext = instanceManager.currentReactContext
        if (reactContext != null) {
          Log.d(TAG, "reactContext is not null")
          reactContext!!
                  .getJSModule(RCTDeviceEventEmitter::class.java)
                  .emit("stableAreaChanged", params)
        }
      }

      override fun onVisibleAreaChanged(visibleArea: Rect) {
        Log.d(TAG, "onVisibleAreaChanged: visibleArea: $visibleArea")
        val instanceManager =
                (context.applicationContext as ReactApplication).reactNativeHost.reactInstanceManager
        val params = Arguments.createMap()
        val fullWidth = this@VirtualRenderer.surfaceContainer?.width ?: 0
        val fullHeight = this@VirtualRenderer.surfaceContainer?.height ?: 0
        params.putString("top", visibleArea.top.toString())
        params.putString("left", visibleArea.left.toString())
        params.putString("right", visibleArea.right.toString())
        params.putString("bottom", visibleArea.bottom.toString())
        params.putString("width", fullWidth.toString())
        params.putString("height", fullHeight.toString())
        Log.d(TAG, "moduleName: $moduleName")
        params.putString("templateId", moduleName)
        val reactContext = instanceManager.currentReactContext
        if (reactContext != null) {
          Log.d(TAG, "reactContext is not null")
          reactContext!!
                  .getJSModule(RCTDeviceEventEmitter::class.java)
                  .emit("visibleAreaChanged", params)
        }
      }

      override fun onClick(x: Float, y: Float) {
        Log.d(TAG, "onClick: x: $x, y: $y")
      }
    })
  }

  private fun renderPresentation(container: SurfaceContainer) {
    val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val display = manager.createVirtualDisplay(
            "AndroidAutoMapTemplate",
            container.width,
            container.height,
            container.dpi,
            container.surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
    )
    val presentation = MapPresentation(context, display.display, moduleName)
    presentation.show()
  }

  inner class MapPresentation(context: Context, display: Display, private val moduleName: String) :
          Presentation(context, display) {
    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      val instanceManager =
              (context.applicationContext as ReactApplication).reactNativeHost.reactInstanceManager
      if (rootView == null) {
        Log.d(TAG, "onCreate: rootView is null, initializing rootView")
        rootView = ReactRootView(context).apply {
          startReactApplication(instanceManager, moduleName)
          runApplication()
        }
      } else {
        (rootView?.parent as? ViewGroup)?.removeView(rootView)
      }
      rootView?.let {
        setContentView(it)
      }
    }
  }

  fun unmountComponent() {
    rootView?.unmountReactApplication()
    rootView = null // Reset the root tag after unmounting
  }

  companion object {
    const val TAG = "VirtualRenderer"
  }
}
