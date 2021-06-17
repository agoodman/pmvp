package org.pmvp.view

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.pmvp.Immutable
import org.pmvp.Mutable
import org.pmvp.Proxy

sealed class MutableViewModelIntent<K, T: Proxy<K>>: ViewModelIntent {
    data class SetKey<K, T: Proxy<K>>(val key: K): MutableViewModelIntent<K, T>()
    data class SetModel<K, T: Proxy<K>>(
        val model: T
    ): MutableViewModelIntent<K, T>()
    class Submit<K, T: Proxy<K>>(): MutableViewModelIntent<K, T>()
    class Cancel<K, T: Proxy<K>>(): MutableViewModelIntent<K, T>()
}

enum class MutableViewModelState(val key: String): ViewModelState {
    Loading("loading"),
    Invalid("invalid"),
    Configuring("configuring"),
    Updating("updating"),
    Updated("updated"),
    Failed("failed"),
    Canceled("canceled")
}

@FlowPreview
@ExperimentalCoroutinesApi
class MutableViewModel<K, T: Proxy<K>, P>(
    private val provider: P,
    private val scope: CoroutineScope
): StatefulViewModel<MutableViewModelState, MutableViewModelIntent<K, T>>(
    initialState = MutableViewModelState.Loading
) where P: Mutable<K, T>, P: Immutable<K, T> {

    private val modelChannel = ConflatedBroadcastChannel<T>()
    val model: Flow<T> = modelChannel.asFlow()

    override fun onIntent(intent: MutableViewModelIntent<K, T>) {
        when(intent) {
            is MutableViewModelIntent.SetKey -> setKey(intent.key)
            is MutableViewModelIntent.SetModel -> setModel(intent.model)
            is MutableViewModelIntent.Submit -> submit()
            is MutableViewModelIntent.Cancel -> cancel()
        }
    }

    private fun setKey(key: K) {
        if( !expect(MutableViewModelState.Loading) ) {
            return
        }

        scope.launch {
            val existing = provider.model(key).first()
            existing?.let {
                modelChannel.send(it)
                transitionTo(MutableViewModelState.Configuring)
            } ?: transitionTo(MutableViewModelState.Invalid)
        }
    }

    private fun setModel(model: T) {
        if( !expect(MutableViewModelState.Configuring) ) {
            return
        }

        modelChannel.offer(model)
    }

    private fun submit() {
        if (!expect(MutableViewModelState.Configuring) ) {
            return
        }

        model
            .take(1)
            .onEach { transitionTo(MutableViewModelState.Updating) }
            .flatMapLatest { provider.upsert(it) }
            .onEach { transitionTo(MutableViewModelState.Updated) }
            .catch { transitionTo(MutableViewModelState.Failed) }
            .launchIn(scope)
    }

    private fun cancel() {
        transitionTo(MutableViewModelState.Canceled)
    }

}
