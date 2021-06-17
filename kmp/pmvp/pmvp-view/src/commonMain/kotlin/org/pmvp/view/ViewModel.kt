package org.pmvp.view

import kotlinx.coroutines.flow.Flow

interface ViewModelState

/**
 * A contract between ViewModel and a view for producing a [ViewModelState]'s.
 */
interface ViewModelStateProducer<T : ViewModelState> {
    val state: Flow<T>
}

interface ViewModelIntent

interface ViewModelIntentConsumer<N : ViewModelIntent> {
    fun onIntent(intent: N)
}

interface ViewModel<T : ViewModelState, N : ViewModelIntent> :
    ViewModelStateProducer<T>,
    ViewModelIntentConsumer<N>
