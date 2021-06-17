package org.pmvp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A dynamic in-memory wrapper around the [Storage<K, T>] interface.
 */
open class InMemoryStorage<K, T>(
    private val map: MutableMap<K, T> = emptyMap<K, T>().toMutableMap()
) : Storage<K, T> where T: Proxy<K> {

    override fun objects(): Flow<List<T>> = flow {
        emit(map.values.toList())
    }

    override fun objectFor(key: K): Flow<T?> = flow {
        emit(map[key])
    }

    override fun objectsFor(keys: List<K>): Flow<List<T>> = flow {
        emit(keys.map { it to map[it] }.toMap().values.filterNotNull().toList())
    }

    override fun updateObject(proxy: T): Flow<T> = flow {
        map.put(proxy.key, proxy)
        emit(proxy)
    }

    override fun updateObjects(proxies: List<T>): Flow<List<T>> = flow {
        map.putAll(proxies.map { it.key to it })
        emit(proxies)
    }

    override fun destroyObject(proxy: T): Flow<T> = flow {
        map.remove(proxy.key)
        emit(proxy)
    }

    override fun destroyObjects(objects: List<T>): Flow<List<T>> = flow {
        for (obj in objects) {
            map.remove(obj.key)
        }
        emit(objects)
    }

}
