package org.pmvp.view

import kotlinx.coroutines.flow.Flow

interface ListSectionFilter<LI: ListItem, T: ListSection<LI>> {
    operator fun invoke(sections: List<T>): List<T>
}

interface ListSectionFilterProducer<LI: ListItem, T: ListSection<LI>> {
    val filter: Flow<ListSectionFilter<LI, T>>
}
