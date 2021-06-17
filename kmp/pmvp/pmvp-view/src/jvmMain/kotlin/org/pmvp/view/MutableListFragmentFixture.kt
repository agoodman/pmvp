package org.pmvp.view

import androidx.fragment.app.Fragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

/**
 * Fragment binding fixture for [MutableListViewModel]
 *
 * Activates or cancels a consumer for the [MutableListViewModel.elements] flow.
 */
@FlowPreview
@ExperimentalCoroutinesApi
interface MutableListFragmentFixture<T> {

    /**
     * Activates a binding between the given [fragment] and [viewModel].
     *
     * This will cancel any active binding, if present.
     */
    fun bind(fragment: Fragment, viewModel: MutableListViewModel<T>)

    /**
     * Cancels the active binding, if any.
     */
    fun cancel()
}
