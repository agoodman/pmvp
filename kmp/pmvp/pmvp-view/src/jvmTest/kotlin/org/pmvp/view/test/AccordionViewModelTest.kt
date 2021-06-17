package org.pmvp.view.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.first
import org.junit.Test
import kotlinx.coroutines.test.runBlockingTest
import org.pmvp.view.AccordionItemSource
import org.pmvp.view.AccordionViewModel
import org.pmvp.view.AccordionViewModelIntent
import kotlin.test.assertEquals

@FlowPreview
@ExperimentalCoroutinesApi
class AccordionViewModelTest {
    private class ItemSourceTestImpl: AccordionItemSource {
        override fun numberOfSections(): Int {
            return 1
        }

        override fun sectionHeaderLabel(section: Int): String {
            return "header $section"
        }

        override fun headerRowLabel(section: Int, selectedItems: List<String>): String {
            return "header row $section"
        }

        override fun numberOfRows(section: Int): Int {
            return 1
        }

        override fun rowLabel(section: Int, row: Int): String {
            return "row $section $row"
        }

        override fun sectionCanSelectMultiple(section: Int): Boolean {
            return false
        }

    }

    @Test
    fun testExpand() = runBlockingTest {
        val itemSource = ItemSourceTestImpl()
        val viewModel = AccordionViewModel(this, itemSource)

        val sections = viewModel.items.first()
        assertEquals(1, sections.count(), "wrong section count")
        val rows = sections[0].items
        assertEquals(1, rows.count(), "wrong row count")

        viewModel.onIntent(AccordionViewModelIntent.Expand(0))

        val expandedSections = viewModel.items.first()
        assertEquals(1, expandedSections.count(), "wrong expanded section count")
        val expandedRows = expandedSections[0].items
        assertEquals(2, expandedRows.count(), "wrong expanded row count")
        assertEquals(true, expandedRows[0].canExpand)
        assertEquals(true, expandedRows[0].isExpanded)
        assertEquals(false, expandedRows[0].canSelect)
        assertEquals(false, expandedRows[0].isSelected)
        assertEquals(false, expandedRows[1].canExpand)
        assertEquals(false, expandedRows[1].isExpanded)
        assertEquals(true, expandedRows[1].canSelect)
        assertEquals(false, expandedRows[1].isSelected)
    }

    @Test
    fun testSelect() = runBlockingTest {
        val itemSource = ItemSourceTestImpl()
        val viewModel = AccordionViewModel(this, itemSource)

        val sections = viewModel.items.first()
        assertEquals(1, sections.count(), "wrong section count")
        val rows = sections[0].items
        assertEquals(1, rows.count(), "wrong row count")

        viewModel.onIntent(AccordionViewModelIntent.Expand(0))
        viewModel.onIntent(AccordionViewModelIntent.Select(0, 0))

        val expandedSections = viewModel.items.first()
        assertEquals(1, expandedSections.count(), "wrong expanded section count")
        val expandedRows = expandedSections[0].items
        assertEquals(2, expandedRows.count(), "wrong expanded row count")
        assertEquals("header row 0", expandedRows[0].label)
        assertEquals(true, expandedRows[0].canExpand)
        assertEquals(true, expandedRows[0].isExpanded)
        assertEquals(false, expandedRows[0].canSelect)
        assertEquals(false, expandedRows[0].isSelected)
        assertEquals("row 0 0", expandedRows[1].label)
        assertEquals(false, expandedRows[1].canExpand)
        assertEquals(false, expandedRows[1].isExpanded)
        assertEquals(true, expandedRows[1].canSelect)
        assertEquals(true, expandedRows[1].isSelected)
    }
}
