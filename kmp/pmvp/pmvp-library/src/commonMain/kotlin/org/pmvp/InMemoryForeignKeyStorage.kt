package org.pmvp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.pmvp.Proxy

/**
 * A dynamic in-memory wrapper around the [Storage<K, T>] interface.
 * Primarily used for testing. This is a storage class that uses a Map to map foreign keys to a map of keys to
 */
open class InMemoryForeignKeyStorage<FK, K, T>(
    private val map: MutableMap<FK, MutableMap<K, T>> = emptyMap<FK, MutableMap<K, T>>().toMutableMap()
) : ForeignKeyStorage<FK, K, T> where T: Proxy<K> {

    fun count(foreignKey: FK): Int {
        val submap = map[foreignKey] ?: emptyMap<K, T>()
        return submap.count()
    }

    override fun objects(foreignKey: FK): Flow<List<T>> = flow {
        val submap: MutableMap<K, T> = map[foreignKey] ?: emptyMap<K, T>().toMutableMap()
        emit(submap.values.toList())
    }

    override fun objectFor(foreignKey: FK, key: K): Flow<T?> = flow {
        val submap: MutableMap<K, T> = map[foreignKey] ?: emptyMap<K, T>().toMutableMap()
        emit(submap[key])
    }

    override fun objectsFor(foreignKey: FK, keys: List<K>): Flow<List<T>> = flow {
        val submap: MutableMap<K, T> = map[foreignKey] ?: emptyMap<K, T>().toMutableMap()
        emit(keys.map { it to submap[it] }.toMap().values.filterNotNull().toList())
    }

    override fun updateObject(foreignKey: FK, proxy: T): Flow<T> = flow {
        val submap: MutableMap<K, T> = map[foreignKey] ?: emptyMap<K, T>().toMutableMap()
        submap.put(proxy.key, proxy)
        map.put(foreignKey, submap)
        emit(proxy)
    }

    override fun updateObjects(foreignKey: FK, proxies: List<T>): Flow<List<T>> = flow {
        val submap: MutableMap<K, T> = map[foreignKey] ?: emptyMap<K, T>().toMutableMap()
        submap.putAll(proxies.map { it.key to it })
        map.put(foreignKey, submap)
        emit(proxies)
    }

    override fun destroyObject(foreignKey: FK, proxy: T): Flow<T> = flow {
        val submap: MutableMap<K, T> = map[foreignKey] ?: emptyMap<K, T>().toMutableMap()
        submap.remove(proxy.key)
        map.put(foreignKey, submap)
        emit(proxy)
    }

    override fun destroyObjects(foreignKey: FK, objects: List<T>): Flow<List<T>> = flow {
        val submap: MutableMap<K, T> = map[foreignKey] ?: emptyMap<K, T>().toMutableMap()
        for (obj in objects) {
            submap.remove(obj.key)
        }
        map.put(foreignKey, submap)
        emit(objects)
    }

}
