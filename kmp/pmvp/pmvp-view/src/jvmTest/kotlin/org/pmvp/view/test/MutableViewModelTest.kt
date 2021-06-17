package org.pmvp.view.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.pmvp.InMemoryStorage
import org.pmvp.Provider
import org.pmvp.Proxy
import org.pmvp.Storage
import org.pmvp.view.MutableViewModel
import org.pmvp.view.MutableViewModelIntent
import org.pmvp.view.MutableViewModelState
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@FlowPreview
@ExperimentalCoroutinesApi
class MutableViewModelTest {

    private data class ItemModel(
        override val key: Int,
        val value: Int
    ): Proxy<Int>

    private class ItemProvider(
        localStorage: Storage<Int, ItemModel>,
        remoteStorage: Storage<Int, ItemModel>
    ): Provider<Int, ItemModel>(
        localStorage = localStorage,
        remoteStorage = remoteStorage)

    private lateinit var provider: Provider<Int, ItemModel>

    private fun buildProvider(items: MutableMap<Int, ItemModel> = mutableMapOf()) {
        val local = InMemoryStorage<Int, ItemModel>(items)
        val remote = InMemoryStorage<Int, ItemModel>()
        provider = ItemProvider(local, remote)
    }

    @Test
    fun testSetKey() = runBlockingTest {
        val item = ItemModel(1, 2)
        buildProvider(mutableMapOf(item.key to item))
        val viewModel = MutableViewModel<Int, ItemModel, Provider<Int, ItemModel>>(provider, this)
        val initialState = viewModel.state.first()
        assertEquals(MutableViewModelState.Loading, initialState, "bad initial state")
        viewModel.onIntent(MutableViewModelIntent.SetKey(1))
        val readyState = viewModel.state.first()
        assertEquals(MutableViewModelState.Configuring, readyState, "failed to transition")
        val model = viewModel.model.first()
        assertEquals(item.value, model.value, "bad value")
    }

    @Test
    fun testModifyAndCancel() = runBlockingTest {
        val item = ItemModel(1, 2)
        buildProvider(mutableMapOf(item.key to item))
        val viewModel = MutableViewModel<Int, ItemModel, Provider<Int, ItemModel>>(provider, this)
        viewModel.onIntent(MutableViewModelIntent.SetKey(1))
        val model = viewModel.model.first()
        viewModel.onIntent(MutableViewModelIntent.SetModel(model.copy(value = 3)))
        val modifiedModel = viewModel.model.first()
        assertEquals(3, modifiedModel.value, "expected modified value")
        viewModel.onIntent(MutableViewModelIntent.Cancel())
        val finalState = viewModel.state.first()
        assertEquals(MutableViewModelState.Canceled, finalState, "expected cancel state")
        val actualModel = provider.model(1).first()
        assertNotNull(actualModel)
        assertEquals(item.value, actualModel.value, "expected no change to persistent store")
    }

    @Test
    fun testModifyAndSubmit() = runBlockingTest {
        val item = ItemModel(1, 2)
        buildProvider(mutableMapOf(item.key to item))
        val viewModel = MutableViewModel<Int, ItemModel, Provider<Int, ItemModel>>(provider, this)
        viewModel.onIntent(MutableViewModelIntent.SetKey(1))
        val model = viewModel.model.first()
        val newValue = 3
        viewModel.onIntent(MutableViewModelIntent.SetModel(model.copy(value = newValue)))
        val modifiedModel = viewModel.model.first()
        assertEquals(newValue, modifiedModel.value, "expected modified value")
        viewModel.onIntent(MutableViewModelIntent.Submit())
        val finalState = viewModel.state.first()
        assertEquals(MutableViewModelState.Updated, finalState, "expected updated state")
        val actualModel = provider.model(1).first()
        assertNotNull(actualModel)
        assertEquals(newValue, actualModel.value, "expected change reflected in persistent store")
    }
}
