package org.pmvp.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.pmvp.ForeignKeyStorage
import org.pmvp.Proxy

/**
 * A static wrapper around the [ForeignKeyStorage<FK, K, T>] interface, with support for a foreign key relationship.
 *
 */
open class StaticForeignKeyStorage<FK, K, T>(
    private val map: Map<FK, Map<K, T>>
) : ForeignKeyStorage<FK, K, T> where T: Proxy<K> {

    override fun objects(foreignKey: FK): Flow<List<T>> =
        flowOf(map[foreignKey]?.values?.toList() ?: emptyList())

    override fun objectFor(foreignKey: FK, key: K): Flow<T?> =
        flowOf(map[foreignKey]?.get(key))

    override fun objectsFor(foreignKey: FK, keys: List<K>): Flow<List<T>> = flow {
        val submap: Map<K, T> = map[foreignKey] ?: emptyMap<K, T>()
        emit(
            submap.keys.map { it to submap[it] }
                .toMap()
                .values
                .filterNotNull()
        )
    }

    override fun updateObject(foreignKey: FK, proxy: T): Flow<T> =
        flowOf(proxy)

    override fun updateObjects(foreignKey: FK, proxies: List<T>): Flow<List<T>> =
        flowOf(proxies)

    override fun destroyObject(foreignKey: FK, proxy: T): Flow<T> =
        flowOf(proxy)

    override fun destroyObjects(foreignKey: FK, objects: List<T>): Flow<List<T>> =
        flowOf(objects)

}
