package com.lexora.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.network.AuthenticationExpiredException

class ServiceSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val database = LexoraServiceDatabase.create(applicationContext)
        val graph = LexoraBackendGraph(applicationContext, database)
        val session = graph.authSession.restore() ?: return Result.success()
        val organizationId = session.organizationId?.takeIf { it.isNotBlank() }
            ?: database.serviceDao().activeOrganization()?.id
            ?: return Result.success()

        graph.syncQueue.recoverStaleInProgress(organizationId)
        return try {
            graph.syncEngine.runOnce(organizationId)
            graph.syncQueue.nextRetryDelayMs(organizationId)?.let { delay ->
                ServiceSyncScheduler.enqueue(applicationContext, delay)
            }
            Result.success()
        } catch (_: AuthenticationExpiredException) {
            graph.authSession.clearLocalSession()
            Result.success()
        } catch (_: Exception) {
            graph.syncQueue.nextRetryDelayMs(organizationId)?.let { delay ->
                ServiceSyncScheduler.enqueue(applicationContext, delay)
            }
            Result.retry()
        }
    }
}
