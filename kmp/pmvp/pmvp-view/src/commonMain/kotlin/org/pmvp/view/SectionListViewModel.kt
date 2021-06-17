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

interface ListSection<T: ListItem> {
    val header: String?
    val items: List<T>
}

open class SectionListViewModelIntent<LI: ListItem, T: ListSection<LI>> :
    ViewModelIntent {
    data class Register<LI: ListItem, T: ListSection<LI>>(
        val sectionSource: Flow<List<T>>
    ) : SectionListViewModelIntent<LI, T>()
    class Unregister<LI: ListItem, T: ListSection<LI>>: SectionListViewModelIntent<LI, T>()
    class Select<LI: ListItem, T: ListSection<LI>>(val item: LI): SectionListViewModelIntent<LI, T>()
    class Clear<LI: ListItem, T: ListSection<LI>>: SectionListViewModelIntent<LI, T>()
    class Confirm<LI: ListItem, T: ListSection<LI>>: SectionListViewModelIntent<LI, T>()
    class Cancel<LI: ListItem, T: ListSection<LI>>: SectionListViewModelIntent<LI, T>()
}

/**
 * States:
 *
 *   loading <-> selecting -> { selected, canceled }
 *
 * Intents:
 *
 *   register(source) - registers the model to consume the given flow as its source of sections
 *   unregister() - disposes registered source observer
 *   select(item) - selects the given item in the given section
 *   confirm() - confirms selection with terminal selected state
 *   clear() - clears selected item
 *   cancel() - cancel the list selection and complete
 *
 * Properties:
 *
 *   sections - available sections, exactly as received from its section source;
 *   empty if no source is registered.
 */
@FlowPreview
@ExperimentalCoroutinesApi
open class SectionListViewModel<LI : ListItem, T: ListSection<LI>, N: SectionListViewModelIntent<LI, T>>(
    val scope: CoroutineScope
) : StatefulViewModel<ListViewModelState, N>(ListViewModelState.Loading) {

    /**
     * Private channel used by the model to store a reference to the list of available sections.
     *
     * After receiving a Register intent, this channel emits any value emitted by the registered source.
     */
    private var sectionsChannel: ConflatedBroadcastChannel<List<T>> =
        ConflatedBroadcastChannel(emptyList())

    /**
     * A list of sections managed by this view model.
     */
    val items: Flow<List<T>>
        get() = sectionsChannel.asFlow()

    /**
     * Private channel used by the model to store the selected item, if any.
     */
    private val selectedItemChannel = ConflatedBroadcastChannel<LI?>(null)
    val selectedItem: Flow<LI?> = selectedItemChannel.asFlow()

    /**
     * Private reference to the job responsible for binding registered source with sections channel.
     *
     * Upon receiving a Register intent, this reference is assigned.
     *
     * This is required to allow the state lifecycle to dispose the registered source observer.
     */
    private var sectionSourceJob: Job? = null

    @Suppress("UNCHECKED_CAST")
    override fun onIntent(intent: N) {
        when (intent) {
            is SectionListViewModelIntent.Register<*, *> -> registerSource(intent.sectionSource as Flow<List<T>>)
            is SectionListViewModelIntent.Unregister<*, *> -> unregister()
            is SectionListViewModelIntent.Select<*, *> -> selectItem(intent.item as LI)
            is SectionListViewModelIntent.Confirm<*, *> -> confirmSelection()
            is SectionListViewModelIntent.Clear<*, *> -> clearSelection()
            is SectionListViewModelIntent.Cancel<*, *> -> cancel()
        }
    }

    protected fun registerSource(flow: Flow<List<T>>) {
        if( !expect(ListViewModelState.Loading) ) {
            didRejectRegister()
            return
        }
        sectionSourceJob?.cancel()
        sectionSourceJob = flow.onEach { sectionsChannel.send(it) }
            .launchIn(scope)
        transitionTo(ListViewModelState.Selecting)
    }

    protected open fun didRejectRegister() {
    }

    protected fun unregister() {
        if(!expect(ListViewModelState.Selecting)) {
            didRejectUnregister()
            return
        }
        selectedItemChannel.offer(null)
        transitionTo(ListViewModelState.Loading)
        disposeSource()
    }

    protected open fun didRejectUnregister() {
    }

    protected fun disposeSource() {
        sectionSourceJob?.cancel()
        sectionSourceJob = null
    }

    protected fun selectItem(item: LI) {
        if(!expect(ListViewModelState.Selecting)) {
            didRejectSelect(item)
            return
        }
        selectedItemChannel.offer(item)
    }

    protected open fun didRejectSelect(item: LI) {
    }

    protected fun clearSelection() {
        if(!expect(ListViewModelState.Selecting)) {
            didRejectClear()
            return
        }
        selectedItemChannel.offer(null)
    }

    protected open fun didRejectClear() {
    }

    protected fun confirmSelection() {
        if(!expect(ListViewModelState.Selecting)) {
            didRejectConfirm()
            return
        }
        transitionTo(ListViewModelState.Selected)
        disposeSource()
    }

    protected open fun didRejectConfirm() {
    }

    protected fun cancel() {
        if( !expect(ListViewModelState.Selecting) ) {
            didRejectCancel()
            return
        }
        transitionTo(ListViewModelState.Canceled)
        disposeSource()
    }

    protected open fun didRejectCancel() {
    }
}
