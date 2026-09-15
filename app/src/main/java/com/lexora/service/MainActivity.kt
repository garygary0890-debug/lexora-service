package com.lexora.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lexora.service.core.data.InMemoryModuleRegistry
import com.lexora.service.core.data.InMemoryOrganizationRepository
import com.lexora.service.core.data.InMemoryUserRepository
import com.lexora.service.core.database.AuditEventEntity
import com.lexora.service.core.database.ClientEntity
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.database.OrganizationEntity
import com.lexora.service.core.database.VehicleEntity
import com.lexora.service.core.designsystem.LexoraTheme
import com.lexora.service.core.domain.ModuleAccessPolicy
import com.lexora.service.core.model.Client
import com.lexora.service.core.model.ClientType
import com.lexora.service.core.model.LexoraModuleId
import com.lexora.service.core.model.SyncState
import com.lexora.service.core.model.Vehicle
import com.lexora.service.core.navigation.Routes
import com.lexora.service.feature.clients.ClientDraft
import com.lexora.service.feature.clients.ClientsScreen
import com.lexora.service.feature.home.HomeScreen
import com.lexora.service.feature.settings.SettingsScreen
import com.lexora.service.feature.vehicles.VehicleDraft
import com.lexora.service.feature.vehicles.VehiclesScreen
import com.lexora.service.feature.tires.TiresScreen
import com.lexora.service.feature.wash.WashScreen
import java.util.UUID
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LexoraServiceApp() }
    }
}

