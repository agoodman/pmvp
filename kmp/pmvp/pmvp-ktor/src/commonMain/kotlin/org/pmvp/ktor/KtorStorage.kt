package org.pmvp.ktor

import kotlinx.coroutines.CoroutineDispatcher
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.*
import org.pmvp.Proxy
import org.pmvp.Storage

interface KtorResponder<T> {
    fun parseCollectionResponse(response: String): List<T>
    fun parseInstanceResponse(response: String): T?
    fun parseUpdateResponse(response: String, proxy: T): T
    fun parseDestroyResponse(response: String, proxy: T): T
}

open class KtorStorage<K, T : Proxy<K>>(
    private val httpClient: HttpClient,
    private val authTokenProvider: AuthTokenProvider,
    private val dispatcher: CoroutineDispatcher,
    private val responder: KtorResponder<T>,
    internal val collectionRequest: suspend (String, HttpClient) -> String = { _, _ -> "" },
    internal val instanceRequest: suspend (String, HttpClient, K) -> String = { _, _, _ -> "" },
    internal val updateRequest: suspend (String, HttpClient, T) -> String = { _, _, _ -> "" },
    internal val destroyRequest: suspend (String, HttpClient, T) -> String = { _, _, _ -> "" }
) : Storage<K, T> {

    override fun objects(): Flow<List<T>> =
        authTokenProvider.token()
            .map { token -> collectionRequest(token, httpClient) }
            .map { responder.parseCollectionResponse(it) }
            .flowOn(dispatcher)

    override fun objectFor(key: K): Flow<T?> =
        authTokenProvider.token()
            .map { token -> instanceRequest(token, httpClient, key) }
            .map { responder.parseInstanceResponse(it) }
            .flowOn(dispatcher)

    override fun objectsFor(keys: List<K>): Flow<List<T>> {
        // TODO: consider implementing this
        return flowOf(emptyList())
    }

    override fun updateObject(proxy: T): Flow<T> =
        authTokenProvider.token()
            .map { token -> updateRequest(token, httpClient, proxy) }
            .map { responder.parseUpdateResponse(it, proxy) }
            .flowOn(dispatcher)

    override fun updateObjects(proxies: List<T>): Flow<List<T>> = flow {
        emit(proxies.map {
            updateObject(it).first()
        })
    }

    override fun destroyObject(proxy: T): Flow<T> =
        authTokenProvider.token()
            .map { token -> destroyRequest(token, httpClient, proxy) }
            .map { responder.parseDestroyResponse(it, proxy) }
            .flowOn(dispatcher)

    override fun destroyObjects(objects: List<T>): Flow<List<T>> {
        // TODO: consider implementing this
        return flowOf(emptyList())
    }
}
