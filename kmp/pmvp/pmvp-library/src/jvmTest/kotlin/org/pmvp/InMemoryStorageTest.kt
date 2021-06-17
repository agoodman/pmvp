package org.pmvp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class InMemoryStorageTest {

    private data class Item(
        override val key: Int,
        val name: String
    ) : Proxy<Int>
    private class ItemStorage(items: MutableMap<Int, Item>): InMemoryStorage<Int, Item>(items)

    @Test
    fun testAllObjectsEmpty() = runBlockingTest {
        val storage = ItemStorage(mutableMapOf())
        val actual = storage.objects().first()
        assertEquals(0, actual.count())
    }

    @Test
    fun testObjectByKeyEmpty() = runBlockingTest {
        val storage = ItemStorage(mutableMapOf())
        val actual = storage.objectFor(1).first()
        assertNull(actual)
    }

    @Test
    fun testAllObjects() = runBlockingTest {
        val storage = ItemStorage(mutableMapOf(1 to Item(1, "one")))
        val actual = storage.objects().first()
        assertEquals(1, actual.count())
    }

    @Test
    fun testObjectByKey() = runBlockingTest {
        val storage = ItemStorage(mutableMapOf(1 to Item(1, "one")))
        val actual: Item? = storage.objectFor(1).first()
        assertNotNull(actual)
        assertEquals(1, actual.key)
    }

    @Test
    fun testObjectBatch() = runBlockingTest {
        val item1 = Item(1, "one")
        val item2 = Item(2, "two")
        val item3 = Item(3, "three")
        val storage = ItemStorage(mutableMapOf(1 to item1, 2 to item2, 3 to item3))
        val actual = storage.objectsFor(listOf(1, 2)).first()
        assertEquals(2, actual.count())
        assertEquals(1, actual.first().key)
        assertEquals(2, actual.last().key)
    }

    @Test
    fun testUpsert() = runBlockingTest {
        val item = Item(1, "one")
        val storage = ItemStorage(mutableMapOf(1 to item))
        storage.updateObject(item.copy(name = "two")).first()
        val actual = storage.objectFor(1).first()
        assertNotNull(actual)
        assertEquals("two", actual.name)
    }

    @Test
    fun testUpsertBatch() = runBlockingTest {
        val item1 = Item(1, "one")
        val item2 = Item(2, "two")
        val item3 = Item(3, "three")
        val storage = ItemStorage(mutableMapOf(1 to item1, 2 to item2, 3 to item3))
        storage.updateObjects(listOf(item1.copy(name = "oneone"))).first()
        val actual1 = storage.objectFor(1).first()
        val actual2 = storage.objectFor(2).first()
        val actual3 = storage.objectFor(3).first()
        assertNotNull(actual1)
        assertEquals("oneone", actual1.name)
        assertNotNull(actual2)
        assertEquals("two", actual2.name)
        assertNotNull(actual3)
        assertEquals("three", actual3.name)
    }

    @Test
    fun testDestroy() = runBlockingTest {
        val item = Item(1, "one")
        val storage = ItemStorage(mutableMapOf(1 to item))
        storage.destroyObject(item).first()
        val actual = storage.objectFor(1).first()
        assertNull(actual)
    }

    @Test
    fun testDestroyBatch() = runBlockingTest {
        val item1 = Item(1, "one")
        val item2 = Item(2, "two")
        val item3 = Item(3, "three")
        val storage = ItemStorage(mutableMapOf(1 to item1, 2 to item2, 3 to item3))
        storage.destroyObjects(listOf(item1)).first()
        val actual1 = storage.objectFor(1).first()
        val actual2 = storage.objectFor(2).first()
        val actual3 = storage.objectFor(3).first()
        assertNull(actual1)
        assertNotNull(actual2)
        assertEquals("two", actual2.name)
        assertNotNull(actual3)
        assertEquals("three", actual3.name)
    }
}
