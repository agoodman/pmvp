package org.pmvp.sqldelight

import com.squareup.sqldelight.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.pmvp.Proxy
import org.pmvp.Storage

interface Queryable<K, T, Q: Any> {
    fun getOne(key: K): Query<Q>

    fun get(keys: List<K>): Query<Q>

    fun all(): Query<Q>

    fun insert(proxy: T)

    fun update(proxy: T)

    fun delete(proxy: T)

    fun fromLocal(localObject: Q): T
}

open class SqlDelightStorage<K, T : Proxy<K>, Q: Any>(
    private val queryable: Queryable<K, T, Q>,
    private val queryToFlow: QueryToFlowInvokable
) : Storage<K, T> {

    override fun objects(): Flow<List<T>> =
        queryToFlow(queryable.all()).map { it.executeAsList().map { queryable.fromLocal(it) } }

    override fun objectFor(key: K): Flow<T?> = queryToFlow(queryable.getOne(key)).map {
        val result = it.executeAsOneOrNull()
        if (result == null) {
            null
        } else {
            queryable.fromLocal(result)
        }
    }

    override fun objectsFor(keys: List<K>): Flow<List<T>> =
        queryToFlow(queryable.get(keys)).map { it.executeAsList().map { queryable.fromLocal(it) } }

    override fun updateObject(proxy: T): Flow<T> = updateObjects(listOf(proxy)).map { it.first() }

    override fun updateObjects(proxies: List<T>): Flow<List<T>> = flow {
        val existing = queryable.get(proxies.map { it.key }).executeAsList()
            .map { queryable.fromLocal(it) }
            .map { it.key to it }
            .toMap()
        val upsertedObjects = proxies.map { it.key to it }.toMap().values
        for (proxy in upsertedObjects) {
            if (existing[proxy.key] == null) {
                queryable.insert(proxy)
            } else {
                queryable.update(proxy)
            }
        }
        emit(proxies)
    }

    override fun destroyObject(proxy: T): Flow<T> = destroyObjects(listOf(proxy)).map { it.first() }

    override fun destroyObjects(objects: List<T>): Flow<List<T>> = flow {
        for (obj in objects) {
            queryable.delete(obj)
        }
        emit(objects)
    }
}
