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
import org.pmvp.view.*
import kotlin.test.assertEquals
import kotlin.test.asserter

@FlowPreview
@ExperimentalCoroutinesApi
class SectionListViewModelTest {

    private enum class Item(val key: String) : ListItem {
        DESK("desk"),
        CHAIR("chair"),
        LAMP("lamp")
    }

    private data class Section(
        override val header: String?,
        override val items: List<Item>
    ): ListSection<Item>

    private class ItemListViewModel(
        scope: CoroutineScope
    ) : SectionListViewModel<Item, Section, SectionListViewModelIntent<Item, Section>>(
        scope = scope
    )

    @Test
    fun testColdSource() = runBlockingTest {
        val model = ItemListViewModel(this)

        // should start in loading state
        val initialState = model.state.first()
        assertEquals(ListViewModelState.Loading, initialState)

        // should transition to selecting state upon receiving a source
        val items1 = listOf(Item.DESK, Item.CHAIR)
        val section1 = Section("headerTitle", items1)
        val items2 = listOf(Item.LAMP)
        val section2 = Section(null, items2)
        val source = flowOf(listOf(section1, section2))
        model.onIntent(SectionListViewModelIntent.Register(source))
        val selectingState = model.state.first()
        assertEquals(ListViewModelState.Selecting, selectingState)

        // should have two sections as configured above
        val sections = model.items.first()
        assertEquals(2, sections.count())
        val first = sections.first()
        val last = sections.last()
        assertEquals(section1.header, first.header)
        assertEquals(2, first.items.count())
        assertEquals(section1.items.first().key, first.items.first().key)
        assertEquals(section2.header, last.header)
        assertEquals(1, last.items.count())
        assertEquals(section2.items.first().key, last.items.first().key)

        // should transition to done state on cancel
        model.onIntent(SectionListViewModelIntent.Cancel())
        assertEquals(ListViewModelState.Canceled, model.state.first())
    }

}
