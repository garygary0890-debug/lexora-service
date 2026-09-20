package com.lexora.service.core.domain

import com.lexora.service.core.model.ExtendedServiceContract
import com.lexora.service.core.model.LinkedServiceTask
import com.lexora.service.core.model.RequestPriority
import com.lexora.service.core.model.ServiceContractStatus
import com.lexora.service.core.model.ServiceNotificationPriority
import com.lexora.service.core.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRuleEngineTest {
    @Test
    fun `open task reminder becomes due and completed task closes it`() {
        val task = LinkedServiceTask(
            id = "task-1", organizationId = "org", title = "Call client",
            status = TaskStatus.OPEN, priority = RequestPriority.HIGH,
            reminderAtEpochMs = 10_000L, createdAtEpochMs = 1_000L, updatedAtEpochMs = 1_000L,
        )
        val due = NotificationRuleEngine.taskReminder(task, 10_000L)
        assertEquals("task-reminder:task-1", due?.id)
        assertEquals(ServiceNotificationPriority.WARNING, due?.priority)
        assertNull(NotificationRuleEngine.taskReminder(task.copy(status = TaskStatus.DONE), 10_000L))
    }

    @Test
    fun `active contract warns before expiry and escalates after expiry`() {
        val contract = ExtendedServiceContract(
            id = "contract-1", organizationId = "org", counterpartyClientId = "client",
            number = "C-1", validFromEpochMs = 0L, validToEpochMs = 100_000L,
            status = ServiceContractStatus.ACTIVE,
        )
        val warning = NotificationRuleEngine.contractExpiry(contract, 90_000L, 20_000L)
        assertEquals(ServiceNotificationPriority.WARNING, warning?.priority)
        val expired = NotificationRuleEngine.contractExpiry(contract, 100_001L, 20_000L)
        assertEquals(ServiceNotificationPriority.CRITICAL, expired?.priority)
        assertTrue(expired?.title?.contains("C-1") == true)
        assertNull(NotificationRuleEngine.contractExpiry(contract.copy(status = ServiceContractStatus.TERMINATED), 90_000L, 20_000L))
    }

    @Test
    fun `stored tire set warns before pickup and closes after release`() {
        val warning = NotificationRuleEngine.tireStorage(
            storageId = "storage-1", storageCode = "T-001", active = true,
            expectedEndEpochMs = 100_000L, reminderLeadMs = 20_000L, nowEpochMs = 90_000L,
        )
        assertEquals("tire-storage:storage-1", warning?.id)
        assertEquals(ServiceNotificationPriority.WARNING, warning?.priority)
        val overdue = NotificationRuleEngine.tireStorage("storage-1", "T-001", true, 100_000L, 20_000L, 100_001L)
        assertEquals(ServiceNotificationPriority.CRITICAL, overdue?.priority)
        assertNull(NotificationRuleEngine.tireStorage("storage-1", "T-001", false, 100_000L, 20_000L, 90_000L))
    }
}
