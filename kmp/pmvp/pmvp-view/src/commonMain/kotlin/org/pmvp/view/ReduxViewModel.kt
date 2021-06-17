package org.pmvp.view

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*


open class ReduxViewModel<T: ViewModelState, N: ViewModelIntent>(
    private val scope: CoroutineScope,
    initialState: T,
    private val intentReducer: ViewModelReducer<T, N>
): ViewModelIntentConsumer<N>, ViewModelStateProducer<T> {

    private val stateFlow = MutableStateFlow(initialState)
    override val state: Flow<T> = stateFlow

    override fun onIntent(intent: N) {
        stateFlow.take(1)
            .map { intentReducer.reduce(it, intent) }
            .onEach { stateFlow.value = it }
            .launchIn(scope)
    }
}

interface ViewModelReducer<T: ViewModelState, N: ViewModelIntent> {
    suspend fun reduce(currentState: T, intent: N): T
}
