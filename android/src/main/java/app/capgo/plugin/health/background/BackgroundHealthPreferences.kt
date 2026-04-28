package app.capgo.plugin.health.background

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.json.JSONObject

/**
 * Persists background-sync wiring in [EncryptedSharedPreferences]. Reads/writes use protobuf-backed
 * keysets internally; rare corruption or OEM bugs surface as
 * `InvalidProtocolBufferException` / `"invalid tag (zero)"` — treat as missing config.
 */
class BackgroundHealthPreferences(context: Context) {
    private val appContext: Context = context.applicationContext

    fun getConfig(): BackgroundSyncConfig? {
        return try {
            val rawConfig = openPreferencesWithRecovery().getString(KEY_CONFIG, null) ?: return null
            BackgroundSyncConfig.fromJson(JSONObject(rawConfig))
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read background sync config from encrypted prefs.", e)
            null
        }
    }

    fun requireConfig(): BackgroundSyncConfig {
        return getConfig() ?: throw IllegalStateException("Background sync is not configured.")
    }

    fun saveConfig(config: BackgroundSyncConfig) {
        try {
            openPreferencesWithRecovery()
                .edit()
                .putString(KEY_CONFIG, config.toJson().toString())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to save background sync config to encrypted prefs.", e)
            throw e
        }
    }

    fun setEnabled(enabled: Boolean) {
        val config = getConfig() ?: return
        saveConfig(config.withEnabled(enabled))
    }

    @Suppress("DEPRECATION")
    private fun createEncryptedPreferences(appContext: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            appContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun openPreferencesWithRecovery(): SharedPreferences {
        return try {
            createEncryptedPreferences(appContext)
        } catch (e: Exception) {
            if (!isRecoverableEncryptedPrefsFailure(e)) {
                throw e
            }
            Log.w(
                TAG,
                "Encrypted prefs open failed; clearing background sync encrypted state and retrying.",
                e
            )
            clearEncryptedPrefsState()
            createEncryptedPreferences(appContext)
        }
    }

    private fun clearEncryptedPrefsState() {
        val prefsFiles = listOf(
            PREFS_NAME,
            "__androidx_security_crypto_encrypted_prefs__",
            "androidx_security_crypto_encrypted_prefs"
        )
        for (name in prefsFiles) {
            runCatching {
                appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()
            }.onFailure { clearErr ->
                Log.w(TAG, "Failed clearing encrypted prefs file: $name", clearErr)
            }
        }
    }

    private fun isRecoverableEncryptedPrefsFailure(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            val message = current.message?.lowercase() ?: ""
            val className = current::class.java.name
            if (
                message.contains("invalid tag (zero)") ||
                message.contains("protocol message contained an invalid tag") ||
                className.contains("InvalidProtocolBufferException")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    companion object {
        private const val TAG = "BgHealthPrefs"
        private const val PREFS_NAME = "capgo_health_background_sync"
        private const val KEY_CONFIG = "background_sync_config"
    }
}
