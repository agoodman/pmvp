package org.pmvp

import kotlinx.coroutines.flow.Flow

interface SyncOrchestrator<K, T : Proxy<K>> {
    fun register(localStorage: Storage<K, T>, remoteStorage: Storage<K, T>)
    fun performSync(): Flow<Float>
}

interface ForeignKeySyncOrchestrator<FK, K, T : Proxy<K>> {
    fun register(localStorage: ForeignKeyStorage<FK, K, T>, remoteStorage: ForeignKeyStorage<FK, K, T>)
    fun performSync(foreignKey: FK): Flow<Float>
}
