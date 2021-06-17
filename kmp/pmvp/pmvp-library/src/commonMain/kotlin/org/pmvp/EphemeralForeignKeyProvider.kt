package org.pmvp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.pmvp.storage.StaticForeignKeyStorage

/**
 * Ephemeral providers allow their standard flow API to be used to encapsulate remote data, without caching it locally.
 *
 * This one adds the foreign key relationship to its methods.
 *
 */
open class EphemeralForeignKeyProvider<FK, K, T: Proxy<K>>(
    storage: ForeignKeyStorage<FK, K, T>
) : ForeignKeyProvider<FK, K, T>(
    localStorage = storage,
    remoteStorage = StaticForeignKeyStorage<FK, K, T>(emptyMap()),
    beforeFilter = EmptyProxyFilter<K, T>()
) {

    /*
     * Ephemeral providers do not sync
     */
    final override fun syncUsing(foreignKey: FK, orchestrator: ForeignKeySyncOrchestrator<FK, K, T>): Flow<Float> =
        flowOf(1.0f)

}
