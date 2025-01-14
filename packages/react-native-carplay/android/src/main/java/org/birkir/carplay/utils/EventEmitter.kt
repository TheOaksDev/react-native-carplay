package org.birkir.carplay.utils

import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter


class EventEmitter(
  private var reactContext: ReactContext? = null,
  private var templateId: String? = null
) {

  companion object {
    const val DidConnect = "didConnect"
    const val DidDisconnect = "didDisconnect"

    // interface
    const val BarButtonPressed = "barButtonPressed"
    const val BackButtonPressed = "backButtonPressed"
    const val DidAppear = "didAppear"
    const val DidDisappear = "didDisappear"
    const val WillAppear = "willAppear"
    const val WillDisappear = "willDisappear"
    const val ButtonPressed = "buttonPressed"
    const val Scroll = "scroll"
    const val Scale = "scale"
    const val Fling = "fling"

    // grid
    const val GridButtonPressed = "gridButtonPressed"

    // information
    const val ActionButtonPressed = "actionButtonPressed"

    // list
    const val DidSelectListItem = "didSelectListItem"

    // search
    const val UpdatedSearchText = "updatedSearchText"
    const val SearchButtonPressed = "searchButtonPressed"
    const val SelectedResult = "selectedResult"

    // tab bar
    const val DidSelectTemplate = "didSelectTemplate"

    // now playing
    const val UpNextButtonPressed = "upNextButtonPressed"
    const val AlbumArtistButtonPressed = "albumArtistButtonPressed"

    // poi
    const val DidSelectPointOfInterest = "didSelectPointOfInterest"

    // map
    const val MapButtonPressed = "mapButtonPressed"
    const val PaneButtonPressed = "paneButtonPressed"
    const val DidUpdatePanGestureWithTranslation = "didUpdatePanGestureWithTranslation"
    const val DidEndPanGestureWithVelocity = "didEndPanGestureWithVelocity"
    const val PanBeganWithDirection = "panBeganWithDirection"
    const val PanEndedWithDirection = "panEndedWithDirection"
    const val PanWithDirection = "panWithDirection"
    const val DidBeginPanGesture = "didBeginPanGesture"
    const val DidDismissPanningInterface = "didDismissPanningInterface"
    const val WillDismissPanningInterface = "willDismissPanningInterface"
    const val DidShowPanningInterface = "didShowPanningInterface"
    const val DidDismissNavigationAlert = "didDismissNavigationAlert"
    const val WillDismissNavigationAlert = "willDismissNavigationAlert"
    const val DidShowNavigationAlert = "didShowNavigationAlert"
    const val WillShowNavigationAlert = "willShowNavigationAlert"
    const val DidCancelNavigation = "didCancelNavigation"
    const val AlertActionPressed = "alertActionPressed"
    const val SelectedPreviewForTrip = "selectedPreviewForTrip"
    const val StartedTrip = "startedTrip"
  }

  fun didConnect() {
    Log.d("EventEmitter", "Did connect")
    emit(DidConnect)
  }

  fun didDisconnect() {
    Log.d("EventEmitter", "Did disconnect")
    emit(DidDisconnect)
  }

  fun buttonPressed(buttonId: String) {
    emit(ButtonPressed, Arguments.createMap().apply {
      putString("buttonId", buttonId)
    })
  }

  fun mapButtonPressed(buttonId: String) {
    emit(MapButtonPressed, Arguments.createMap().apply {
      putString("buttonId", buttonId)
    })
  }

  fun paneButtonPressed(buttonId: String) {
    emit(PaneButtonPressed, Arguments.createMap().apply {
      putString("buttonId", buttonId)
    })
  }

  fun barButtonPressed(buttonId: String) {
    emit(BarButtonPressed, Arguments.createMap().apply {
      putString("buttonId", buttonId)
    })
  }

  fun backButtonPressed(templateId: String?) {
    emit(BackButtonPressed, Arguments.createMap().apply {
      templateId?.let { putString("templateId", templateId) }
    })
  }

  fun didSelectListItem(id: String, index: Int) {
    emit(DidSelectListItem, Arguments.createMap().apply {
      putString("id", id)
      putInt("index", index)
    })
  }

  fun didSelectTemplate(selectedTemplateId: String) {
    emit(DidSelectTemplate, Arguments.createMap().apply {
      putString("selectedTemplateId", selectedTemplateId)
    })
  }

  fun updatedSearchText(searchText: String) {
    emit(UpdatedSearchText, Arguments.createMap().apply {
      putString("searchText", searchText)
    })
  }

  fun searchButtonPressed(searchText: String) {
    emit(SearchButtonPressed, Arguments.createMap().apply {
      putString("searchText", searchText)
    })
  }

  fun alertActionPressed(type: String, reason: String? = null) {
    emit(AlertActionPressed, Arguments.createMap().apply {
      putString("type", type);
      reason?.let { putString("reason", reason) }
    });
  }

  fun selectedResult(index: Int, id: String?) {
    emit(SelectedResult, Arguments.createMap().apply {
      id?.let { putString("id", id) }
      putInt("index", index)
    })
  }

  fun gridButtonPressed(id: String, index: Int) {
    val event = Arguments.createMap()
    event.putString("id", id)
    event.putInt("index", index)
    emit(GridButtonPressed, event)
  }

  fun onScroll(distanceX: Float, distanceY: Float) {
    emit(Scroll, Arguments.createMap().apply {
      Log.d("EventEmitter", "Emitting scroll event with distanceX: $distanceX and distanceY: $distanceY")
      putString("distanceX", distanceX.toString())
      putString("distanceY", distanceY.toString())
    })
  }

  fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
    emit(Scale, Arguments.createMap().apply {
      Log.d("EventEmitter", "Emitting scale event with focusX: $focusX, focusY: $focusY, and scaleFactor: $scaleFactor")
      putString("focusX", focusX.toString())
      putString("focusY", focusY.toString())
      putString("scaleFactor", scaleFactor.toString())
    })
  }

  fun onFling(velocityX: Float, velocityY: Float) {
    emit(Fling, Arguments.createMap().apply {
      Log.d("EventEmitter", "Emitting fling event with velocityX: $velocityX and velocityY: $velocityY")
      putString("velocityX", velocityX.toString())
      putString("velocityY", velocityY.toString())
    })
  }

  fun didShowPanningInterface() {
    Log.d("EventEmitter", "Did show panning interface")
    emit(DidShowPanningInterface)
  }

  fun didDismissPanningInterface() {
    Log.d("EventEmitter", "Did dismiss panning interface")
    emit(DidDismissPanningInterface)
  }

  private fun emit(eventName: String, data: WritableMap = Arguments.createMap()) {
    if (reactContext == null) {
      Log.e("RNCarPlay", "Could not send event $eventName. React context is null!")
      return
    }
    Log.d("EventEmitter", "Emitting event $eventName with data $data")
    Log.d("EventEmitter", "Template ID: $templateId")
    if (templateId != null && !data.hasKey("templateId")) {
      Log.d("EventEmitter", "Setting templateId to $templateId")
      data.putString("templateId", templateId)
    } else if (!data.hasKey("templateId")) {
      // issue with templateID being null/undefined
      // when its not defined then the events do not pass to the JS side
      Log.d("EventEmitter", "Setting templateId to wridzCarplayRoot")
      data.putString("templateId", "wridzCarplayRoot") // temporary fix to set the templateId to a hardcoded ID value
    }

    reactContext!!
      .getJSModule(RCTDeviceEventEmitter::class.java)
      .emit(eventName, data)
  }

}
