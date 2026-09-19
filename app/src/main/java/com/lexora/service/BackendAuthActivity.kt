package com.lexora.service

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.database.OrganizationEntity
import com.lexora.service.core.designsystem.LexoraTheme
import com.lexora.service.core.network.AuthenticationExpiredException
import kotlinx.coroutines.launch

class BackendAuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceSyncScheduler.ensurePeriodic(this)
        ServiceSyncScheduler.enqueue(this)
        val database = LexoraServiceDatabase.create(this)
        val backend = LexoraBackendGraph(this, database)
        setContent {
            LexoraTheme {
                BackendSessionGate(
                    backend = backend,
                    database = database,
                    onReady = { globalOwner ->
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .putExtra(MainActivity.EXTRA_GLOBAL_OWNER, globalOwner),
                        )
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun BackendSessionGate(
    backend: LexoraBackendGraph,
    database: LexoraServiceDatabase,
    onReady: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bootstrapping by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var organizationId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var pendingSso by remember { mutableStateOf<PreparedTrustedAdminSso?>(null) }
    val adminSsoAvailable = remember { TrustedAdminSsoBridge.isAvailable(context) }

    suspend fun bindAndSync(orgId: String) {
        val dao = database.serviceDao()
        val now = System.currentTimeMillis()
        val existing = dao.organizations().firstOrNull { it.id == orgId }
        dao.upsertOrganization(
            OrganizationEntity(
                id = orgId,
                name = existing?.name ?: "Lexora Service",
                isActive = true,
                updatedAtEpochMs = now,
            ),
        )
        dao.setActiveOrganization(orgId)
        backend.syncEngine.runOnce(orgId)
    }

    val adminSsoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val pending = pendingSso ?: return@rememberLauncherForActivityResult
        pendingSso = null
        if (result.resultCode != Activity.RESULT_OK) {
            error = result.data?.getStringExtra(TrustedAdminSsoBridge.EXTRA_ERROR)
                ?: "Вход через Lexora Admin не подтверждён"
            submitting = false
            return@rememberLauncherForActivityResult
        }
        submitting = true
        error = null
        scope.launch {
            runCatching {
                val authorization = TrustedAdminSsoBridge.readResult(result.data, pending.requestState)
                val tokens = backend.trustedAdminSsoApi.exchange(
                    authorizationCode = authorization.authorizationCode,
                    codeVerifier = pending.codeVerifier,
                    targetProductCode = TrustedAdminSsoBridge.TARGET_PRODUCT,
                )
                val orgId = requireNotNull(tokens.organizationId) { "Backend SSO session has no organization" }
                authorization.organizationId?.let { require(it == orgId) { "Lexora Admin organization mismatch" } }
                backend.tokenProvider.save(tokens)
                organizationId = orgId
                bindAndSync(orgId)
                tokens.globalOwner
            }.onSuccess { globalOwner ->
                onReady(globalOwner)
            }.onFailure { failure ->
                backend.authSession.clearLocalSession()
                error = failure.message ?: "Не удалось войти через Lexora Admin"
                submitting = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val restored = backend.authSession.restore()
        val restoredOrganizationId = restored?.organizationId?.takeIf { it.isNotBlank() }
        if (restored == null || restored.refreshExpired(System.currentTimeMillis()) || restoredOrganizationId == null) {
            if (restored != null) backend.authSession.clearLocalSession()
            bootstrapping = false
            return@LaunchedEffect
        }
        organizationId = restoredOrganizationId
        try {
            bindAndSync(restoredOrganizationId)
            onReady(restored.globalOwner)
        } catch (_: AuthenticationExpiredException) {
            backend.authSession.clearLocalSession()
            bootstrapping = false
        } catch (_: Exception) {
            // A still-valid cached session may continue offline; durable sync will resume later.
            onReady(restored.globalOwner)
        }
    }

    if (bootstrapping) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Text("Подключение к Lexora Backend", modifier = Modifier.padding(top = 16.dp))
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Lexora Service", style = MaterialTheme.typography.headlineMedium)
        Text("Вход через общий Lexora Backend", modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))

        if (adminSsoAvailable) {
            Button(
                enabled = !submitting,
                onClick = {
                    runCatching { TrustedAdminSsoBridge.prepare() }
                        .onSuccess { prepared ->
                            pendingSso = prepared
                            submitting = true
                            error = null
                            adminSsoLauncher.launch(prepared.intent)
                        }
                        .onFailure { failure -> error = failure.message ?: "Не удалось открыть Lexora Admin" }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (submitting && pendingSso != null) "Ожидание Lexora Admin…" else "Войти через Lexora Admin")
            }
            Text(
                "Доступно, если в Lexora Admin уже выполнен вход и сервер подтверждает это устройство как доверенное.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
            HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        OutlinedTextField(
            value = organizationId,
            onValueChange = { organizationId = it },
            label = { Text("ID организации") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        OutlinedButton(
            enabled = !submitting && email.isNotBlank() && password.isNotBlank() && organizationId.isNotBlank(),
            onClick = {
                submitting = true
                error = null
                scope.launch {
                    runCatching {
                        val tokens = backend.authSession.login(email, password, organizationId.trim())
                        password = ""
                        bindAndSync(requireNotNull(tokens.organizationId))
                        tokens.globalOwner
                    }.onSuccess { globalOwner ->
                        onReady(globalOwner)
                    }.onFailure { failure ->
                        password = ""
                        error = failure.message ?: "Не удалось войти в Lexora Backend"
                        submitting = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        ) {
            if (submitting && pendingSso == null) CircularProgressIndicator() else Text("Войти по e-mail и паролю")
        }
    }
}
