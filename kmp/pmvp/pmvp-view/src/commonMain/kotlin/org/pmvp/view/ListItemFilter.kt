package org.pmvp.view

import kotlinx.coroutines.flow.Flow

interface ListItemFilter<T: ListItem> {
    operator fun invoke(items: List<T>): List<T>
}

interface ListItemFilterProducer<T: ListItem> {
    val filter: Flow<ListItemFilter<T>>
}
