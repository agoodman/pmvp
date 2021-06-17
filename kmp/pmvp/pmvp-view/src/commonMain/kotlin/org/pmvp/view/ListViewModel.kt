package org.pmvp.view

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

interface ListItem

enum class ListViewModelState : ViewModelState {
    Loading,
    Selecting,
    Selected,
    Canceled
}

open class ListViewModelIntent<T : ListItem> :
    ViewModelIntent {
    data class Register<T : ListItem>(
        val itemSource: Flow<List<T>>
    ) : ListViewModelIntent<T>()
    class Unregister<T: ListItem> : ListViewModelIntent<T>()

    data class Select<T : ListItem>(
        val selectedItem: T
    ) : ListViewModelIntent<T>()

    class Clear<T: ListItem> : ListViewModelIntent<T>()
    class Confirm<T: ListItem> : ListViewModelIntent<T>()
    class Cancel<T : ListItem> : ListViewModelIntent<T>()
}

/**
 * States:
 *
 *   loading <-> selecting -> { selected, canceled }
 *
 * Intents:
 *
 *   register(itemSource) - registers the model to consume the given flow as its source of item records
 *   unregister - disposes any active item source observer and returns to loading state
 *   select(item) - select the given item
 *   clear() - clear selected item, setting to null
 *   confirm() - confirm selection
 *   cancel() - cancel the list selection and abort
 *
 * Properties:
 *
 *   items - available items, exactly as received from its item source; empty if no source is registered.
 *   selectedItem - upon selecting a valid item, this will be updated to represent the selected item; null otherwise.
 */
@ExperimentalCoroutinesApi
@FlowPreview
open class ListViewModel<T : ListItem, N: ListViewModelIntent<T>>(
    val scope: CoroutineScope
) : StatefulViewModel<ListViewModelState, N>(ListViewModelState.Loading) {

    /**
     * Private channel used by the model to store a reference to the list of available items.
     *
     * After receiving a Register intent, this channel emits any value emitted by the registered source.
     */
    private var itemsChannel: ConflatedBroadcastChannel<List<T>> =
        ConflatedBroadcastChannel(emptyList())

    /**
     * A list of items managed by this view model.
     */
    val items: Flow<List<T>>
        get() = itemsChannel.asFlow()

    /**
     * Private channel used by the model to store a reference to the selected item.
     *
     * After receiving a Select intent, this channel emits a non-null value representing the selected item.
     */
    private val selectedItemChannel: ConflatedBroadcastChannel<T?> =
        ConflatedBroadcastChannel(null)

    /**
     * A nullable reference to the selected item, if any is selected; null otherwise.
     */
    val selectedItem: Flow<T?>
        get() = selectedItemChannel.asFlow()

    /**
     * Private reference to the job responsible for binding registered source with items channel.
     *
     * Upon receiving a Register intent, this reference is assigned.
     *
     * This is required to allow the state lifecycle to dispose the registered source observer.
     */
    private var itemSourceJob: Job? = null

    @Suppress("UNCHECKED_CAST")
    override fun onIntent(intent: N) {
        when (intent) {
            is ListViewModelIntent.Register<*> -> registerSource(intent.itemSource as Flow<List<T>>)
            is ListViewModelIntent.Unregister<*> -> unregister()
            is ListViewModelIntent.Select<*> -> selectItem(intent.selectedItem as T)
            is ListViewModelIntent.Clear<*> -> clearSelection()
            is ListViewModelIntent.Confirm<*> -> confirmSelection()
            is ListViewModelIntent.Cancel<*> -> cancelSelection()
        }
    }

    protected fun registerSource(flow: Flow<List<T>>) {
        if( !expect(ListViewModelState.Loading) ) {
            didRejectRegister()
            return
        }
        itemSourceJob?.cancel()
        itemSourceJob = flow.onEach {
            itemsChannel.send(it)
        }
            .launchIn(scope)
        transitionTo(ListViewModelState.Selecting)
    }

    protected fun unregister() {
        if (!expect(ListViewModelState.Selecting)) {
            didRejectRegister()
            return
        }

        disposeSource()
        transitionTo(ListViewModelState.Loading)
    }

    internal open fun didRejectRegister() {
    }

    protected fun disposeSource() {
        itemSourceJob?.cancel()
        itemSourceJob = null
    }

    protected fun selectItem(item: T) {
        if (!expect(ListViewModelState.Selecting)) {
            didRejectSelect(item)
            return
        }
        selectedItemChannel.offer(item)
    }

    internal open fun didRejectSelect(item: T) {
    }

    protected fun clearSelection() {
        if (!expect(ListViewModelState.Selecting)) {
            didRejectClear()
            return
        }
        selectedItemChannel.offer(null)
    }

    internal open fun didRejectClear() {
    }

    protected fun confirmSelection() {
        if( !expect(ListViewModelState.Selecting) ) {
            didRejectConfirm()
            return
        }
        transitionTo(ListViewModelState.Selected)
        disposeSource()
    }

    internal open fun didRejectConfirm() {
    }

    protected fun cancelSelection() {
        if( !expect(ListViewModelState.Selecting) ) {
            didRejectCancel()
            return
        }
        selectedItemChannel.offer(null)
        transitionTo(ListViewModelState.Canceled)
        disposeSource()
    }

    internal open fun didRejectCancel() {
    }
}
