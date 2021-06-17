package org.pmvp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.pmvp.storage.StaticStorage

/**
 * Ephemeral providers allow their standard flow API to be used to encapsulate remote data, without caching it locally.
 *
 */
open class EphemeralProvider<K, T: Proxy<K>>(
    storage: Storage<K, T>
) : Provider<K, T>(
    localStorage = storage,
    remoteStorage = StaticStorage<K, T>(emptyMap())
) {

    override fun upsert(model: T): Flow<T> = flowOf(model)

    override fun delete(model: T): Flow<T> = flowOf(model)

    /*
     * Ephemeral providers do not sync
     */
    final override fun syncUsing(orchestrator: SyncOrchestrator<K, T>): Flow<Float> =
        flowOf(1.0f)

}
