package org.pmvp.view

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*

data class AccordionItem(
    val label: String,
    val canExpand: Boolean,
    val isExpanded: Boolean,
    val canSelect: Boolean,
    val canSelectMultiple: Boolean,
    val isSelected: Boolean
)

data class AccordionSection(
    val header: String,
    val items: List<AccordionItem>,
    val isExpanded: Boolean,
    val canSelectMultiple: Boolean
)

interface AccordionItemSource {
    fun numberOfSections(): Int
    fun sectionHeaderLabel(section: Int): String
    fun headerRowLabel(section: Int, selectedItems: List<String>): String
    fun numberOfRows(section: Int): Int
    fun rowLabel(section: Int, row: Int): String
    fun sectionCanSelectMultiple(section: Int): Boolean
}

enum class AccordionViewModelState: ViewModelState {
    Idle,
    Canceled,
    Completed
}

sealed class AccordionViewModelIntent: ViewModelIntent {
    data class Expand(val section: Int): AccordionViewModelIntent()
    data class Select(val section: Int, val row: Int): AccordionViewModelIntent()
    object Reset: AccordionViewModelIntent()
    object Cancel: AccordionViewModelIntent()
    object Confirm: AccordionViewModelIntent()
}

@ExperimentalCoroutinesApi
@FlowPreview
open class AccordionViewModel(
    val scope: CoroutineScope,
    private val itemSource: AccordionItemSource,
    private val defaultSelection: Map<Int, Map<Int, Boolean>> = emptyMap()
): StatefulViewModel<AccordionViewModelState, AccordionViewModelIntent>(
    initialState = AccordionViewModelState.Idle
) {
    private val expandedSectionMapChannel = ConflatedBroadcastChannel<Map<Int, Boolean>>(emptyMap())
    protected val expandedSectionMap: Flow<Map<Int, Boolean>> = expandedSectionMapChannel.asFlow()

    private val selectedItemMapChannel = ConflatedBroadcastChannel<Map<Int, Map<Int, Boolean>>>(emptyMap())
    protected val selectedItemMap: Flow<Map<Int, Map<Int, Boolean>>> = selectedItemMapChannel.asFlow()

    val items: Flow<List<AccordionSection>> = combine(
        expandedSectionMap,
        selectedItemMap
    ) { expanded: Map<Int, Boolean>, selected: Map<Int, Map<Int, Boolean>> ->
        val numSections = itemSource.numberOfSections()
        (0 until numSections).map { section ->
            val numRows = itemSource.numberOfRows(section)
            val isExpanded = expanded[section] ?: false
            val sectionSelected = selected[section] ?: emptyMap()
            val canSelectMultiple = itemSource.sectionCanSelectMultiple(section)
            val sectionItems = (0..numRows - 1).map { row ->
                AccordionItem(
                    label = itemSource.rowLabel(section, row),
                    canExpand = false,
                    isExpanded = false,
                    canSelect = true,
                    canSelectMultiple = canSelectMultiple,
                    isSelected = sectionSelected[row] ?: false
                )
            }
            val selectedItems = sectionItems.filter { it.isSelected }
            val headerItem = AccordionItem(
                label = itemSource.headerRowLabel(section, selectedItems.map { it.label }),
                canExpand = true,
                isExpanded = expanded[section] ?: false,
                canSelect = false,
                canSelectMultiple = false,
                isSelected = false
            )
            val items = if (isExpanded) {
                listOf(headerItem) + sectionItems
            } else {
                listOf(headerItem)
            }
            AccordionSection(
                header = itemSource.sectionHeaderLabel(section),
                items = items,
                isExpanded = expanded[section] ?: false,
                canSelectMultiple = canSelectMultiple
            )
        }
    }

    init {
        restoreDefaults()
    }

    override fun onIntent(intent: AccordionViewModelIntent) {
        when(intent) {
            is AccordionViewModelIntent.Expand -> toggleExpand(section = intent.section)
            is AccordionViewModelIntent.Select -> toggleSelect(section = intent.section, row = intent.row)
            is AccordionViewModelIntent.Reset -> restoreDefaults()
            is AccordionViewModelIntent.Cancel -> cancel()
            is AccordionViewModelIntent.Confirm -> confirm()
        }
    }

    private fun cancel() {
        if( !expect(AccordionViewModelState.Idle) ) {
            return
        }
        transitionTo(AccordionViewModelState.Canceled)
    }

    private fun confirm() {
        if( !expect(AccordionViewModelState.Idle) ) {
            return
        }
        transitionTo(AccordionViewModelState.Completed)
    }

    private fun restoreDefaults() {
        expandedSectionMapChannel.offer(emptyMap())
        selectedItemMapChannel.offer(defaultSelection)
    }

    private fun toggleExpand(section: Int) {
        if( !expect(AccordionViewModelState.Idle) ) {
            return
        }
        expandedSectionMap.take(1)
            .onEach { expanded ->
                val isExpanded = expanded[section] ?: false
                val newMap = expanded.toMutableMap()
                newMap[section] = !isExpanded
                expandedSectionMapChannel.offer(newMap)
            }
            .launchIn(scope)
    }

    private fun toggleSelect(section: Int, row: Int) {
        if( !expect(AccordionViewModelState.Idle) ) {
            return
        }
        val canSelectMultiple = itemSource.sectionCanSelectMultiple(section)

        selectedItemMap.take(1)
            .onEach { selected ->
                val rowSelectionMap = selected[section] ?: emptyMap()
                val isSelected = rowSelectionMap[row] ?: false

                if( canSelectMultiple ) {
                    val newMap: MutableMap<Int, Map<Int, Boolean>> = selected.toMutableMap()
                    val newRowSelectionMap = rowSelectionMap.toMutableMap()
                    newRowSelectionMap[row] = !isSelected
                    newMap[section] = newRowSelectionMap
                    selectedItemMapChannel.offer(newMap)
                }
                else {
                    val newMap: MutableMap<Int, Map<Int, Boolean>> = selected.toMutableMap()
                    newMap[section] = mapOf(row to true)
                    selectedItemMapChannel.offer(newMap)
                }
            }
            .launchIn(scope)
    }

}
