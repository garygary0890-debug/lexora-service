package com.lexora.service.core.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores only issued session tokens and server session metadata. Passwords are never persisted.
 * Values are encrypted with an AES key generated and retained by AndroidKeyStore.
 */
class AndroidKeystoreTokenProvider(context: Context) : TokenProvider {
    private val prefs = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override suspend fun current(): AuthTokens? {
        val access = prefs.getString(KEY_ACCESS, null)?.let(::decrypt) ?: return null
        val refresh = prefs.getString(KEY_REFRESH, null)?.let(::decrypt) ?: return null
        return AuthTokens(
            accessToken = access,
            accessExpiresAtEpochMs = prefs.getLong(KEY_ACCESS_EXPIRES, 0L),
            refreshToken = refresh,
            refreshExpiresAtEpochMs = prefs.getLong(KEY_REFRESH_EXPIRES, 0L),
            organizationId = prefs.getString(KEY_ORGANIZATION_ID, null),
            membershipId = prefs.getString(KEY_MEMBERSHIP_ID, null),
            permissions = prefs.getStringSet(KEY_PERMISSIONS, emptySet())?.toSet().orEmpty(),
            globalOwner = prefs.getBoolean(KEY_GLOBAL_OWNER, false),
        )
    }

    override suspend fun save(tokens: AuthTokens) {
        prefs.edit()
            .putString(KEY_ACCESS, encrypt(tokens.accessToken))
            .putString(KEY_REFRESH, encrypt(tokens.refreshToken))
            .putLong(KEY_ACCESS_EXPIRES, tokens.accessExpiresAtEpochMs)
            .putLong(KEY_REFRESH_EXPIRES, tokens.refreshExpiresAtEpochMs)
            .putString(KEY_ORGANIZATION_ID, tokens.organizationId)
            .putString(KEY_MEMBERSHIP_ID, tokens.membershipId)
            .putStringSet(KEY_PERMISSIONS, tokens.permissions)
            .putBoolean(KEY_GLOBAL_OWNER, tokens.globalOwner)
            .apply()
    }

    override suspend fun clear() {
        prefs.edit().clear().apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encrypted = Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        return "$iv:$encrypted"
    }

    private fun decrypt(value: String): String {
        val parts = value.split(':', limit = 2)
        require(parts.size == 2) { "Invalid encrypted session value" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)),
        )
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    private companion object {
        const val FILE_NAME = "lexora_service_secure_session"
        const val KEY_ALIAS = "lexora_service_session_key"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_ACCESS_EXPIRES = "access_expires_at"
        const val KEY_REFRESH_EXPIRES = "refresh_expires_at"
        const val KEY_ORGANIZATION_ID = "organization_id"
        const val KEY_MEMBERSHIP_ID = "membership_id"
        const val KEY_PERMISSIONS = "permissions"
        const val KEY_GLOBAL_OWNER = "global_owner"
    }
}
