package org.pmvp.view

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@ExperimentalCoroutinesApi
@FlowPreview
open class StatefulViewModel<T: ViewModelState, N: ViewModelIntent>(
        initialState: T
) : ViewModel<T, N> {

    private val stateChannel = ConflatedBroadcastChannel<T>()

    override val state: Flow<T>
        get() = stateChannel.asFlow()

    override fun onIntent(intent: N) {
    }

    init {
        stateChannel.offer(initialState)
    }

    public fun expect(state: T): Boolean {
        return( stateChannel.value == state )
    }

    public fun transitionTo(state: T) {
        willTransitionTo(state)
        stateChannel.offer(state)
        didTransitionTo(state)
    }

    open fun willTransitionTo(newState: T) {
    }

    open fun didTransitionTo(newState: T) {
    }

}