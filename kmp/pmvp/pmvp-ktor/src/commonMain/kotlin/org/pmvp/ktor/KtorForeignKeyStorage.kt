package org.pmvp.ktor

import kotlinx.coroutines.CoroutineDispatcher
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.pmvp.ForeignKeyStorage
import org.pmvp.Proxy

interface KtorForeignKeyResponder<FK, T> {
    fun parseCollectionResponse(foreignKey: FK, response: String): List<T>
    fun parseInstanceResponse(foreignKey: FK, response: String): T?
    fun parseUpdateResponse(foreignKey: FK, response: String, proxy: T): T
    fun parseDestroyResponse(foreignKey: FK, response: String, proxy: T): T
}

open class KtorForeignKeyStorage<FK, K, T : Proxy<K>>(
    private val httpClient: HttpClient,
    private val authTokenProvider: AuthTokenProvider,
    private val dispatcher: CoroutineDispatcher,
    private val responder: KtorForeignKeyResponder<FK, T>,
    internal val collectionRequest: suspend (String, FK, HttpClient) -> String = { _, _, _ -> "" },
    internal val instanceRequest: suspend (String, FK, K, HttpClient) -> String = { _, _, _, _ -> "" },
    internal val updateRequest: suspend (String, FK, T, HttpClient) -> String = { _, _, _, _ -> "" },
    internal val destroyRequest: suspend (String, FK, T, HttpClient) -> String = { _, _, _, _ -> "" }
) : ForeignKeyStorage<FK, K, T> {

    override fun objects(foreignKey: FK): Flow<List<T>> =
        authTokenProvider.token()
            .map { token -> collectionRequest(token, foreignKey, httpClient) }
            .map { responder.parseCollectionResponse(foreignKey, it) }
            .flowOn(dispatcher)

    override fun objectFor(foreignKey: FK, key: K): Flow<T?> =
        authTokenProvider.token()
            .map { token -> instanceRequest(token, foreignKey, key, httpClient) }
            .map { responder.parseInstanceResponse(foreignKey, it) }
            .flowOn(dispatcher)

    override fun objectsFor(foreignKey: FK, keys: List<K>): Flow<List<T>> {
        // TODO: consider implementing this
        return flowOf(emptyList())
    }

    override fun updateObject(foreignKey: FK, proxy: T): Flow<T> =
        authTokenProvider.token()
            .map { token -> updateRequest(token, foreignKey, proxy, httpClient) }
            .map { responder.parseUpdateResponse(foreignKey, it, proxy) }
            .flowOn(dispatcher)

    override fun updateObjects(foreignKey: FK, proxies: List<T>): Flow<List<T>> {
        // TODO: consider implementing this
        return flowOf(emptyList())
    }

    override fun destroyObject(foreignKey: FK, proxy: T): Flow<T> =
        authTokenProvider.token()
            .map { token -> destroyRequest(token, foreignKey, proxy, httpClient) }
            .map { responder.parseDestroyResponse(foreignKey, it, proxy) }
            .flowOn(dispatcher)

    override fun destroyObjects(foreignKey: FK, objects: List<T>): Flow<List<T>> {
        // TODO: consider implementing this
        return flowOf(emptyList())
    }
}
