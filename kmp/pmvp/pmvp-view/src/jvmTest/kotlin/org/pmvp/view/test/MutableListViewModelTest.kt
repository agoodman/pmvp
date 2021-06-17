package org.pmvp.view.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.pmvp.view.*
import kotlin.test.assertEquals

@FlowPreview
@ExperimentalCoroutinesApi
class MutableListViewModelTest {

    private class ItemListViewModel(
        scope: CoroutineScope,
        initial: List<Int> = emptyList()
    ) : MutableListViewModel<Int>(scope, initial)

    @Test
    fun testAdd() = runBlockingTest {
        val model = ItemListViewModel(this)

        // should start empty
        val before = model.elements.first()
        assertEquals(0, before.count(), "expected empty list")

        // should have one element after add
        model.onIntent(MutableListViewIntent.Add(1))
        val after = model.elements.first()
        assertEquals(listOf(1), after, "expected [1]")
    }

    @Test
    fun testRemove() = runBlockingTest {
        val model = ItemListViewModel(this, listOf(2))

        // should start with [2]
        val before = model.elements.first()
        assertEquals(1, before.count(), "expected one element")
        assertEquals(2, before.first(), "expected [2]")

        // should be empty after remove
        model.onIntent(MutableListViewIntent.Remove(2))
        val after = model.elements.first()
        assertEquals(0, after.count(), "expected empty list")
    }

    @Test
    fun testBringToFront() = runBlockingTest {
        val model = ItemListViewModel(this, listOf(1, 2))

        // should start with [1,2]
        val before = model.elements.first()
        assertEquals(2, before.count(), "expected two elements")
        before.firstOrNull()?.let { assertEquals(1, it, "first element should be 1")}
        before.lastOrNull()?.let { assertEquals(2, it, "last element should be 2")}

        model.onIntent(MutableListViewIntent.BringToFront(1))

        // should reorganize elements to [2,1]
        val after = model.elements.first()
        assertEquals(2, after.count(), "expected two elements")
        after.firstOrNull()?.let { assertEquals(2, it, "first element should be 2")}
        after.lastOrNull()?.let { assertEquals(1, it, "last element should be 1")}
    }

    @Test
    fun testSendToBack() = runBlockingTest {
        val model = ItemListViewModel(this, listOf(1, 2))

        // should start with [1,2]
        val before = model.elements.first()
        assertEquals(2, before.count(), "expected two elements")
        before.firstOrNull()?.let { assertEquals(1, it, "first element should be 1")}
        before.lastOrNull()?.let { assertEquals(2, it, "last element should be 2")}

        model.onIntent(MutableListViewIntent.SendToBack(2))

        // should reorganize elements to [2,1]
        val after = model.elements.first()
        assertEquals(2, after.count(), "expected two elements")
        after.firstOrNull()?.let { assertEquals(2, it, "first element should be 2")}
        after.lastOrNull()?.let { assertEquals(1, it, "last element should be 1")}
    }

    @Test
    fun testBringForward() = runBlockingTest {
        val model = ItemListViewModel(this, listOf(1, 2, 3))

        // should start with [1,2,3]
        val before = model.elements.first()
        assertEquals(3, before.count(), "expected three elements")
        before.firstOrNull()?.let { assertEquals(1, it, "first element should be 1")}
        before.lastOrNull()?.let { assertEquals(3, it, "last element should be 3")}

        model.onIntent(MutableListViewIntent.BringForward(2))

        // should reorganize elements to [1,3,2]
        val after = model.elements.first()
        assertEquals(3, after.count(), "expected three elements")
        after.firstOrNull()?.let { assertEquals(1, it, "first element should be 1")}
        after.lastOrNull()?.let { assertEquals(2, it, "last element should be 2")}
    }

    @Test
    fun testSendBackward() = runBlockingTest {
        val model = ItemListViewModel(this, listOf(1, 2, 3))

        // should start with [1,2,3]
        val before = model.elements.first()
        assertEquals(3, before.count(), "expected three elements")
        before.firstOrNull()?.let { assertEquals(1, it, "first element should be 1")}
        before.lastOrNull()?.let { assertEquals(3, it, "last element should be 3")}

        model.onIntent(MutableListViewIntent.SendBackward(2))

        // should reorganize elements to [2,1,3]
        val after = model.elements.first()
        assertEquals(3, after.count(), "expected three elements")
        after.firstOrNull()?.let { assertEquals(2, it, "first element should be 2")}
        after.lastOrNull()?.let { assertEquals(3, it, "last element should be 3")}
    }

    @Test
    fun testClear() = runBlockingTest {
        val model = ItemListViewModel(this, listOf(1, 2))

        // should start with [1,2]
        val before = model.elements.first()
        assertEquals(2, before.count(), "expected two elements")
        before.firstOrNull()?.let { assertEquals(1, it, "first element should be 1")}
        before.lastOrNull()?.let { assertEquals(2, it, "last element should be 2")}

        model.onIntent(MutableListViewIntent.Clear())

        // should have empty list
        val after = model.elements.first()
        assertEquals(0, after.count(), "expected empty list")
    }
}
