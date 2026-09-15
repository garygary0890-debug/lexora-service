package com.lexora.service.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "organizations") data class OrganizationEntity(@PrimaryKey val id: String, val name: String, val isActive: Boolean, val updatedAtEpochMs: Long)
@Entity(tableName = "clients", indices = [Index("organizationId"), Index(value = ["organizationId", "phone"])]) data class ClientEntity(@PrimaryKey val id: String, val organizationId: String, val type: String, val displayName: String, val phone: String?, val email: String?, val taxId: String?, val kpp: String?, val registrationAddress: String?, val actualAddress: String?, val note: String?, val consentPersonalData: Boolean, val archived: Boolean, val syncState: String, val createdAtEpochMs: Long, val updatedAtEpochMs: Long)
@Entity(tableName = "vehicles", indices = [Index("organizationId"), Index("clientId"), Index(value = ["organizationId", "registrationNumber"])]) data class VehicleEntity(@PrimaryKey val id: String, val organizationId: String, val clientId: String?, val registrationNumber: String, val vin: String?, val make: String?, val model: String?, val year: Int?, val bodyType: String?, val color: String?, val mileageKm: Int?, val archived: Boolean, val syncState: String, val createdAtEpochMs: Long, val updatedAtEpochMs: Long)
@Entity(tableName = "branches", indices = [Index("organizationId")]) data class BranchEntity(@PrimaryKey val id: String, val organizationId: String, val name: String, val address: String?, val phone: String?, val email: String?, val workSchedule: String?, val timeZoneId: String, val active: Boolean, val syncState: String, val updatedAtEpochMs: Long)
@Entity(tableName = "employees", indices = [Index("organizationId"), Index("branchId")]) data class EmployeeEntity(@PrimaryKey val id: String, val organizationId: String, val branchId: String?, val displayName: String, val position: String?, val phone: String?, val email: String?, val active: Boolean, val syncState: String, val updatedAtEpochMs: Long)
@Entity(tableName = "module_settings", primaryKeys = ["organizationId", "moduleId"]) data class ModuleSettingEntity(val organizationId: String, val moduleId: String, val enabled: Boolean, val licenseStatus: String, val updatedAtEpochMs: Long)
@Entity(tableName = "audit_events", indices = [Index("organizationId"), Index("entityId"), Index("occurredAtEpochMs")]) data class AuditEventEntity(@PrimaryKey val id: String, val organizationId: String, val userId: String, val entityType: String, val entityId: String?, val action: String, val summary: String, val occurredAtEpochMs: Long)
@Entity(tableName = "service_history", indices = [Index("organizationId"), Index("vehicleId"), Index("occurredAtEpochMs")]) data class ServiceHistoryEntity(@PrimaryKey val id: String, val organizationId: String, val vehicleId: String, val sourceType: String, val sourceId: String?, val title: String, val description: String?, val mileageKm: Int?, val occurredAtEpochMs: Long, val createdAtEpochMs: Long)
@Entity(tableName = "service_objects", indices = [Index("organizationId"), Index("clientId")]) data class ServiceObjectEntity(@PrimaryKey val id: String, val organizationId: String, val clientId: String?, val name: String, val address: String?, val accessMode: String?, val responsibleContact: String?, val archived: Boolean, val syncState: String, val createdAtEpochMs: Long, val updatedAtEpochMs: Long)
@Entity(tableName = "equipment", indices = [Index("organizationId"), Index("serviceObjectId"), Index(value = ["organizationId", "serialNumber"])]) data class EquipmentEntity(@PrimaryKey val id: String, val organizationId: String, val serviceObjectId: String?, val type: String, val make: String?, val model: String?, val serialNumber: String?, val inventoryNumber: String?, val barcode: String?, val commissionedNote: String?, val warrantyNote: String?, val archived: Boolean, val syncState: String, val createdAtEpochMs: Long, val updatedAtEpochMs: Long)

@Entity(tableName = "service_requests", indices = [Index("organizationId"), Index("clientId"), Index("branchId"), Index("assigneeEmployeeId"), Index("status"), Index(value = ["organizationId", "number"], unique = true)])
data class ServiceRequestEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val number: String,
    val clientId: String?,
    val vehicleId: String?,
    val serviceObjectId: String?,
    val equipmentId: String?,
    val branchId: String?,
    val assigneeEmployeeId: String?,
    val title: String,
    val description: String?,
    val status: String,
    val priority: String,
    val plannedAtEpochMs: Long?,
    val dueAtEpochMs: Long?,
    val slaDeadlineEpochMs: Long?,
    val closedAtEpochMs: Long?,
    val archived: Boolean,
    val syncState: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Entity(tableName = "request_status_history", indices = [Index("requestId"), Index("changedAtEpochMs")])
data class RequestStatusHistoryEntity(
    @PrimaryKey val id: String,
    val requestId: String,
    val fromStatus: String?,
    val toStatus: String,
    val changedByUserId: String,
    val changedAtEpochMs: Long,
    val comment: String?,
)