@Composable
private fun LexoraServiceApp() {
    LexoraTheme {
        val context = LocalContext.current
        val database = remember { LexoraServiceDatabase.create(context) }
        val dao = remember(database) { database.serviceDao() }
        val organizationRepository = remember { InMemoryOrganizationRepository() }
        val userRepository = remember { InMemoryUserRepository() }
        val moduleRegistry = remember { InMemoryModuleRegistry() }
        val moduleAccessPolicy = remember { ModuleAccessPolicy() }
        val scope = rememberCoroutineScope()
        var stateVersion by remember { mutableIntStateOf(0) }
        var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
        var archivedClients by remember { mutableStateOf<List<Client>>(emptyList()) }
        var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
        var archivedVehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }

        val organization = requireNotNull(organizationRepository.activeOrganization())
        val user = userRepository.currentUser()
        val modules = moduleRegistry.modules(organization.id)
        val accessibleModules = modules.filter { moduleAccessPolicy.isAvailable(it, user) }

        suspend fun reloadClients() {
            clients = dao.clients(organization.id).map(ClientEntity::toModel)
            archivedClients = dao.archivedClients(organization.id).map(ClientEntity::toModel)
        }

        suspend fun reloadVehicles() {
            vehicles = dao.vehicles(organization.id).map(VehicleEntity::toModel)
            archivedVehicles = dao.archivedVehicles(organization.id).map(VehicleEntity::toModel)
        }

        suspend fun audit(entityType: String, entityId: String?, action: String, summary: String) {
            dao.insertAuditEvent(
                AuditEventEntity(
                    id = UUID.randomUUID().toString(),
                    organizationId = organization.id,
                    userId = user.id,
                    entityType = entityType,
                    entityId = entityId,
                    action = action,
                    summary = summary,
                    occurredAtEpochMs = System.currentTimeMillis(),
                )
            )
        }

        LaunchedEffect(organization.id) {
            val now = System.currentTimeMillis()
            dao.upsertOrganization(
                OrganizationEntity(
                    id = organization.id,
                    name = organization.name,
                    isActive = true,
                    updatedAtEpochMs = now,
                )
            )
            if (dao.clients(organization.id).isEmpty() && dao.archivedClients(organization.id).isEmpty()) {
                dao.upsertClient(
                    ClientEntity(
                        id = "client-demo-1",
                        organizationId = organization.id,
                        type = ClientType.PERSON.name,
                        displayName = "Демонстрационный клиент",
                        phone = "+7 900 000-00-00",
                        email = null,
                        taxId = null,
                        kpp = null,
                        registrationAddress = null,
                        actualAddress = null,
                        note = null,
                        consentPersonalData = false,
                        archived = false,
                        syncState = SyncState.PENDING_CREATE.name,
                        createdAtEpochMs = now,
                        updatedAtEpochMs = now,
                    )
                )
            }
            reloadClients()
            reloadVehicles()
        }

        val navController = rememberNavController()
        Scaffold { _ ->
            NavHost(navController = navController, startDestination = Routes.Home) {
                composable(Routes.Home) {
                    HomeScreen(
                        organization = organization,
                        modules = accessibleModules,
                        onOpenClients = { navController.navigate(Routes.Clients) },
                        onOpenVehicles = { navController.navigate(Routes.Vehicles) },
                        onOpenWash = {
                            if (accessibleModules.any { it.id == LexoraModuleId.WASH }) {
                                navController.navigate(Routes.Wash)
                            }
                        },
                        onOpenTires = {
                            if (accessibleModules.any { it.id == LexoraModuleId.TIRES }) {
                                navController.navigate(Routes.Tires)
                            }
                        },
                        onOpenSettings = { navController.navigate(Routes.Settings) },
                    )
                }
                composable(Routes.Clients) {
                    ClientsScreen(
                        clients = clients,
                        archivedClients = archivedClients,
                        onSave = { draft, existingId ->
                            scope.launch {
                                val now = System.currentTimeMillis()
                                val id = existingId ?: UUID.randomUUID().toString()
                                val existing = existingId?.let { dao.client(it) }
                                dao.upsertClient(
                                    ClientEntity(
                                        id = id,
                                        organizationId = organization.id,
                                        type = draft.type.name,
                                        displayName = draft.displayName,
                                        phone = draft.phone.ifBlank { null },
                                        email = draft.email.ifBlank { null },
                                        taxId = draft.taxId.ifBlank { null },
                                        kpp = draft.kpp.ifBlank { null },
                                        registrationAddress = draft.registrationAddress.ifBlank { null },
                                        actualAddress = draft.actualAddress.ifBlank { null },
                                        note = draft.note.ifBlank { null },
                                        consentPersonalData = draft.consentPersonalData,
                                        archived = existing?.archived ?: false,
                                        syncState = if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name,
                                        createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                                        updatedAtEpochMs = now,
                                    )
                                )
                                audit("CLIENT", id, if (existing == null) "CREATE" else "UPDATE", draft.displayName)
                                reloadClients()
                            }
                        },
                        onArchive = { id ->
                            scope.launch {
                                val name = dao.client(id)?.displayName.orEmpty()
                                dao.archiveClient(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis())
                                audit("CLIENT", id, "ARCHIVE", name)
                                reloadClients()
                            }
                        },
                        onRestore = { id ->
                            scope.launch {
                                val name = dao.client(id)?.displayName.orEmpty()
                                dao.restoreClient(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis())
                                audit("CLIENT", id, "RESTORE", name)
                                reloadClients()
                            }
                        },
                    )
                }
                composable(Routes.Vehicles) {
                    VehiclesScreen(
                        vehicles = vehicles,
                        archivedVehicles = archivedVehicles,
                        clients = clients + archivedClients,
                        onSave = { draft, existingId ->
                            scope.launch {
                                val now = System.currentTimeMillis()
                                val id = existingId ?: UUID.randomUUID().toString()
                                val existing = existingId?.let { dao.vehicle(it) }
                                dao.upsertVehicle(
                                    VehicleEntity(
                                        id = id,
                                        organizationId = organization.id,
                                        clientId = draft.clientId,
                                        registrationNumber = draft.registrationNumber.trim().uppercase(),
                                        vin = draft.vin.trim().uppercase().ifBlank { null },
                                        make = draft.make.trim().ifBlank { null },
                                        model = draft.model.trim().ifBlank { null },
                                        year = draft.year.toIntOrNull(),
                                        bodyType = draft.bodyType.trim().ifBlank { null },
                                        color = draft.color.trim().ifBlank { null },
                                        mileageKm = draft.mileageKm.toIntOrNull(),
                                        archived = existing?.archived ?: false,
                                        syncState = if (existing == null) SyncState.PENDING_CREATE.name else SyncState.PENDING_UPDATE.name,
                                        createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                                        updatedAtEpochMs = now,
                                    )
                                )
                                audit(
                                    entityType = "VEHICLE",
                                    entityId = id,
                                    action = if (existing == null) "CREATE" else "UPDATE",
                                    summary = draft.registrationNumber.trim().uppercase(),
                                )
                                reloadVehicles()
                            }
                        },
                        onArchive = { id ->
                            scope.launch {
                                val registration = dao.vehicle(id)?.registrationNumber.orEmpty()
                                dao.archiveVehicle(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis())
                                audit("VEHICLE", id, "ARCHIVE", registration)
                                reloadVehicles()
                            }
                        },
                        onRestore = { id ->
                            scope.launch {
                                val registration = dao.vehicle(id)?.registrationNumber.orEmpty()
                                dao.restoreVehicle(id, SyncState.PENDING_UPDATE.name, System.currentTimeMillis())
                                audit("VEHICLE", id, "RESTORE", registration)
                                reloadVehicles()
                            }
                        },
                    )
                }
                composable(Routes.Settings) {
                    SettingsScreen(
                        organization = organization,
                        user = user,
                        modules = modules,
                        onModuleEnabledChange = { moduleId, enabled ->
                            moduleRegistry.updateEnabled(organization.id, moduleId, enabled)
                            stateVersion++
                        },
                    )
                }
                composable(Routes.Wash) { WashScreen() }
                composable(Routes.Tires) { TiresScreen() }
            }
        }
        stateVersion
    }
}

private fun ClientEntity.toModel(): Client = Client(
    id = id,
    organizationId = organizationId,
    type = ClientType.valueOf(type),
    displayName = displayName,
    phone = phone,
    email = email,
    taxId = taxId,
    kpp = kpp,
    registrationAddress = registrationAddress,
    actualAddress = actualAddress,
    note = note,
    consentPersonalData = consentPersonalData,
    archived = archived,
    syncState = SyncState.valueOf(syncState),
)

private fun VehicleEntity.toModel(): Vehicle = Vehicle(
    id = id,
    organizationId = organizationId,
    clientId = clientId,
    registrationNumber = registrationNumber,
    vin = vin,
    make = make,
    model = model,
    year = year,
    bodyType = bodyType,
    color = color,
    mileageKm = mileageKm,
    archived = archived,
    syncState = SyncState.valueOf(syncState),
)
