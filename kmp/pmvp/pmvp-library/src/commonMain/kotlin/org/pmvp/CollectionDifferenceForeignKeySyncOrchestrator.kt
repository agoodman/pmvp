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
abstract class CollectionDifferenceForeignKeySyncOrchestrator<FK, K, T : Proxy<K>>(
        private val unsavedFilter: (T) -> Boolean,
        private val deletedFilter: (T) -> Boolean,
        private val afterPush: (T) -> T,
        private val afterPull: (T) -> T,
        private val dispatcher: CoroutineDispatcher,
        private val DEBUG: Boolean = false
) : ForeignKeySyncOrchestrator<FK, K, T> {

    lateinit var localStorage: ForeignKeyStorage<FK, K, T>

    lateinit var remoteStorage: ForeignKeyStorage<FK, K, T>

    override fun register(
        localStorage: ForeignKeyStorage<FK, K, T>,
        remoteStorage: ForeignKeyStorage<FK, K, T>
    ) {
        this.localStorage = localStorage
        this.remoteStorage = remoteStorage
    }

    override fun performSync(foreignKey: FK): Flow<Float> = flow {
        println("${this@CollectionDifferenceForeignKeySyncOrchestrator} performSync")
        // start by reporting 0% progress
        emit(0.0f)

        if( DEBUG ) println("${this@CollectionDifferenceForeignKeySyncOrchestrator} find deleted local records to push")
        // first, push local deletes to remote
        val deletedObjects = localStorage.objects(foreignKey).first().filter(deletedFilter)
        if (!deletedObjects.isEmpty()) {
            if( DEBUG ) println("${this@CollectionDifferenceForeignKeySyncOrchestrator} push ${deletedObjects.count()} deletes to remote")
            remoteStorage.destroyObjects(foreignKey, deletedObjects).collect()
        } else {
            if( DEBUG ) println("${this@CollectionDifferenceForeignKeySyncOrchestrator} nothing to delete")
        }

        if( DEBUG ) println("${this@CollectionDifferenceForeignKeySyncOrchestrator} find upserted local records to push")
        // next, push local upserts to remote
        val unsavedObjects = localStorage.objects(foreignKey).first().filter(unsavedFilter)
        if (!unsavedObjects.isEmpty()) {
            if( DEBUG ) println("${this@CollectionDifferenceForeignKeySyncOrchestrator} push ${unsavedObjects.count()} upserts to remote")
            remoteStorage.updateObjects(foreignKey, unsavedObjects)
                .flatMapLatest { localStorage.updateObjects(foreignKey, it.map(afterPush)) }
                .collect()
        } else {
            if( DEBUG ) println("${this@CollectionDifferenceForeignKeySyncOrchestrator} nothing to push to remote")
        }

        // report 50% progress
        emit(0.5f)

        if( DEBUG ) println("${this@CollectionDifferenceForeignKeySyncOrchestrator} fetch local objects before pull")
        // read local collection into map
        val localObjects = localStorage.objects(foreignKey).first()
        if( DEBUG ) println("${this@CollectionDifferenceForeignKeySyncOrchestrator} insert local objects into map")
        val localMap = localObjects.map { it.key to it }.toMap().toMutableMap()

        if( DEBUG ) println("${this@CollectionDifferenceForeignKeySyncOrchestrator} fetch remote objects")
        // read from remote
        val remoteObjects = remoteStorage.objects(foreignKey).first()
        if( DEBUG ) println("${this@CollectionDifferenceForeignKeySyncOrchestrator} received ${remoteObjects.count()} objects")

        // upsert remote objects first
        localStorage.updateObjects(foreignKey, remoteObjects.map(afterPull)).collect()
        if( DEBUG ) println("${this@CollectionDifferenceForeignKeySyncOrchestrator} upserted objects into local store")

        // find local objects no longer in the remote set
        for (obj in remoteObjects) {
            localMap.remove(obj.key)
        }
        val objectsPendingDelete = localMap.values.toList()
        if (!objectsPendingDelete.isEmpty()) {
            localStorage.destroyObjects(foreignKey, objectsPendingDelete).collect()
        } else {
            if( DEBUG ) println("${this@CollectionDifferenceForeignKeySyncOrchestrator} nothing to delete from local")
        }

        // report 100% progress
        emit(1.0f)
        println("${this@CollectionDifferenceForeignKeySyncOrchestrator} synced")
    }.flowOn(dispatcher)
}