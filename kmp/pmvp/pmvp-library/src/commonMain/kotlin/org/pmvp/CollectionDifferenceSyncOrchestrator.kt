package org.pmvp

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

@ExperimentalCoroutinesApi
open class CollectionDifferenceSyncOrchestrator<K, T : Proxy<K>>(
    private val unsavedFilter: (T) -> Boolean,
    private val deletedFilter: (T) -> Boolean,
    private val afterPush: (T) -> T,
    private val afterPull: (T) -> T,
    private val dispatcher: CoroutineDispatcher
) : SyncOrchestrator<K, T> {

    lateinit var localStorage: Storage<K, T>

    lateinit var remoteStorage: Storage<K, T>

    private val DEBUG = true

    override fun register(
        localStorage: Storage<K, T>,
        remoteStorage: Storage<K, T>
    ) {
        this.localStorage = localStorage
        this.remoteStorage = remoteStorage
    }

    override fun performSync(): Flow<Float> = flow {
        println("${this@CollectionDifferenceSyncOrchestrator} performSync")
        // start by reporting 0% progress
        emit(0.0f)

        if( DEBUG ) println("${this@CollectionDifferenceSyncOrchestrator} find deleted local records to push")
        // first, push local deletes to remote
        val deletedObjects = localStorage.objects().first().filter(deletedFilter)
        if (!deletedObjects.isEmpty()) {
            if( DEBUG ) println("${this@CollectionDifferenceSyncOrchestrator} push ${deletedObjects.count()} deletes to remote")
            remoteStorage.destroyObjects(deletedObjects).collect()
        }

        if( DEBUG ) println("${this@CollectionDifferenceSyncOrchestrator} find upserted local records to push")
        // next, push local upserts to remote
        val unsavedObjects = localStorage.objects().first().filter(unsavedFilter)
        if (!unsavedObjects.isEmpty()) {
            if( DEBUG ) println("${this@CollectionDifferenceSyncOrchestrator} push ${unsavedObjects.count()} upserts to remote")
            remoteStorage.updateObjects(unsavedObjects)
                .flatMapLatest { localStorage.updateObjects(it.map(afterPush)) }
                .collect()
        } else {
            if( DEBUG ) println("${this@CollectionDifferenceSyncOrchestrator} nothing to push to remote")
        }

        if( DEBUG ) println("${this@CollectionDifferenceSyncOrchestrator} fetch local objects before pull")
        // report 50% progress
        emit(0.5f)

        // read local collection into map
        val localObjects = localStorage.objects().first()
        if( DEBUG ) println("${this@CollectionDifferenceSyncOrchestrator} insert local objects into map")
        val localMap = localObjects.map { it.key to it }.toMap().toMutableMap()

        if( DEBUG ) println("${this@CollectionDifferenceSyncOrchestrator} fetch remote objects")
        // read from remote
        val remoteObjects = remoteStorage.objects().first()
        if( DEBUG ) println("${this@CollectionDifferenceSyncOrchestrator} received ${remoteObjects.count()} objects")

        // upsert remote objects first
        localStorage.updateObjects(remoteObjects.map(afterPull)).collect()
        if( DEBUG ) println("${this@CollectionDifferenceSyncOrchestrator} upserted objects into local store")

        // find local objects no longer in the remote set
        for (obj in remoteObjects) {
            localMap.remove(obj.key)
        }
        val objectsPendingDelete = localMap.values.toList()
        if (!objectsPendingDelete.isEmpty()) {
            localStorage.destroyObjects(objectsPendingDelete).collect()
        } else {
            if( DEBUG ) println("${this@CollectionDifferenceSyncOrchestrator} nothing to delete from local")
        }

        // report 100% progress
        emit(1.0f)
        println("${this@CollectionDifferenceSyncOrchestrator} synced")
    }.flowOn(dispatcher)
}