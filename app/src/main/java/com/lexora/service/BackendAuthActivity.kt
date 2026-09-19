package com.lexora.service

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
        val database = LexoraServiceDatabase.create(this)
        val backend = LexoraBackendGraph(this, database)
        setContent {
            LexoraTheme {
                BackendSessionGate(
                    backend = backend,
                    database = database,
                    onReady = {
                        startActivity(Intent(this, MainActivity::class.java))
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
    onReady: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var bootstrapping by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var organizationId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        val restored = backend.authSession.restore()
        if (restored == null || restored.refreshExpired(System.currentTimeMillis()) || restored.organizationId.isNullOrBlank()) {
            if (restored != null) backend.authSession.clearLocalSession()
            bootstrapping = false
            return@LaunchedEffect
        }
        organizationId = restored.organizationId
        try {
            bindAndSync(restored.organizationId)
            onReady()
        } catch (_: AuthenticationExpiredException) {
            backend.authSession.clearLocalSession()
            bootstrapping = false
        } catch (_: Exception) {
            // A valid cached session may continue offline; queued mutations remain durable.
            onReady()
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
        Button(
            enabled = !submitting && email.isNotBlank() && password.isNotBlank() && organizationId.isNotBlank(),
            onClick = {
                submitting = true
                error = null
                scope.launch {
                    runCatching {
                        val tokens = backend.authSession.login(email, password, organizationId.trim())
                        password = "" // password is never persisted and is removed from UI state immediately after use
                        bindAndSync(requireNotNull(tokens.organizationId))
                    }.onSuccess {
                        onReady()
                    }.onFailure {
                        password = ""
                        error = it.message ?: "Не удалось войти в Lexora Backend"
                        submitting = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        ) {
            if (submitting) CircularProgressIndicator() else Text("Войти")
        }
    }
}
