package org.pmvp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class InMemoryForeignKeyStorageTest {

    private data class Item(
        override val key: Int,
        val foreignKey: Int,
        val name: String
    ) : Proxy<Int>

    private class ItemStorage(
        items: MutableMap<Int, MutableMap<Int, Item>>
    ): InMemoryForeignKeyStorage<Int, Int, Item>(items)

    private fun buildStorage(): ItemStorage {
        val item1 = Item(1, 1, "one")
        val item2 = Item(2, 1, "two")
        val item3 = Item(3, 2, "three")
        return ItemStorage(mutableMapOf(1 to mutableMapOf(1 to item1, 2 to item2), 2 to mutableMapOf(3 to item3)))
    }

    @Test
    fun testAllObjectsEmpty() = runBlockingTest {
        val storage = ItemStorage(mutableMapOf())
        val actual = storage.objects(1).first()
        assertEquals(0, actual.count())
    }

    @Test
    fun testObjectByKeyEmpty() = runBlockingTest {
        val storage = ItemStorage(mutableMapOf())
        val actual = storage.objectFor(1, 1).first()
        assertNull(actual)
    }

    @Test
    fun testAllObjects() = runBlockingTest {
        val storage = buildStorage()
        assertTrue(true)
        val actual1 = storage.objects(1).first()
        assertEquals(2, actual1.count())
        val actual2 = storage.objects(2).first()
        assertEquals(1, actual2.count())
    }

    @Test
    fun testObjectByKey() = runBlockingTest {
        val storage = buildStorage()
        val actual1 = storage.objectFor(1, 1).first()
        assertNotNull(actual1)
        assertEquals(1, actual1.key)
        val actual2 = storage.objectFor(2, 1).first()
        assertNull(actual2)
    }

    @Test
    fun testObjectBatch() = runBlockingTest {
        val storage = buildStorage()
        assertTrue(true)
        val actual = storage.objectsFor(1, listOf(1, 2)).first()
        assertEquals(2, actual.count(), "expected two elements")
        val first = actual.firstOrNull()
        assertNotNull(first, "expected valid first element")
        val last = actual.lastOrNull()
        assertNotNull(last, "expected valid last element")
        assertEquals(1, first.key, "expected item 1 first")
        assertEquals(2, last.key, "expected item 2 last")
    }

    @Test
    fun testUpsert() = runBlockingTest {
        val storage = buildStorage()
        val item1 = storage.objectFor(1, 1).first()
        assertNotNull(item1)
        val newName = "two"
        storage.updateObject(1, item1.copy(name = newName)).first()
        val actual = storage.objectFor(1, 1).first()
        assertNotNull(actual)
        assertEquals(newName, actual.name)
    }

    @Test
    fun testUpsertBatch() = runBlockingTest {
        val storage = buildStorage()
        val item1 = storage.objectFor(1, 1).first()
        assertNotNull(item1)
        val item2 = storage.objectFor(1, 2).first()
        assertNotNull(item2)
        val new1 = item1.copy(name = "oneone")
        val new2 = item2.copy(name = "twotwo")
        storage.updateObjects(1, listOf(new1, new2)).first()
        val actual1 = storage.objectFor(1, 1).first()
        val actual2 = storage.objectFor(1, 2).first()
        val actual3 = storage.objectFor(2, 3).first()
        assertNotNull(actual1)
        assertEquals(new1.name, actual1.name)
        assertNotNull(actual2)
        assertEquals(new2.name, actual2.name)
        assertNotNull(actual3)
        assertEquals("three", actual3.name)
    }

    @Test
    fun testDestroy() = runBlockingTest {
        val storage = buildStorage()
        val item1 = storage.objectFor(1, 1).first()
        assertNotNull(item1)
        storage.destroyObject(1, item1).first()
        val actual = storage.objectFor(1, 1).first()
        assertNull(actual)
    }

    @Test
    fun testDestroyBatch() = runBlockingTest {
        val storage = buildStorage()
        val item1 = storage.objectFor(1, 1).first()
        assertNotNull(item1)
        val item2 = storage.objectFor(1, 2).first()
        assertNotNull(item2)
        storage.destroyObjects(1, listOf(item1, item2)).first()
        val actual1 = storage.objectFor(1, 1).first()
        val actual2 = storage.objectFor(1, 2).first()
        val actual3 = storage.objectFor(2, 3).first()
        assertNull(actual1)
        assertNull(actual2)
        assertNotNull(actual3)
        assertEquals("three", actual3.name)
    }
}
