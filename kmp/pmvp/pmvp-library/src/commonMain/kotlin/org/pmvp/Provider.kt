package org.pmvp

import kotlinx.coroutines.flow.Flow

/**
 * Provider classes are the lowest level of infrastructure within the PMVP architecture.
 *
 * Providers rely on strongly typed objects (T) and a primary key (K) for each object to enable coordination of
 * object management in generic boilerplate code.
 *
 * Provider<K, T> is the work horse of the framework. At its core, a provider manages objects of a given type,
 * delegating to subcomponents to achieve various concerns, such as local/remote persistence and sync orchestration.
 *
 * Providers rely on channel holder classes to coordinate pub/sub streams used to inform external components of
 * changes to objects and collections of objects. External components subscribe to these streams as Kotlin Flow<T>.
 *
 * Since all traffic for a given type is coordinated through the Provider, clients can be guaranteed their requests
 * have transactional atomicity. Moreover, coroutines enable the dispatch of database i/o off the main thread.
 */
open class Provider<K, T : Proxy<K>>(
    open val localStorage: Storage<K, T>,
    open val remoteStorage: Storage<K, T>,
    open val beforeFilter: ProxyFilter<K, T> = EmptyProxyFilter()
    ) : Syncable<K, T>, Mutable<K, T>, Immutable<K, T> {

    protected fun objectFor(key: K): Flow<T?> = localStorage.objectFor(key)

    protected fun objectsFor(keys: List<K>): Flow<List<T>> = localStorage.objectsFor(keys)

    protected fun objects(): Flow<List<T>> = localStorage.objects()

    protected fun updateObject(proxy: T): Flow<T> = localStorage.updateObject(beforeFilter.filter(proxy))

    protected fun updateObjects(proxies: List<T>): Flow<List<T>> = localStorage.updateObjects(proxies)

    protected fun destroyObject(proxy: T): Flow<T> = localStorage.destroyObject(proxy)

    protected fun destroyObjects(proxies: List<T>): Flow<List<T>> = localStorage.destroyObjects(proxies)

    override fun syncUsing(orchestrator: SyncOrchestrator<K, T>): Flow<Float> {
        orchestrator.register(this@Provider.localStorage, this@Provider.remoteStorage)
        return orchestrator.performSync()
    }

    override fun model(key: K): Flow<T?> = objectFor(key)

    override fun models(keys: List<K>): Flow<List<T>> = objectsFor(keys)

    override fun upsert(model: T): Flow<T> = updateObject(model)

    override fun delete(model: T): Flow<T> = destroyObject(model)

}
