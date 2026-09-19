package com.lexora.service.core.model

data class SaveClientCommand(
    val type: ClientType,
    val displayName: String,
    val phone: String,
    val email: String,
    val taxId: String,
    val kpp: String,
    val registrationAddress: String,
    val actualAddress: String,
    val note: String,
    val consentPersonalData: Boolean,
)

data class SaveVehicleCommand(
    val clientId: String? = null,
    val registrationNumber: String = "",
    val vin: String = "",
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val bodyType: String = "",
    val color: String = "",
    val mileageKm: String = "",
)

data class SaveServiceObjectCommand(
    val clientId: String? = null,
    val name: String = "",
    val address: String = "",
    val accessMode: String = "",
    val responsibleContact: String = "",
)

data class SaveEquipmentCommand(
    val serviceObjectId: String? = null,
    val type: String = "",
    val make: String = "",
    val model: String = "",
    val serialNumber: String = "",
    val inventoryNumber: String = "",
    val barcode: String = "",
    val commissionedNote: String = "",
    val warrantyNote: String = "",
)

data class SaveBranchCommand(
    val name: String,
    val address: String,
    val phone: String,
    val email: String,
    val workSchedule: String,
    val timeZoneId: String,
)

data class SaveEmployeeCommand(
    val displayName: String,
    val position: String,
    val phone: String,
    val email: String,
    val branchId: String?,
)

data class SaveRequestCommand(
    val title: String,
    val description: String,
    val priority: RequestPriority,
    val clientId: String?,
    val serviceObjectId: String?,
    val equipmentId: String?,
    val contractId: String?,
    val branchId: String?,
    val assigneeEmployeeId: String?,
    val assigneeTeamName: String?,
    val plannedAtEpochMs: Long?,
    val dueAtEpochMs: Long?,
    val slaDeadlineEpochMs: Long?,
)
