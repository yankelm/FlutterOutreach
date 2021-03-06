package com.flutter.flutter_outreach

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.annotation.VisibleForTesting
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener

interface PermissionsRegistry {
    fun addListener(
        handler: RequestPermissionsResultListener?
    )
}

interface ResultCallback {
    fun onResult(errorCode: String?, errorDescription: String?)
}


class ExternalStoragePermissions {


    private var ongoing = false

    fun requestPermissions(
        activity: Activity,
        permissionsRegistry: PermissionsRegistry,
        callback: ResultCallback
    ) {
        if (ongoing) {
            callback.onResult("storagePermission", "Internal Storage permission request ongoing")
        }
        if (!hasExternalStoragePermission(activity)) {
            permissionsRegistry.addListener(
                StorageRequestPermissionsListener(
                    object : ResultCallback {
                        override fun onResult(errorCode: String?, errorDescription: String?) {
                            ongoing = false
                            callback.onResult(errorCode, errorDescription)
                        }
                    })
            )
            ongoing = true
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                STORAGE_REQUEST_ID
            )
        } else {
            // Permissions already exist. Call the callback with success.
            callback.onResult(null, null)
        }
    }

    private fun hasExternalStoragePermission(activity: Activity): Boolean {
        return (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
    }


    @VisibleForTesting
    internal class StorageRequestPermissionsListener @VisibleForTesting constructor(private val callback: ResultCallback) :
        RequestPermissionsResultListener {
        // There's no way to unregister permission listeners in the v1 embedding, so we'll be called
        // duplicate times in cases where the user denies and then grants a permission. Keep track of if
        // we've responded before and bail out of handling the callback manually if this is a repeat
        // call.
        var alreadyCalled = false
        override fun onRequestPermissionsResult(
            id: Int,
            permissions: Array<String>,
            grantResults: IntArray
        ): Boolean {
            if (alreadyCalled || id != STORAGE_REQUEST_ID) {
                return false
            }
            alreadyCalled = true
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                callback.onResult("storagePermission", "Storage permission not granted")
            } else {
                callback.onResult(null, null)
            }
            return true
        }
    }

    companion object {
        private const val STORAGE_REQUEST_ID = 9796
    }
}