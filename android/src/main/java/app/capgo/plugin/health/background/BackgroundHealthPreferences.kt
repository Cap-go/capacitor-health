package app.capgo.plugin.health.background

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.json.JSONObject

class BackgroundHealthPreferences(context: Context) {
    private val preferences: SharedPreferences = createEncryptedPreferences(context.applicationContext)

    fun getConfig(): BackgroundSyncConfig? {
        val rawConfig = preferences.getString(KEY_CONFIG, null) ?: return null
        return BackgroundSyncConfig.fromJson(JSONObject(rawConfig))
    }

    fun requireConfig(): BackgroundSyncConfig {
        return getConfig() ?: throw IllegalStateException("Background sync is not configured.")
    }

    fun saveConfig(config: BackgroundSyncConfig) {
        preferences.edit().putString(KEY_CONFIG, config.toJson().toString()).apply()
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

    companion object {
        private const val PREFS_NAME = "capgo_health_background_sync"
        private const val KEY_CONFIG = "background_sync_config"
    }
}
