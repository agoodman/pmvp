package org.pmvp.view.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.pmvp.view.ListItem
import org.pmvp.view.ListViewModel
import org.pmvp.view.ListViewModelIntent
import org.pmvp.view.ListViewModelState
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@FlowPreview
class ListViewModelTest {

    private enum class Item(val key: String) : ListItem {
        DESK("desk"),
        CHAIR("chair"),
        LAMP("lamp")
    }

    private class ItemListViewModel(
        scope: CoroutineScope
    ) : ListViewModel<Item, ListViewModelIntent<Item>>(
        scope
    )

    @Test
    fun testColdSourceCancel() = runBlockingTest {
        val model = ItemListViewModel(this)

        // should start in loading state
        val initialState = model.state.first()
        assertEquals(ListViewModelState.Loading, initialState)

        // should transition to selecting state upon receiving a source
        val source = flowOf(listOf(Item.DESK))
        model.onIntent(ListViewModelIntent.Register(source))
        val selectingState = model.state.first()
        assertEquals(ListViewModelState.Selecting, selectingState)

        // should transition to selected state upon receiving a selection intent
        val item = model.items.first().first()
        model.onIntent(ListViewModelIntent.Select(item))
        assertEquals(item, model.selectedItem.first())
        assertEquals(ListViewModelState.Selecting, model.state.first())
        model.onIntent(ListViewModelIntent.Cancel())
        assertEquals(ListViewModelState.Canceled, model.state.first())
    }

    @Test
    fun testColdSourceConfirm() = runBlockingTest {
        val model = ItemListViewModel(this)

        // should start in loading state
        val initialState = model.state.first()
        assertEquals(ListViewModelState.Loading, initialState)

        // should transition to selecting state upon receiving a source
        val source = flowOf(listOf(Item.DESK))
        model.onIntent(ListViewModelIntent.Register(source))
        val selectingState = model.state.first()
        assertEquals(ListViewModelState.Selecting, selectingState)

        // should transition to selected state upon receiving a selection intent
        val item = model.items.first().first()
        model.onIntent(ListViewModelIntent.Select(item))
        assertEquals(item, model.selectedItem.first())
        assertEquals(ListViewModelState.Selecting, model.state.first())
        model.onIntent(ListViewModelIntent.Confirm())
        assertEquals(ListViewModelState.Selected, model.state.first())
    }

    @Test
    fun testHotSource() = runBlockingTest {
        val model = ItemListViewModel(this)
        val sourceChannel = ConflatedBroadcastChannel<List<Item>>(emptyList())
        model.onIntent(ListViewModelIntent.Register(sourceChannel.asFlow()))

        // should have no items
        assertEquals(0, model.items.first().count())
        assertEquals(ListViewModelState.Selecting, model.state.first())

        // should have one after new value emitted from channel
        sourceChannel.send(listOf(Item.CHAIR))
        assertEquals(1, model.items.first().count())
        assertEquals(ListViewModelState.Selecting, model.state.first())

        // should have different value
        sourceChannel.send(listOf(Item.LAMP, Item.CHAIR))
        val items = model.items.first()
        assertEquals(2, items.count())
        assertEquals("lamp", items.first().key)

        // should transition to canceled state upon receiving a cancel intent
        model.onIntent(ListViewModelIntent.Cancel())
        assertEquals(ListViewModelState.Canceled, model.state.first())
    }
}
