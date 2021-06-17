package org.pmvp.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.pmvp.Proxy
import org.pmvp.Storage

/**
 * A static wrapper around the [Storage<K, T>] interface.
 */
open class StaticStorage<K, T>(
    private val map: Map<K, T>
) : Storage<K, T> where T: Proxy<K> {

    override fun objects(): Flow<List<T>> =
        flowOf(map.values.toList())

    override fun objectFor(key: K): Flow<T?> =
        flowOf(map[key])

    override fun objectsFor(keys: List<K>): Flow<List<T>> =
        flowOf(keys.map { it to map[it] }.toMap().values.filterNotNull().toList())

    override fun updateObject(proxy: T): Flow<T> =
        flowOf(proxy)

    override fun updateObjects(proxies: List<T>): Flow<List<T>> =
        flowOf(proxies)

    override fun destroyObject(proxy: T): Flow<T> =
        flowOf(proxy)

    override fun destroyObjects(objects: List<T>): Flow<List<T>> =
        flowOf(objects)

}
