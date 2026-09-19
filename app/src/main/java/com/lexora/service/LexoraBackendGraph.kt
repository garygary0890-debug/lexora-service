package com.lexora.service

import android.content.Context
import com.lexora.service.core.data.BidirectionalSyncEngine
import com.lexora.service.core.data.LexoraServiceMutationMapper
import com.lexora.service.core.data.LexoraServiceRemoteChangeApplier
import com.lexora.service.core.data.ServiceSyncMetadataStore
import com.lexora.service.core.data.SyncQueueRepository
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.model.DeploymentEnvironment
import com.lexora.service.core.network.AndroidKeystoreTokenProvider
import com.lexora.service.core.network.ApiConfiguration
import com.lexora.service.core.network.AuthSessionManager
import com.lexora.service.core.network.LexoraAuthApi
import com.lexora.service.core.network.LexoraBackendSyncApi
import com.lexora.service.core.network.UrlConnectionHttpTransport
import com.lexora.service.core.network.VersionedApiClient

class LexoraBackendGraph(
    context: Context,
    database: LexoraServiceDatabase,
) {
    val configuration = ApiConfiguration(
        baseUrl = BuildConfig.LEXORA_BACKEND_BASE_URL,
        apiVersion = "v1",
        environment = DeploymentEnvironment.PRODUCTION,
    )
    val transport = UrlConnectionHttpTransport(configuration)
    val tokenProvider = AndroidKeystoreTokenProvider(context)
    val authApi = LexoraAuthApi(configuration, transport)
    val authSession = AuthSessionManager(authApi, tokenProvider)
    val apiClient = VersionedApiClient(configuration, transport, tokenProvider, authApi)
    val syncApi = LexoraBackendSyncApi(apiClient)
    val syncMetadata = ServiceSyncMetadataStore(context)
    val syncQueue = SyncQueueRepository(database.serviceDao())
    val syncEngine = BidirectionalSyncEngine(
        queue = syncQueue,
        api = syncApi,
        cursorStore = syncMetadata,
        remoteChangeApplier = LexoraServiceRemoteChangeApplier(database.serviceDao()),
        mutationMapper = LexoraServiceMutationMapper(syncMetadata),
    )
}
