package com.lexora.service

import android.content.Context
import android.content.Intent
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

data class PreparedTrustedAdminSso(
    val intent: Intent,
    val codeVerifier: String,
    val requestState: String,
    val silentOwnerOnly: Boolean = false,
)

data class TrustedAdminSsoResult(
    val authorizationCode: String,
    val organizationId: String?,
)

object TrustedAdminSsoBridge {
    const val ADMIN_PACKAGE = "com.lexora.admin"
    const val ACTION_AUTHORIZE = "com.lexora.admin.action.TRUSTED_SSO_AUTHORIZE"
    const val TARGET_PRODUCT = "LEXORA_SERVICE"
    const val EXTRA_TARGET_PRODUCT_CODE = "lexora.target_product_code"
    const val EXTRA_PKCE_CHALLENGE = "lexora.pkce_challenge"
    const val EXTRA_REQUEST_STATE = "lexora.request_state"
    const val EXTRA_AUTHORIZATION_CODE = "lexora.authorization_code"
    const val EXTRA_ORGANIZATION_ID = "lexora.organization_id"
    const val EXTRA_ERROR = "lexora.error"
    const val EXTRA_SILENT_OWNER_ONLY = "lexora.silent_owner_only"

    private val random = SecureRandom()

    fun isAvailable(context: Context): Boolean = runCatching {
        context.packageManager.resolveActivity(
            Intent(ACTION_AUTHORIZE).setPackage(ADMIN_PACKAGE),
            0,
        ) != null
    }.getOrDefault(false)

    fun prepare(silentOwnerOnly: Boolean = false): PreparedTrustedAdminSso {
        val verifier = randomToken(32)
        val challenge = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        val state = randomToken(24)
        val intent = Intent(ACTION_AUTHORIZE)
            .setPackage(ADMIN_PACKAGE)
            .putExtra(EXTRA_TARGET_PRODUCT_CODE, TARGET_PRODUCT)
            .putExtra(EXTRA_PKCE_CHALLENGE, challenge)
            .putExtra(EXTRA_REQUEST_STATE, state)
            .putExtra(EXTRA_SILENT_OWNER_ONLY, silentOwnerOnly)
        return PreparedTrustedAdminSso(intent, verifier, state, silentOwnerOnly)
    }

    fun readResult(intent: Intent?, expectedState: String): TrustedAdminSsoResult {
        require(intent != null) { "Lexora Admin did not return authorization data" }
        require(intent.getStringExtra(EXTRA_REQUEST_STATE) == expectedState) { "Lexora Admin authorization state mismatch" }
        require(intent.getStringExtra(EXTRA_TARGET_PRODUCT_CODE) == TARGET_PRODUCT) { "Unexpected Lexora product authorization" }
        val code = intent.getStringExtra(EXTRA_AUTHORIZATION_CODE)?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException(intent.getStringExtra(EXTRA_ERROR) ?: "Lexora Admin did not authorize sign-in")
        return TrustedAdminSsoResult(
            authorizationCode = code,
            organizationId = intent.getStringExtra(EXTRA_ORGANIZATION_ID)?.takeIf(String::isNotBlank),
        )
    }

    private fun randomToken(bytes: Int): String = ByteArray(bytes)
        .also(random::nextBytes)
        .let { Base64.encodeToString(it, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) }
}
