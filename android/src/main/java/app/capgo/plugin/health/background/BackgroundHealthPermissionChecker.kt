package app.capgo.plugin.health.background

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import app.capgo.plugin.health.HealthManager

class BackgroundHealthPermissionChecker(
    private val context: Context,
    private val healthManager: HealthManager
) {
    /**
     * Background health sync is only supported on Android 15 (API 35)+ where
     * [READ_HEALTH_DATA_IN_BACKGROUND] can be requested. Below that, treat as unavailable.
     */
    fun isBackgroundSyncSupported(): Boolean {
        return Build.VERSION.SDK_INT >= MIN_SDK_BACKGROUND_SYNC
    }

    /**
     * Health Connect grants only: whether [config.dataTypes] read permissions are granted.
     * Does not check Android background runtime permissions or API-level background support.
     */
    suspend fun hasHealthConnectPermissions(
        client: HealthConnectClient,
        config: BackgroundSyncConfig
    ): Boolean {
        val grantedHealthPermissions = client.permissionController.getGrantedPermissions()
        val requiredHealthPermissions = healthManager.permissionsFor(config.dataTypes, emptyList())
        return grantedHealthPermissions.containsAll(requiredHealthPermissions)
    }

    /**
     * [READ_HEALTH_DATA_IN_BACKGROUND] when running on API 35+; always false below that.
     */
    fun hasBackgroundHealthRuntimePermissions(): Boolean {
        if (!isBackgroundSyncSupported()) {
            return false
        }
        return ContextCompat.checkSelfPermission(context, READ_HEALTH_DATA_IN_BACKGROUND) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Full gate: supported API, Health Connect data permissions, and background runtime permission.
     */
    suspend fun hasAllPermissionsForBackgroundSync(
        client: HealthConnectClient,
        config: BackgroundSyncConfig
    ): Boolean {
        if (!isBackgroundSyncSupported()) {
            return false
        }
        return hasHealthConnectPermissions(client, config) && hasBackgroundHealthRuntimePermissions()
    }

    companion object {
        /** Android 15 — background Health Connect reads require [READ_HEALTH_DATA_IN_BACKGROUND]. */
        private const val MIN_SDK_BACKGROUND_SYNC = 35

        private const val READ_HEALTH_DATA_IN_BACKGROUND =
            "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
    }
}
