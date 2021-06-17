package org.pmvp

import kotlinx.coroutines.flow.Flow

interface Syncable<K, T : Proxy<K>> {
    fun syncUsing(orchestrator: SyncOrchestrator<K, T>): Flow<Float>
}

interface ForeignKeySyncable<FK, K, T : Proxy<K>> {
    fun syncUsing(foreignKey: FK, orchestrator: ForeignKeySyncOrchestrator<FK, K, T>): Flow<Float>
}
