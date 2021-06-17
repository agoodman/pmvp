package org.pmvp

import kotlinx.coroutines.flow.Flow

interface ForeignKeyStorage<FK, K, T> where T : Proxy<K> {

    fun objects(foreignKey: FK): Flow<List<T>>

    fun objectFor(foreignKey: FK, key: K): Flow<T?>

    fun objectsFor(foreignKey: FK, keys: List<K>): Flow<List<T>>

    fun updateObject(foreignKey: FK, proxy: T): Flow<T>

    fun updateObjects(foreignKey: FK, proxies: List<T>): Flow<List<T>>

    fun destroyObject(foreignKey: FK, proxy: T): Flow<T>

    fun destroyObjects(foreignKey: FK, objects: List<T>): Flow<List<T>>
}