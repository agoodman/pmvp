package org.pmvp

import kotlinx.coroutines.flow.Flow

open class ForeignKeyProvider<FK, K, T: Proxy<K>>(
    private val localStorage: ForeignKeyStorage<FK, K, T>,
    private val remoteStorage: ForeignKeyStorage<FK, K, T>,
    private val beforeFilter: ProxyFilter<K, T>
) : ForeignKeyStorage<FK, K, T>,
    ForeignKeySyncable<FK, K, T> {

    override fun objects(foreignKey: FK): Flow<List<T>> = localStorage.objects(foreignKey)

    override fun objectFor(foreignKey: FK, key: K): Flow<T?> =
        localStorage.objectFor(foreignKey, key)

    override fun objectsFor(foreignKey: FK, keys: List<K>): Flow<List<T>> =
        localStorage.objectsFor(foreignKey, keys)

    override fun updateObject(foreignKey: FK, proxy: T): Flow<T> =
        localStorage.updateObject(foreignKey, beforeFilter.filter(proxy))

    override fun updateObjects(foreignKey: FK, proxies: List<T>): Flow<List<T>> =
        localStorage.updateObjects(foreignKey, proxies.map { beforeFilter.filter(it) })

    override fun destroyObject(foreignKey: FK, proxy: T): Flow<T> =
        localStorage.destroyObject(foreignKey, proxy)

    override fun destroyObjects(foreignKey: FK, objects: List<T>): Flow<List<T>> =
        localStorage.destroyObjects(foreignKey, objects)

    override fun syncUsing(
        foreignKey: FK,
        orchestrator: ForeignKeySyncOrchestrator<FK, K, T>
    ): Flow<Float> {
        orchestrator.register(localStorage, remoteStorage)
        return orchestrator.performSync(foreignKey = foreignKey)
    }
}