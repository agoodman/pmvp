package org.pmvp

import kotlinx.coroutines.flow.Flow

interface Proxy<K> {
    val key: K
}

interface Immutable<K, T: Proxy<K>> {
    fun model(key: K): Flow<T?>
    fun models(keys: List<K>): Flow<List<T>>
}

interface Mutable<K, T: Proxy<K>> {
    fun upsert(model: T): Flow<T>
    fun delete(model: T): Flow<T>
}
