package org.pmvp.sqldelight

import com.squareup.sqldelight.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.pmvp.ForeignKeyStorage
import org.pmvp.Proxy

interface ForeignKeyQueryable<FK, K, T, Q: Any> {
    fun getOne(foreignKey: FK, key: K): Query<Q>

    fun get(foreignKey: FK, keys: List<K>): Query<Q>

    fun all(foreignKey: FK): Query<Q>

    fun insert(foreignKey: FK, proxy: T)

    fun update(foreignKey: FK, proxy: T)

    fun delete(foreignKey: FK, proxy: T)

    fun fromLocal(localObject: Q): T
}

open class SqlDelightForeignKeyStorage<FK, K, T : Proxy<K>, Q : Any>(
    private val queryable: ForeignKeyQueryable<FK, K, T, Q>,
    private val queryToFlow: QueryToFlowInvokable
) :
    ForeignKeyStorage<FK, K, T> {

    override fun objects(foreignKey: FK): Flow<List<T>> =
        queryToFlow(queryable.all(foreignKey)).map { it.executeAsList().map { queryable.fromLocal(it) } }

    override fun objectFor(foreignKey: FK, key: K): Flow<T?> =
        queryToFlow(queryable.getOne(foreignKey, key)).map {
            val result = it.executeAsOneOrNull()
            if (result == null) {
                null
            } else {
                queryable.fromLocal(result)
            }
        }

    override fun objectsFor(foreignKey: FK, keys: List<K>): Flow<List<T>> =
        queryToFlow(queryable.get(foreignKey, keys)).map { it.executeAsList().map { queryable.fromLocal(it) } }

    override fun updateObject(foreignKey: FK, proxy: T): Flow<T> =
        updateObjects(foreignKey, listOf(proxy)).map { it.first() }

    override fun updateObjects(foreignKey: FK, proxies: List<T>): Flow<List<T>> = flow {
        val existing = queryable.get(foreignKey, proxies.map { it.key }).executeAsList()
            .map { queryable.fromLocal(it) }
            .map { it.key to it }
            .toMap()
        val upsertedObjects = proxies.map { it.key to it }.toMap().values
        for (proxy in upsertedObjects) {
            if (existing[proxy.key] == null) {
                queryable.insert(foreignKey, proxy)
            } else {
                queryable.update(foreignKey, proxy)
            }
        }
        emit(proxies)
    }

    override fun destroyObject(foreignKey: FK, proxy: T): Flow<T> =
        destroyObjects(foreignKey, listOf(proxy)).map { it.first() }

    override fun destroyObjects(foreignKey: FK, objects: List<T>): Flow<List<T>> = flow {
        for (obj in objects) {
            queryable.delete(foreignKey, obj)
        }
        emit(objects)
    }
}
