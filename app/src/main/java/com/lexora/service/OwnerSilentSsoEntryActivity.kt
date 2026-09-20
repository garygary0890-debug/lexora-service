package com.lexora.service

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.database.OrganizationEntity
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Fast entry path for the global platform owner.
 *
 * A product never reads Lexora Admin credentials or tokens. It asks the installed
 * Admin app for a one-time PKCE authorization code. Lexora Admin and Backend both
 * require the current account to be the global owner and the Admin device to be
 * server-confirmed as trusted. Any failure falls back to the normal product login.
 */
class OwnerSilentSsoEntryActivity : ComponentActivity() {
    private lateinit var database: LexoraServiceDatabase
    private lateinit var backend: LexoraBackendGraph
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private var pending: PreparedTrustedAdminSso? = null
    private val routed = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceSyncScheduler.ensurePeriodic(this)
        database = LexoraServiceDatabase.create(this)
        backend = LexoraBackendGraph(this, database)
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val request = pending
            pending = null
            if (result.resultCode != Activity.RESULT_OK || request == null) {
                openNormalLogin()
                return@registerForActivityResult
            }
            exchangeOwnerSso(result.data, request)
        }

        thread(name = "lexora-service-entry") {
            val hasReusableSession = runCatching {
                runBlocking {
                    val session = backend.authSession.restore()
                    session != null &&
                        !session.refreshExpired(System.currentTimeMillis()) &&
                        !session.organizationId.isNullOrBlank()
                }
            }.getOrDefault(false)
            runOnUiThread {
                if (hasReusableSession || !TrustedAdminSsoBridge.isAvailable(this)) openNormalLogin()
                else startSilentOwnerSso()
            }
        }
    }

    private fun startSilentOwnerSso() {
        runCatching { TrustedAdminSsoBridge.prepare(silentOwnerOnly = true) }
            .onSuccess { prepared ->
                pending = prepared
                launcher.launch(prepared.intent)
            }
            .onFailure { openNormalLogin() }
    }

    private fun exchangeOwnerSso(resultIntent: Intent?, request: PreparedTrustedAdminSso) {
        thread(name = "lexora-service-owner-sso") {
            val success = runCatching {
                runBlocking {
                    val authorization = TrustedAdminSsoBridge.readResult(resultIntent, request.requestState)
                    val tokens = backend.trustedAdminSsoApi.exchange(
                        authorizationCode = authorization.authorizationCode,
                        codeVerifier = request.codeVerifier,
                        targetProductCode = TrustedAdminSsoBridge.TARGET_PRODUCT,
                    )
                    require(tokens.globalOwner) { "Silent SSO returned a non-owner session" }
                    val organizationId = requireNotNull(tokens.organizationId) { "Owner session has no organization" }
                    authorization.organizationId?.let { require(it == organizationId) { "Lexora Admin organization mismatch" } }
                    backend.tokenProvider.save(tokens)
                    bindOrganization(organizationId)
                    runCatching { backend.syncEngine.runOnce(organizationId) }
                    true
                }
            }.getOrDefault(false)
            runOnUiThread {
                if (success) openProduct() else openNormalLogin()
            }
        }
    }

    private suspend fun bindOrganization(organizationId: String) {
        val dao = database.serviceDao()
        val existing = dao.organizations().firstOrNull { it.id == organizationId }
        dao.upsertOrganization(
            OrganizationEntity(
                id = organizationId,
                name = existing?.name ?: "Lexora Service",
                isActive = true,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        dao.setActiveOrganization(organizationId)
    }

    private fun openProduct() {
        if (!routed.compareAndSet(false, true)) return
        startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_GLOBAL_OWNER, true))
        finish()
    }

    private fun openNormalLogin() {
        if (!routed.compareAndSet(false, true)) return
        startActivity(Intent(this, BackendAuthActivity::class.java))
        finish()
    }
}
