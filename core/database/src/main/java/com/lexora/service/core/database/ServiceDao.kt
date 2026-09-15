package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ServiceDao {
    @Query("SELECT * FROM organizations ORDER BY name")
    suspend fun organizations(): List<OrganizationEntity>

    @Query("SELECT * FROM organizations WHERE isActive = 1 LIMIT 1")
    suspend fun activeOrganization(): OrganizationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrganization(value: OrganizationEntity)

    @Query("UPDATE organizations SET isActive = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setActiveOrganization(id: String)

    @Query("SELECT * FROM clients WHERE organizationId = :organizationId AND archived = 0 ORDER BY displayName")
    suspend fun clients(organizationId: String): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE organizationId = :organizationId AND archived = 1 ORDER BY displayName")
    suspend fun archivedClients(organizationId: String): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun client(id: String): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClient(value: ClientEntity)

    @Query("UPDATE clients SET archived = 1, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun archiveClient(id: String, syncState: String, updatedAt: Long)

    @Query("UPDATE clients SET archived = 0, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun restoreClient(id: String, syncState: String, updatedAt: Long)

    @Query("SELECT * FROM vehicles WHERE organizationId = :organizationId AND archived = 0 ORDER BY registrationNumber")
    suspend fun vehicles(organizationId: String): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE organizationId = :organizationId AND archived = 1 ORDER BY registrationNumber")
    suspend fun archivedVehicles(organizationId: String): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun vehicle(id: String): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVehicle(value: VehicleEntity)

    @Query("UPDATE vehicles SET archived = 1, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun archiveVehicle(id: String, syncState: String, updatedAt: Long)

    @Query("UPDATE vehicles SET archived = 0, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun restoreVehicle(id: String, syncState: String, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceHistory(value: ServiceHistoryEntity)

    @Query("SELECT * FROM service_history WHERE vehicleId = :vehicleId ORDER BY occurredAtEpochMs DESC")
    suspend fun serviceHistory(vehicleId: String): List<ServiceHistoryEntity>

    @Query("SELECT * FROM branches WHERE organizationId = :organizationId ORDER BY name")
    suspend fun branches(organizationId: String): List<BranchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBranch(value: BranchEntity)

    @Query("SELECT * FROM employees WHERE organizationId = :organizationId ORDER BY displayName")
    suspend fun employees(organizationId: String): List<EmployeeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEmployee(value: EmployeeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertModuleSetting(value: ModuleSettingEntity)

    @Query("SELECT * FROM module_settings WHERE organizationId = :organizationId ORDER BY moduleId")
    suspend fun moduleSettings(organizationId: String): List<ModuleSettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditEvent(value: AuditEventEntity)

    @Query("SELECT * FROM audit_events WHERE organizationId = :organizationId ORDER BY occurredAtEpochMs DESC LIMIT :limit")
    suspend fun recentAuditEvents(organizationId: String, limit: Int = 100): List<AuditEventEntity>

    @Query("SELECT * FROM service_objects WHERE organizationId = :organizationId AND archived = 0 ORDER BY name")
    suspend fun serviceObjects(organizationId: String): List<ServiceObjectEntity>

    @Query("SELECT * FROM service_objects WHERE organizationId = :organizationId AND archived = 1 ORDER BY name")
    suspend fun archivedServiceObjects(organizationId: String): List<ServiceObjectEntity>

    @Query("SELECT * FROM service_objects WHERE id = :id LIMIT 1")
    suspend fun serviceObject(id: String): ServiceObjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertServiceObject(value: ServiceObjectEntity)

    @Query("UPDATE service_objects SET archived = 1, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun archiveServiceObject(id: String, syncState: String, updatedAt: Long)

    @Query("UPDATE service_objects SET archived = 0, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun restoreServiceObject(id: String, syncState: String, updatedAt: Long)

    @Query("SELECT * FROM equipment WHERE organizationId = :organizationId AND archived = 0 ORDER BY type, make, model")
    suspend fun equipment(organizationId: String): List<EquipmentEntity>

    @Query("SELECT * FROM equipment WHERE organizationId = :organizationId AND archived = 1 ORDER BY type, make, model")
    suspend fun archivedEquipment(organizationId: String): List<EquipmentEntity>

    @Query("SELECT * FROM equipment WHERE id = :id LIMIT 1")
    suspend fun equipmentItem(id: String): EquipmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEquipment(value: EquipmentEntity)

    @Query("UPDATE equipment SET archived = 1, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun archiveEquipment(id: String, syncState: String, updatedAt: Long)

    @Query("UPDATE equipment SET archived = 0, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun restoreEquipment(id: String, syncState: String, updatedAt: Long)
}
