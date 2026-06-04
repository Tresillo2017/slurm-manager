package com.tomasps.slurmmanager.data.credential

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "slrum_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun savePassword(serverId: String, password: String) {
        prefs.edit().putString(passwordKey(serverId), password).apply()
    }

    fun getPassword(serverId: String): String? =
        prefs.getString(passwordKey(serverId), null)

    fun saveSshPrivateKey(serverId: String, privateKeyPem: String) {
        prefs.edit().putString(sshKeyKey(serverId), privateKeyPem).apply()
    }

    fun getSshPrivateKey(serverId: String): String? =
        prefs.getString(sshKeyKey(serverId), null)

    fun deleteCredentials(serverId: String) {
        prefs.edit()
            .remove(passwordKey(serverId))
            .remove(sshKeyKey(serverId))
            .apply()
    }

    private fun passwordKey(serverId: String) = "pwd_$serverId"
    private fun sshKeyKey(serverId: String) = "sshkey_$serverId"
}
